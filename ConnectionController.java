import java.net.Socket;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;

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
