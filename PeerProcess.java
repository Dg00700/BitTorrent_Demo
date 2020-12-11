import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.*;
class DataMessageWrapper
{
	String fromPeerID;
	
	public DataMessageWrapper() 
	{
		fromPeerID = null;
	}
    public void setFromPeerID(String fromPeerID) {
        this.fromPeerID = fromPeerID;
    }
    
	public String getFromPeerID() {
		return fromPeerID;
	}
}



public class PeerProcess extends Thread {
	private BitSet peerBitset;
	private String remotePeerId;

	private FileHandler fileHandler;
	private MessageBroadcastThreadPoolHandler broadcaster;
	private boolean peerHasFile;
	private Node node = Node.getInstance();
	private BlockingQueue<byte[]> messageQueue;
	private boolean isPeerProcessInstanceAlive;

	private ConnectionModel activeConnection;
	private volatile boolean uploadHandshake;
	private volatile boolean isHandshakeDownloaded;
	ClientOutput clientOutput;

	public PeerProcess(ConnectionModel connection) {
		activeConnection = connection;
		messageQueue = new LinkedBlockingQueue<>();
		isPeerProcessInstanceAlive = true;
		fileHandler = FileHandler.getInstance();
		broadcaster = MessageBroadcastThreadPoolHandler.getInstance();
		peerBitset = new BitSet(CommonProperties.numberOfChunks);
	}

	public void setUpload(ClientOutput value) {
		clientOutput = value;
		if (getUploadHandshake()) {
			broadcaster.generateMsg(new Object[] { activeConnection, MessageModel.Type.HANDSHAKE, Integer.MIN_VALUE });
		}
	}

	@Override
	public void run() {
		while (isPeerProcessInstanceAlive) {
			try {
				byte[] messageItem = messageQueue.take();
				processMessage(messageItem);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public synchronized void addPayload(byte[] payload) {
		try {
			messageQueue.put(payload);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public synchronized BitSet getPeerBitSet() {
		return peerBitset;
	}

	public synchronized void sendHandshake() {
		setUploadHandshake();
	}

	public synchronized void setUploadHandshake() {
		uploadHandshake = true;
	}

	public synchronized boolean getUploadHandshake() {
		return uploadHandshake;
	}


	public synchronized void setPeerBitset(byte[] payload) {
		for (int i = 1; i < payload.length; i++) {
			if (payload[i] == 1) {
				peerBitset.set(i - 1);
			}
		}
		if (peerBitset.cardinality() == CommonProperties.numberOfChunks) {
			peerHasFile = true;
			ConnectionController.getInstance().addToPeersWithFullFile(remotePeerId);
		}
	}

	public synchronized void updatePeerBitset(int index) {
		peerBitset.set(index);
		if (peerBitset.cardinality() == CommonProperties.numberOfChunks) {
			ConnectionController.getInstance().addToPeersWithFullFile(remotePeerId);
			peerHasFile = true;
		}
	}

	private MessageModel.Type processChoke()
	{
		LoggerHandler.getInstance().logChokNeighbor(CommonProperties.getTime(), BitTorrentMainController.peerId, activeConnection.getRemotePeerId());
		activeConnection.removeRequestedPiece();
		return null;
	}

	private MessageModel.Type processInterested(){
		LoggerHandler.getInstance().logInterestedMessage(CommonProperties.getTime(), BitTorrentMainController.peerId,
				activeConnection.getRemotePeerId());
		activeConnection.processAcceptedPeerConnections();
		return null;
	}

	private MessageModel.Type processNotInterested(){
		LoggerHandler.getInstance().logNotInterestedMessage(CommonProperties.getTime(), BitTorrentMainController.peerId,
				activeConnection.getRemotePeerId());
		activeConnection.processRejectedPeerConnections();
		return null;
	}

	private  byte[] intToByteArray(int v)
	{
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) 
        {
            int off = (b.length - 1 - i) * 8;
            b[i] = (byte) ((v >>> off) & 0xFF);
        }
        return b;
    }
	protected void processMessage(byte[] message) {
		try {
			MessageModel.Type messageType = getMessageType(message[0]);
		
		MessageModel.Type responseMessageType = null;
		int fileChunkIndex = Integer.MIN_VALUE;
		System.out.println("Received message: " + messageType);
		if(messageType==MessageModel.Type.CHOKE){
			responseMessageType=processChoke();
		}
		else if(messageType==MessageModel.Type.UNCHOKE){
			LoggerHandler.getInstance().logUnchokNeighbor(CommonProperties.getTime(), BitTorrentMainController.peerId, activeConnection.getRemotePeerId());
			responseMessageType = MessageModel.Type.REQUEST;
			fileChunkIndex = fileHandler.getRequestPieceIndex(activeConnection);
		}
		else if(messageType==MessageModel.Type.INTERESTED){
			responseMessageType = processInterested();
		}
		else if(messageType==MessageModel.Type.NOTINTERESTED){
			responseMessageType = processNotInterested();
		}
		else if(messageType==MessageModel.Type.HAVE){
			fileChunkIndex = ByteBuffer.wrap(message, 1, 4).getInt();
				LoggerHandler.getInstance().logReceivedHaveMessage(CommonProperties.getTime(), BitTorrentMainController.peerId, activeConnection.getRemotePeerId(),
						fileChunkIndex);
				updatePeerBitset(fileChunkIndex);
				responseMessageType = getInterestedNotInterested();
		}
		else if(messageType==MessageModel.Type.BITFIELD){
			setPeerBitset(message);
				responseMessageType = getInterestedNotInterested();
		}
		else if(messageType==MessageModel.Type.REQUEST){
			responseMessageType = MessageModel.Type.PIECE;
				byte[] content = new byte[4];
				System.arraycopy(message, 1, content, 0, 4);
				fileChunkIndex = ByteBuffer.wrap(content).getInt();
				if (fileChunkIndex == Integer.MIN_VALUE) {
					System.out.println("received file");
					responseMessageType = null;
				}
		}
		else if(messageType==MessageModel.Type.PIECE){
			processPiece(fileChunkIndex,message,responseMessageType,messageType);
		}
		else if(messageType==MessageModel.Type.HANDSHAKE){
			processHandshake(message,responseMessageType,fileChunkIndex);
		}
		if (null != responseMessageType) {
			broadcaster.generateMsg(new Object[] { activeConnection, responseMessageType, fileChunkIndex });
		}
	}
	catch(Exception e){
		System.out.println("Peer terminated");
	}
	
	}

	private void processPiece(int fileChunkIndex, byte[] message, MessageModel.Type responseMessageType,
							  MessageModel.Type messageType){
		fileChunkIndex = ByteBuffer.wrap(message, 1, 4).getInt();
		activeConnection.incrementTotalBytesDownloaded(message.length);
		fileHandler.setPiece(Arrays.copyOfRange(message, 1, message.length));
		LoggerHandler.getInstance().logDownloadedPiece(CommonProperties.getTime(), BitTorrentMainController.peerId, activeConnection.getRemotePeerId(),
				fileChunkIndex, fileHandler.getReceivedFileSize());
		responseMessageType = MessageModel.Type.REQUEST;
		activeConnection.broadCastHavetoAllRegisteredPeers(fileChunkIndex);
		fileChunkIndex = fileHandler.getRequestPieceIndex(activeConnection);
		if (fileChunkIndex == Integer.MIN_VALUE) {
			LoggerHandler.getInstance().logDownloadComplete(CommonProperties.getTime(), BitTorrentMainController.peerId);
			fileHandler.writeToFile(BitTorrentMainController.peerId);
			messageType = null;
			isPeerProcessInstanceAlive = false;
			responseMessageType = null;
		}
		if (null != responseMessageType) {
			broadcaster.generateMsg(new Object[] { activeConnection, responseMessageType, fileChunkIndex });
		}
	}

	private int byteArrayToInt(byte[] b, int off)
    {
        int v = 0;
        for (int i = 0; i < 4; i++)
        {
            int s = (4 - 1 - i) * 8;
            v += (b[i + off] & 0x000000FF) << s;
        }
        return v;
    }

	private void processHandshake(byte[] message, MessageModel.Type responseMessageType, int fileChunkIndex){
		remotePeerId = MessageModel.getId(message);
		activeConnection.setPeerId(remotePeerId);
		activeConnection.registerConnection();
		if (!getUploadHandshake()) {
			setUploadHandshake();
			LoggerHandler.getInstance().logTcpConnectionFrom(node.getNetwork().getPeerId(), remotePeerId);
			broadcaster.generateMsg(new Object[] { activeConnection, MessageModel.Type.HANDSHAKE, Integer.MIN_VALUE });
		}
		if (fileHandler.hasAnyPieces()) {
			responseMessageType = MessageModel.Type.BITFIELD;
		}
		if (null != responseMessageType) {
			broadcaster.generateMsg(new Object[] { activeConnection, responseMessageType, fileChunkIndex });
		}
	}

	private boolean isInterested() {
		for (int i = 0; i < CommonProperties.numberOfChunks; i++) {
			if (peerBitset.get(i) && !fileHandler.isPieceAvailable(i)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasFile() {
		return peerHasFile;
	}

	private MessageModel.Type getInterestedNotInterested() {
		if (isInterested()) {
			return MessageModel.Type.INTERESTED;
		}
		return MessageModel.Type.NOTINTERESTED;
	}

	private MessageModel.Type getMessageType(byte type) {
		MessageController messageManager = MessageController.getInstance();
		if (!isHandshakeDownloaded()) {
			setHandshakeDownloaded();
			return MessageModel.Type.HANDSHAKE;
		}
		return messageManager.getType(type);
	}

	private boolean isHandshakeDownloaded() {
		return isHandshakeDownloaded;
	}

	private void setHandshakeDownloaded() {
		isHandshakeDownloaded = true;
	}
	
}
