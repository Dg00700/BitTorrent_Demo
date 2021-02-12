import java.net.Socket;
import java.util.Date;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;
import java.io.IOException;
import java.util.BitSet;

public class ConnectionController {

	private HashSet<ConnectionModel> nodesNotInterestedinstance;
	private PriorityQueue<ConnectionModel> pref_Neighbors_queue;
	private int totalPrefNeighornum = CommonProperties.numberOfPreferredNeighbors;
	private MessageBroadcastThreadPoolHandler msgtransmitter;
	private HashSet<ConnectionModel> availableNodes;
	private FileHandler fhandler;
	

	public synchronized void initiateConnection(Socket socket) {
		new ConnectionModel(socket);
	}
	private static ConnectionController Connectioninstance;
	public static ConnectionController getInstance() {
		synchronized (ConnectionController.class) {
			if (Connectioninstance == null) {
				Connectioninstance = new ConnectionController();
			}
		}
		return Connectioninstance;
	}
	private ConnectionController() {
		nodesNotInterestedinstance = new HashSet<>();
		pref_Neighbors_queue = new PriorityQueue<>(totalPrefNeighornum + 1,
				(a, b) -> (int) a.getBytesDownloaded() - (int) b.getBytesDownloaded());
		msgtransmitter = MessageBroadcastThreadPoolHandler.getInstance();
		fhandler = FileHandler.getInstance();
		availableNodes = new HashSet<>();
		ChokePeer();
		unchokePeer();
	}
	
	public synchronized void notInterestedPeerConnection(String peerId, ConnectionModel connectionInstance) {
		nodesNotInterestedinstance.add(connectionInstance);
		pref_Neighbors_queue.remove(connectionInstance);
	}

	public synchronized void startPeerConnection(Socket socket, String peerId) {
		new ConnectionModel(socket, peerId);
	}
	public synchronized Boolean checkifPeerConnections(String peerId, ConnectionModel connectionInstance) {
		if(peerId==null || connectionInstance==null)
		return false;
		else 
		return true;
	}


	public synchronized void addConnection(ConnectionModel connection) {
		availableNodes.add(connection);
	}
	public HashSet<String> CompleteFileNodes = new HashSet<String>();
	public void addToPeersWithFullFile(String str) {

		CompleteFileNodes.add(str);
	}

	public HashSet checkAvailavleNodes(){
		if(availableNodes.size()!=0)
			return availableNodes;
		else if(availableNodes.size()==0 && CompleteFileNodes.size()!=0)
			return CompleteFileNodes; 
		else
			return new HashSet<ConnectionModel>();
	}

	private int totalPeernum = CommonProperties.numberOfPeers();
	private int getunchokingTime = CommonProperties.numberOfPreferredNeighbors;
	//To check
	private void ChokePeer(){
		new Timer().scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (CompleteFileNodes.size() == totalPeernum - 1 && fhandler.isCompleteFile()) {
					System.exit(0);
				}
				if (pref_Neighbors_queue.size() > 0) {
					ConnectionModel notPrefNeighborConnection = pref_Neighbors_queue.poll();
					notPrefNeighborConnection.setDownloadedbytes(0);
					for (ConnectionModel connT : pref_Neighbors_queue) {
						connT.setDownloadedbytes(0);
					}
					msgtransmitter.generateMsg(new Object[] { notPrefNeighborConnection, MessageModel.Type.CHOKE, Integer.MIN_VALUE });
					LoggerHandler.getInstance().changePreferredNeighbors(CommonProperties.getTime(), BitTorrentMainController.peerId,
					pref_Neighbors_queue);
				}
			}
		}, new Date(), getunchokingTime * 1000);
	}

	private int getoptimisticUnchokingTime = CommonProperties.unchokingInterval;


	private void unchokePeer()
	{
		new Timer().scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				for (ConnectionModel connectionInstance : availableNodes) {
					if (!nodesNotInterestedinstance.contains(connectionInstance) && !pref_Neighbors_queue.contains(connectionInstance) && !connectionInstance.hasFile()) {
						msgtransmitter.generateMsg(new Object[] { connectionInstance, MessageModel.Type.UNCHOKE, Integer.MIN_VALUE });
						pref_Neighbors_queue.add(connectionInstance);
						LoggerHandler.getInstance().changeOptimisticallyUnchokeNeighbor(CommonProperties.getTime(), BitTorrentMainController.peerId,
								connectionInstance.getRemotePeerId());
					}
				}
			}
		}, new Date(), getoptimisticUnchokingTime * 1000);
	}

	public synchronized void PrintHaveforAllRegiPeers(int fileChunkIndex) {
		if(availableNodes!=null) {
			for (ConnectionModel connectionInstance : availableNodes) {
				msgtransmitter.generateMsg(new Object[]{
						connectionInstance, MessageModel.Type.HAVE, fileChunkIndex
				});
			}
		}
	}
	public synchronized boolean checkValidConn(ConnectionModel connectionInstance,String peerId){
		if (pref_Neighbors_queue.size() <= totalPrefNeighornum && !pref_Neighbors_queue.contains(connectionInstance)){
		return true;
		}
		return false;
	}
	public synchronized void addValidConnection(ConnectionModel connectionInstance,String peerId) {
		if (checkValidConn(connectionInstance, peerId)) {
			connectionInstance.setDownloadedbytes(0);
			pref_Neighbors_queue.add(connectionInstance);
			msgtransmitter.generateMsg(new Object[] {
					connectionInstance,
					MessageModel.Type.UNCHOKE,
					Integer.MIN_VALUE
			});
		}
		nodesNotInterestedinstance.remove(connectionInstance);
	}


}
class ConnectionModel {

	private ConnectionController connectionController = ConnectionController.getInstance();
	ClientOutput clientOutput;
	Client client;
	PeerProcess peerProcess;
	double bytesDownloaded;
	Socket peerSocket;
	String remotePeerId;
	boolean isConnectionChoked;


	public double getBytesDownloaded() {
		return bytesDownloaded;
	}

	protected ClientOutput getServerInstance() {
		return clientOutput;
	}

	public synchronized void incrementTotalBytesDownloaded(long value) {
		bytesDownloaded += value;
	}


	public ConnectionModel(Socket peerSocket) {
		this.peerSocket = peerSocket;
		peerProcess = new PeerProcess(this);
		clientOutput = new ClientOutput(peerSocket, peerProcess);
		client = new Client(peerSocket, peerProcess);
		Thread serverThread = new Thread(clientOutput);
		Thread clientThread = new Thread(client);
		serverThread.start();
		clientThread.start();
		setup(clientOutput);
	}

	private void setup(ClientOutput clientOutput){
		peerProcess.transfer(clientOutput);
		peerProcess.start();
	}

	public ConnectionModel(Socket peerSocket, String peerId) {
		this.peerSocket = peerSocket;
		peerProcess = new PeerProcess(this);
		clientOutput = new ClientOutput(peerSocket, peerId, peerProcess);
		client = new Client(peerSocket,  peerProcess);
		Thread serverThread = new Thread(clientOutput);
		Thread clientThread = new Thread(client);
		serverThread.start();
		clientThread.start();
        LoggerHandler.getInstance().connectionTo(Node.getInstance().getNetwork().getPeerId(), peerId);
		peerProcess.sendHandshake();
        LoggerHandler.getInstance().handshakeFrom(Node.getInstance().getNetwork().getPeerId(), peerId);

		peerProcess.transfer(clientOutput);
        LoggerHandler.getInstance().bitfieldFrom(Node.getInstance().getNetwork().getPeerId(), peerId);

		peerProcess.start();
	}
	public synchronized void sendMessage(int messageLength, byte[] payload) {
		clientOutput.addMessage(messageLength, payload);
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

	public synchronized void PrintHaveforAllRegiPeers(int fileChunkIndex) {
		connectionController.PrintHaveforAllRegiPeers(fileChunkIndex);
	}

	protected synchronized void addRequestedPiece(int pieceIndex) {
		FileHandler.getInstance().addRequestedPiece(this, pieceIndex);
	}

	public synchronized void setPeerConnections() {
		connectionController.addValidConnection(this,remotePeerId);
	}

	public synchronized void peerConnRejected() {
		connectionController.notInterestedPeerConnection(remotePeerId, this);
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

	public synchronized BitSet getBitSetOfPeer() {
		return peerProcess.getBitSetOfPeer();
	}

	public synchronized boolean hasFile() {
		return peerProcess.hasFile();
	}

	public synchronized void set() {
		connectionController.addConnection(this);
	}

	

}