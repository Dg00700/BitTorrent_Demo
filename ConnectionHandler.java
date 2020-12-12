import java.net.Socket;
import java.util.Date;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;
import java.io.IOException;
import java.util.BitSet;

public class ConnectionHandler {

	private HashSet<ConStructure> nodesNotInterestedinstance;
	private PriorityQueue<ConStructure> pref_Neighbors_queue;
	private int totalPrefNeighornum = PeerProperties.numberOfPreferredNeighbors;
	private ThreadController msgtransmitter;
	private HashSet<ConStructure> availableNodes;
	private FileHandler fhandler;
	

	public synchronized void initiateConnection(Socket socket) {
		new ConStructure(socket);
	}
	private static ConnectionHandler Connectioninstance;
	public static ConnectionHandler getInstance() {
		synchronized (ConnectionHandler.class) {
			if (Connectioninstance == null) {
				Connectioninstance = new ConnectionHandler();
			}
		}
		return Connectioninstance;
	}
	private ConnectionHandler() {
		nodesNotInterestedinstance = new HashSet<>();
		pref_Neighbors_queue = new PriorityQueue<>(totalPrefNeighornum + 1,
				(a, b) -> (int) a.getDownloadedData() - (int) b.getDownloadedData());
		msgtransmitter = ThreadController.getInstance();
		fhandler = FileHandler.getInstance();
		availableNodes = new HashSet<>();
		ChokePeer();
		unchokePeer();
	}
	
	public synchronized void notInterestedPeerConnection(String peerId, ConStructure connectionInstance) {
		nodesNotInterestedinstance.add(connectionInstance);
		pref_Neighbors_queue.remove(connectionInstance);
	}

	public synchronized void startPeerConnection(Socket socket, String peerId) {
		new ConStructure(socket, peerId);
	}
	public synchronized Boolean checkifPeerConnections(String peerId, ConStructure connectionInstance) {
		if(peerId==null || connectionInstance==null)
		return false;
		else 
		return true;
	}


	public synchronized void addConnection(ConStructure connection) {
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
			return new HashSet<ConStructure>();
	}

	private int totalPeernum = PeerProperties.numberOfPeers();
	private int getunchokingTime = PeerProperties.numberOfPreferredNeighbors;
	//To check
	private void ChokePeer(){
		new Timer().scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (CompleteFileNodes.size() == totalPeernum - 1 && fhandler.isFullFile()) {
					System.exit(0);
				}
				if (pref_Neighbors_queue.size() > 0) {
					ConStructure notPrefNeighborConnection = pref_Neighbors_queue.poll();
					notPrefNeighborConnection.setDownloadedbytes(0);
					for (ConStructure connT : pref_Neighbors_queue) {
						connT.setDownloadedbytes(0);
					}
					msgtransmitter.generateMsg(new Object[] { notPrefNeighborConnection, MessageBody.Type.CHOKE, Integer.MIN_VALUE });
					LoggerHandler.getInstance().changePreferredNeighbors(PeerProperties.getTime(), P2PInitializer.peerId,
					pref_Neighbors_queue);
				}
			}
		}, new Date(), getunchokingTime * 1000);
	}

	private int getoptimisticUnchokingTime = PeerProperties.unchokingInterval;


	private void unchokePeer()
	{
		new Timer().scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				for (ConStructure connectionInstance : availableNodes) {
					if (!nodesNotInterestedinstance.contains(connectionInstance) && !pref_Neighbors_queue.contains(connectionInstance) && !connectionInstance.hasFile()) {
						msgtransmitter.generateMsg(new Object[] { connectionInstance, MessageBody.Type.UNCHOKE, Integer.MIN_VALUE });
						pref_Neighbors_queue.add(connectionInstance);
						LoggerHandler.getInstance().changeOptimisticallyUnchokeNeighbor(PeerProperties.getTime(), P2PInitializer.peerId,
								connectionInstance.getRemotePeerId());
					}
				}
			}
		}, new Date(), getoptimisticUnchokingTime * 1000);
	}

	public synchronized void PrintHaveforAllRegiPeers(int fileChunkIndex) {
		if(availableNodes!=null) {
			for (ConStructure connectionInstance : availableNodes) {
				msgtransmitter.generateMsg(new Object[]{
						connectionInstance, MessageBody.Type.HAVE, fileChunkIndex
				});
			}
		}
	}
	public synchronized boolean checkValidConn(ConStructure connectionInstance,String peerId){
		if (pref_Neighbors_queue.size() <= totalPrefNeighornum && !pref_Neighbors_queue.contains(connectionInstance)){
		return true;
		}
		return false;
	}
	public synchronized void addValidConnection(ConStructure connectionInstance,String peerId) {
		if (checkValidConn(connectionInstance, peerId)) {
			connectionInstance.setDownloadedbytes(0);
			pref_Neighbors_queue.add(connectionInstance);
			msgtransmitter.generateMsg(new Object[] {
					connectionInstance,
					MessageBody.Type.UNCHOKE,
					Integer.MIN_VALUE
			});
		}
		nodesNotInterestedinstance.remove(connectionInstance);
	}


}
class ConStructure {

	private ConnectionHandler connectionHandler = ConnectionHandler.getInstance();
	double Downloaded_Data;
	public synchronized void incrementTotalBytesDownloaded(long value) {
		Downloaded_Data += value;
	}
	public double getDownloadedData() {
		return Downloaded_Data;
	}
	PeerProcess pProcess;
	private void setup(ClientOutput clientOutput){
		pProcess.transfer(clientOutput);
		pProcess.start();
	}
	Client client;
	Socket peerSocket;
	
	ClientOutput clientOutput;
	public ConStructure(Socket nodeSocket) {
		this.peerSocket = nodeSocket;
		pProcess = new PeerProcess(this);
		clientOutput = new ClientOutput(nodeSocket, pProcess);
		client = new Client(nodeSocket, pProcess);
		Thread serverThread = new Thread(clientOutput);
		Thread clientThread = new Thread(client);
		serverThread.start();
		clientThread.start();
		setup(clientOutput);
	}

	

	public ConStructure(Socket peerSocket, String peerId) {
		this.peerSocket = peerSocket;
		pProcess = new PeerProcess(this);
		clientOutput = new ClientOutput(peerSocket, peerId, pProcess);
		client = new Client(peerSocket,  pProcess);
		Thread serverThread = new Thread(clientOutput);
		Thread clientThread = new Thread(client);
		serverThread.start();
		clientThread.start();
        LoggerHandler.getInstance().connectionTo(PeerSetter.getInstance().getNetwork().getPeerId(), peerId);
		pProcess.sendHandshake();
        LoggerHandler.getInstance().handshakeFrom(PeerSetter.getInstance().getNetwork().getPeerId(), peerId);

		pProcess.transfer(clientOutput);
        LoggerHandler.getInstance().bitfieldFrom(PeerSetter.getInstance().getNetwork().getPeerId(), peerId);

		pProcess.start();
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
	String remotePeerId;
	public synchronized String getRemotePeerId() {
		return remotePeerId;
	}

	public synchronized void PrintHaveforAllRegiPeers(int fileChunkIndex) {
		connectionHandler.PrintHaveforAllRegiPeers(fileChunkIndex);
	}

	

	public synchronized void setPeerConnections() {
		connectionHandler.addValidConnection(this,remotePeerId);
	}

	public synchronized void peerConnRejected() {
		connectionHandler.notInterestedPeerConnection(remotePeerId, this);
	}

	public synchronized void setDownloadedbytes(int bDownloaded) {
		Downloaded_Data = bDownloaded;
	}

	public void setPeerId(String value) {
		remotePeerId = value;
	}

	public synchronized void removeRequestedPiece() {
		FileHandler.getInstance().removeRequestedPiece(this);
	}

	public synchronized BitSet getBitSetOfPeer() {
		return pProcess.getBitSetOfPeer();
	}

	public synchronized boolean hasFile() {
		return pProcess.hasFile();
	}

	public synchronized void setConnection() {
		connectionHandler.addConnection(this);
	}

	

}
