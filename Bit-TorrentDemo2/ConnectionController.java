import java.net.Socket;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;
import java.io.IOException;
import java.util.BitSet;

public class ConnectionController {

	private static ConnectionController instance;
	private HashSet<ConnectionModel> availableConnections;
	private HashSet<ConnectionModel> notInterested;
	private PriorityQueue<ConnectionModel> preferredNeighbors;
	public HashSet<String> peersWithFullFile = new HashSet<String>();
	private int numberofPrefferedNeighor = CommonProperties.numberOfPreferredNeighbors;
	private int optimisticUnchokingInterval = CommonProperties.unchokingInterval;
	private int unchokingInterval = CommonProperties.numberOfPreferredNeighbors;
	private int totalNumberofPeers = CommonProperties.numberOfPeers();
	private FileHandler fileHandler;
	private MessageBroadcastThreadPoolHandler broadcaster;

	private ConnectionController() {
		notInterested = new HashSet<>();
		preferredNeighbors = new PriorityQueue<>(numberofPrefferedNeighor + 1,
				(a, b) -> (int) a.getBytesDownloaded() - (int) b.getBytesDownloaded());
		broadcaster = MessageBroadcastThreadPoolHandler.getInstance();
		fileHandler = FileHandler.getInstance();
		availableConnections = new HashSet<>();
		chokeChangesnExitScheduler();
		unchokePeer();
	}

	public static ConnectionController getInstance() {
		synchronized (ConnectionController.class) {
			if (instance == null) {
				instance = new ConnectionController();
			}
		}
		return instance;
	}
	public synchronized void createConnection(Socket socket) {
		new ConnectionModel(socket);
	}

	public synchronized void processRejectedPeerConnections(String peerId, ConnectionModel connectionInstance) {
		notInterested.add(connectionInstance);
		preferredNeighbors.remove(connectionInstance);
	}

	public synchronized void createConnection(Socket socket, String peerId) {
		new ConnectionModel(socket, peerId);
	}


	public synchronized void registerConnection(ConnectionModel connection) {
		availableConnections.add(connection);
	}

	public void addToPeersWithFullFile(String str) {

		peersWithFullFile.add(str);
	}

	//To check
	private void chokeChangesnExitScheduler(){
		new Timer().scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (peersWithFullFile.size() == totalNumberofPeers - 1 && fileHandler.isCompleteFile()) {
					System.exit(0);
				}
				if (preferredNeighbors.size() > 1) {
					ConnectionModel notPrefNeighborConnection = preferredNeighbors.poll();
					notPrefNeighborConnection.setDownloadedbytes(0);
					for (ConnectionModel connT : preferredNeighbors) {
						connT.setDownloadedbytes(0);
					}
					broadcaster.addMessage(new Object[] { notPrefNeighborConnection, MessageModel.Type.CHOKE, Integer.MIN_VALUE });
					LoggerHandler.getInstance().logChangePreferredNeighbors(CommonProperties.getTime(), BitTorrentMainController.peerId,
							preferredNeighbors);
				}
			}
		}, new Date(), unchokingInterval * 1000);
	}

	private void unchokePeer()
	{
		new Timer().scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				for (ConnectionModel connectionInstance : availableConnections) {
					if (!notInterested.contains(connectionInstance) && !preferredNeighbors.contains(connectionInstance) && !connectionInstance.hasFile()) {
						broadcaster.addMessage(new Object[] { connectionInstance, MessageModel.Type.UNCHOKE, Integer.MIN_VALUE });
						preferredNeighbors.add(connectionInstance);
						LoggerHandler.getInstance().logOptimisticallyUnchokeNeighbor(CommonProperties.getTime(), BitTorrentMainController.peerId,
								connectionInstance.getRemotePeerId());
					}
				}
			}
		}, new Date(), optimisticUnchokingInterval * 1000);
	}




	public synchronized void broadCastHavetoAllRegisteredPeers(int fileChunkIndex) {
		if(availableConnections!=null) {
			for (ConnectionModel connectionInstance : availableConnections) {
				broadcaster.addMessage(new Object[]{
						connectionInstance, MessageModel.Type.HAVE, fileChunkIndex
				});
			}
		}
	}


	public synchronized void processAcceptedPeerConnections(ConnectionModel connectionInstance,String peerId) {
		if (preferredNeighbors.size() <= numberofPrefferedNeighor && !preferredNeighbors.contains(connectionInstance)) {
			connectionInstance.setDownloadedbytes(0);
			preferredNeighbors.add(connectionInstance);
			broadcaster.addMessage(new Object[] {
					connectionInstance,
					MessageModel.Type.UNCHOKE,
					Integer.MIN_VALUE
			});
		}
		notInterested.remove(connectionInstance);
	}


}

class ConnectionModel {

	private ConnectionController connectionController = ConnectionController.getInstance();
	Server server;
	Client client;
	DataController dataController;
	double bytesDownloaded;
	Socket peerSocket;
	String remotePeerId;
	boolean isConnectionChoked;


	public double getBytesDownloaded() {
		return bytesDownloaded;
	}

	protected Server getServerInstance() {
		return server;
	}

	public synchronized void incrementTotalBytesDownloaded(long value) {
		bytesDownloaded += value;
	}

	public synchronized boolean isConnectionChoked() {
		return isConnectionChoked;
	}

	public ConnectionModel(Socket peerSocket) {
		this.peerSocket = peerSocket;
		dataController = new DataController(this);
		server = new Server(peerSocket, dataController);
		client = new Client(peerSocket, dataController);
		bootStrapNode(server, client);
		dataController.setUpload(server);
		dataController.start();
	}

	public ConnectionModel(Socket peerSocket, String peerId) {
		this.peerSocket = peerSocket;
		dataController = new DataController(this);
		server = new Server(peerSocket, peerId, dataController);
		client = new Client(peerSocket,  dataController);
		bootStrapNode(server, client);
        LoggerHandler.getInstance().logTcpConnectionTo(Node.getInstance().getNetwork().getPeerId(), peerId);
		bootStrapDataController();
	}

	private void bootStrapDataController(){
		dataController.sendHandshake();
		dataController.setUpload(server);
		dataController.start();
	}

	public void bootStrapNode(Server server, Client client) {
		Thread serverThread = new Thread(server);
		Thread clientThread = new Thread(client);
		serverThread.start();
		clientThread.start();
	}

	public synchronized void sendMessage(int messageLength, byte[] payload) {
		server.addMessage(messageLength, payload);
	}

	public void close() {
		try {
			peerSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public synchronized String getRemotePeerId() {
		return remotePeerId;
	}

	public synchronized void broadCastHavetoAllRegisteredPeers(int fileChunkIndex) {
		connectionController.broadCastHavetoAllRegisteredPeers(fileChunkIndex);
	}

	protected synchronized void addRequestedPiece(int pieceIndex) {
		FileHandler.getInstance().addRequestedPiece(this, pieceIndex);
	}

	public synchronized void processAcceptedPeerConnections() {
		connectionController.processAcceptedPeerConnections(this,remotePeerId);
	}

	public synchronized void processRejectedPeerConnections() {
		connectionController.processRejectedPeerConnections(remotePeerId, this);
	}

	public synchronized void setDownloadedbytes(int bDownloaded) {
		bytesDownloaded = bDownloaded;
	}

	public void setPeerId(String value) {
		remotePeerId = value;
	}

	public synchronized void removeRequestedPiece() {
		FileHandler.getInstance().removeRequestedPiece(this);
	}

	public synchronized BitSet getPeerBitSet() {
		return dataController.getPeerBitSet();
	}

	public synchronized boolean hasFile() {
		return dataController.hasFile();
	}

	public synchronized void registerConnection() {
		connectionController.registerConnection(this);
	}

	public boolean ValidateConnectionForNull(ConnectionModel connection){
		if(connection == null)
			return true;
		else
			return false;
	}
	public boolean ValidateDtaInstanceForNull(DataController dataController){
		if(dataController == null)
			return true;
		else
			return false;
	}

}
