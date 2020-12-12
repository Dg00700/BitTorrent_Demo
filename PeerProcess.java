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
	private String peer_id;

	private FileHandler file_id;
	private MessageBroadcastThreadPoolHandler msg_thread_broadcast;
	private boolean hasFile;
	private BitSet peer_bit;
	
	private ConnectionModel link_active;
	private volatile boolean upload;
	private volatile boolean download;
	ClientOutput clientOutput;
	private Node node = Node.getInstance();
	private BlockingQueue<byte[]> msg_link;
	private boolean is_active;


	public PeerProcess(ConnectionModel connection) {
		link_active = connection;
		msg_link = new LinkedBlockingQueue<>();
		is_active = true;
		file_id = FileHandler.getInstance();
		msg_thread_broadcast = MessageBroadcastThreadPoolHandler.getInstance();
		peer_bit = new BitSet(CommonProperties.numberOfChunks);
	}

	public void transfer(ClientOutput value) {
		clientOutput = value;
		if (get_handshake()) {
			msg_thread_broadcast.generateMsg(new Object[] { link_active, MessageModel.Type.HANDSHAKE, Integer.MIN_VALUE });
		}
	}

	@Override
	public void run() {
		while (is_active) {
			try {
				byte[] messageItem = msg_link.take();
				message_task(messageItem);
			}
			catch (InterruptedException e) {
				System.out.println(e);;
			}
		}
	}

	public synchronized void add_Data(byte[] data) {
		try {
			msg_link.put(data);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public synchronized BitSet getBitSetOfPeer() {
		return peer_bit;
	}

	public synchronized void sendHandshake() {
		setHandshake();
	}

	public synchronized void setHandshake() {
		upload = true;
	}

	public synchronized boolean get_handshake() {
		return upload;
	}


	public synchronized void setBitsetOfPeer(byte[] data) {
		for (int i = 1; i < data.length; i++) {
			if (data[i] == 1) {
				peer_bit.set(i - 1);
			}
		}
		if (peer_bit.cardinality() == CommonProperties.numberOfChunks) {
			hasFile = true;
			ConnectionController.getInstance().addToPeersWithFullFile(peer_id);
		}
	}

	public synchronized void updateBitsetOfPeer(int index) {
		peer_bit.set(index);
		if (peer_bit.cardinality() == CommonProperties.numberOfChunks) {
			ConnectionController.getInstance().addToPeersWithFullFile(peer_id);
			hasFile = true;
		}
	}

	private MessageModel.Type processChoke()
	{
		LoggerHandler.getInstance().choked(CommonProperties.getTime(), BitTorrentMainController.peerId, link_active.getRemotePeerId());
		link_active.removeRequestedPiece();
		return null;
	}

	private MessageModel.Type processInterested(){
		LoggerHandler.getInstance().receiveInterested(CommonProperties.getTime(), BitTorrentMainController.peerId,
		link_active.getRemotePeerId());
		link_active.setPeerConnections();
		return null;
	}

	private MessageModel.Type processNotInterested(){
		LoggerHandler.getInstance().receiveNotInterested(CommonProperties.getTime(), BitTorrentMainController.peerId,
		link_active.getRemotePeerId());
		link_active.peerConnRejected();
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
	protected void message_task(byte[] message) {
		try {
			MessageModel.Type messageType = determineType(message[0]);
		
		MessageModel.Type responseMessageType = null;
		int fileChunkIndex = Integer.MIN_VALUE;
		
		System.out.println("Received message: " + messageType);
		 if(messageType==MessageModel.Type.CHOKE){
			responseMessageType=processChoke();
		}
		else if(messageType==MessageModel.Type.UNCHOKE){
			LoggerHandler.getInstance().unchoked(CommonProperties.getTime(), BitTorrentMainController.peerId, link_active.getRemotePeerId());
			responseMessageType = MessageModel.Type.REQUEST;
			fileChunkIndex = file_id.receivedRequestedPiece(link_active);
		}
		else if(messageType==MessageModel.Type.INTERESTED){
			responseMessageType = processInterested();
		}
		else if(messageType==MessageModel.Type.NOTINTERESTED){
			responseMessageType = processNotInterested();
		}
		else if(messageType==MessageModel.Type.HAVE){
			fileChunkIndex = ByteBuffer.wrap(message, 1, 4).getInt();
				LoggerHandler.getInstance().receiveHave(CommonProperties.getTime(), BitTorrentMainController.peerId, link_active.getRemotePeerId(),
						fileChunkIndex);
				updateBitsetOfPeer(fileChunkIndex);
				responseMessageType = getInterestedNotInterested();
		}
		else if(messageType==MessageModel.Type.BITFIELD){
			setBitsetOfPeer(message);
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
			msg_thread_broadcast.generateMsg(new Object[] { link_active, responseMessageType, fileChunkIndex });
		}
	}
	catch(Exception e){
		System.out.println("Peer terminated");
	}
	
	}

	private void processPiece(int fileChunkIndex, byte[] message, MessageModel.Type responseMessageType,
							  MessageModel.Type messageType){
		fileChunkIndex = ByteBuffer.wrap(message, 1, 4).getInt();
		link_active.incrementTotalBytesDownloaded(message.length);
		file_id.setFilePiece(Arrays.copyOfRange(message, 1, message.length));
		LoggerHandler.getInstance().downloadingPiece(CommonProperties.getTime(), BitTorrentMainController.peerId, link_active.getRemotePeerId(),
				fileChunkIndex, file_id.getFileLength());
		responseMessageType = MessageModel.Type.REQUEST;
		link_active.PrintHaveforAllRegiPeers(fileChunkIndex);
		fileChunkIndex = file_id.receivedRequestedPiece(link_active);
		if (fileChunkIndex == Integer.MIN_VALUE) {
			LoggerHandler.getInstance().downloadComplete(CommonProperties.getTime(), BitTorrentMainController.peerId);
			file_id.writeToFile(BitTorrentMainController.peerId);
			messageType = null;
			is_active = false;
			responseMessageType = null;
		}
		if (null != responseMessageType) {
			msg_thread_broadcast.generateMsg(new Object[] { link_active, responseMessageType, fileChunkIndex });
		}
	}

	public synchronized int byteArrayToInt(byte[] b, int off)
    {
        int v = 0;
        for (int i = 0; i < 4; i++)
        {
            int s = (4 - 1 - i) * 8;
            v += (b[i + off] & 0x000000FF) << s;
		}
		if(checkintVal(v))
		return v;
		else return -1;
	}


	private void processHandshake(byte[] message, MessageModel.Type responseMessageType, int fileChunkIndex){
		peer_id=MessageModel.getId(message);
		link_active.setPeerId(peer_id);
		link_active.setConnection();
		if (!get_handshake()) {
			setHandshake();
			LoggerHandler.getInstance().connectionFrom(node.getNetwork().getPeerId(), peer_id);
			msg_thread_broadcast.generateMsg(new Object[] { link_active, MessageModel.Type.HANDSHAKE, Integer.MIN_VALUE });
		}
		if (file_id.hasAnyPieces()) {
			responseMessageType = MessageModel.Type.BITFIELD;
		}
		if (null != responseMessageType) {
			msg_thread_broadcast.generateMsg(new Object[] { link_active, responseMessageType, fileChunkIndex });
		}
	}

	private boolean isInterested() {
		for (int i = 0; i < CommonProperties.numberOfChunks; i++) {
			if (peer_bit.get(i) && !file_id.is_available(i)) {
				return true;
			}
		}
		return false;
	}

	private boolean checkintVal(int v){
		if(v%1==0)
		return true;
		else 
		return false;
	}

	public boolean hasFile() {
		return hasFile;
	}

	private MessageModel.Type getInterestedNotInterested() {
		if (isInterested()) {
			return MessageModel.Type.INTERESTED;
		}
		return MessageModel.Type.NOTINTERESTED;
	}

	private MessageModel.Type determineType(byte type) {
		MessageController messageManager = MessageController.getInstance();
		if (!isDownloaded()) {
			set_handshake();
			return MessageModel.Type.HANDSHAKE;
		}
		return messageManager.getType(type);
	}

	private boolean isDownloaded() {
		return download;
	}

	private void set_handshake() {
		download = true;
	}
	
}
