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
	private ThreadController msg_thread_broadcast;
	private boolean hasFile;
	private BitSet peer_bit;
	
	private ConStructure link_active;
	private volatile boolean upload;
	private volatile boolean download;
	ClientOutput clientOutput;
	private PeerSetter node = PeerSetter.getInstance();
	private BlockingQueue<byte[]> msg_link;
	private boolean is_active;


	public PeerProcess(ConStructure connection) {
		link_active = connection;
		msg_link = new LinkedBlockingQueue<>();
		is_active = true;
		file_id = FileHandler.getInstance();
		msg_thread_broadcast = ThreadController.getInstance();
		peer_bit = new BitSet(PeerProperties.numberOfChunks);
	}

	public void transfer(ClientOutput value) {
		clientOutput = value;
		if (get_handshake()) {
			msg_thread_broadcast.generateMsg(new Object[] { link_active, MessageBody.Type.HANDSHAKE, Integer.MIN_VALUE });
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
		if (peer_bit.cardinality() == PeerProperties.numberOfChunks) {
			hasFile = true;
			ConnectionHandler.getInstance().addToPeersWithFullFile(peer_id);
		}
	}

	public synchronized void updateBitsetOfPeer(int index) {
		peer_bit.set(index);
		if (peer_bit.cardinality() == PeerProperties.numberOfChunks) {
			ConnectionHandler.getInstance().addToPeersWithFullFile(peer_id);
			hasFile = true;
		}
	}

	private MessageBody.Type processChoke()
	{
		LoggerHandler.getInstance().choked(PeerProperties.getTime(), P2PInitializer.peerId, link_active.getRemotePeerId());
		link_active.removeRequestedPiece();
		return null;
	}

	private MessageBody.Type processInterested(){
		LoggerHandler.getInstance().receiveInterested(PeerProperties.getTime(), P2PInitializer.peerId,
		link_active.getRemotePeerId());
		link_active.setPeerConnections();
		return null;
	}

	private MessageBody.Type processNotInterested(){
		LoggerHandler.getInstance().receiveNotInterested(PeerProperties.getTime(), P2PInitializer.peerId,
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
			MessageBody.Type messageType = determineType(message[0]);
		
			MessageBody.Type responseMessageType = null;
		int fileChunkIndex = Integer.MIN_VALUE;
		
		System.out.println("Received message: " + messageType);
		 if(messageType==MessageBody.Type.CHOKE){
			responseMessageType=processChoke();
		}
		else if(messageType==MessageBody.Type.UNCHOKE){
			LoggerHandler.getInstance().unchoked(PeerProperties.getTime(), P2PInitializer.peerId, link_active.getRemotePeerId());
			responseMessageType = MessageBody.Type.REQUEST;
			fileChunkIndex = file_id.receivedRequestedPiece(link_active);
		}
		else if(messageType==MessageBody.Type.INTERESTED){
			responseMessageType = processInterested();
		}
		else if(messageType==MessageBody.Type.NOTINTERESTED){
			responseMessageType = processNotInterested();
		}
		else if(messageType==MessageBody.Type.HAVE){
			fileChunkIndex = ByteBuffer.wrap(message, 1, 4).getInt();
				LoggerHandler.getInstance().receiveHave(PeerProperties.getTime(), P2PInitializer.peerId, link_active.getRemotePeerId(),
						fileChunkIndex);
				updateBitsetOfPeer(fileChunkIndex);
				responseMessageType = getInterestedNotInterested();
		}
		else if(messageType==MessageBody.Type.BITFIELD){
			setBitsetOfPeer(message);
				responseMessageType = getInterestedNotInterested();
		}
		else if(messageType==MessageBody.Type.REQUEST){
			responseMessageType = MessageBody.Type.PIECE;
				byte[] content = new byte[4];
				System.arraycopy(message, 1, content, 0, 4);
				fileChunkIndex = ByteBuffer.wrap(content).getInt();
				if (fileChunkIndex == Integer.MIN_VALUE) {
					System.out.println("received file");
					responseMessageType = null;
				}
		}
		else if(messageType==MessageBody.Type.PIECE){
			processPiece(fileChunkIndex,message,responseMessageType,messageType);
		}
		else if(messageType==MessageBody.Type.HANDSHAKE){
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

	private void processPiece(int fileChunkIndex, byte[] message, MessageBody.Type responseMessageType,
	MessageBody.Type messageType){
		fileChunkIndex = ByteBuffer.wrap(message, 1, 4).getInt();
		link_active.incrementTotalBytesDownloaded(message.length);
		file_id.setFilePiece(Arrays.copyOfRange(message, 1, message.length));
		LoggerHandler.getInstance().downloadingPiece(PeerProperties.getTime(), P2PInitializer.peerId, link_active.getRemotePeerId(),
				fileChunkIndex, file_id.getFileLength());
		responseMessageType = MessageBody.Type.REQUEST;
		link_active.PrintHaveforAllRegiPeers(fileChunkIndex);
		fileChunkIndex = file_id.receivedRequestedPiece(link_active);
		if (fileChunkIndex == Integer.MIN_VALUE) {
			LoggerHandler.getInstance().downloadComplete(PeerProperties.getTime(), P2PInitializer.peerId);
			file_id.writeToFile(P2PInitializer.peerId);
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


	private void processHandshake(byte[] message, MessageBody.Type responseMessageType, int fileChunkIndex){
		peer_id=MessageBody.getId(message);
		link_active.setPeerId(peer_id);
		link_active.setConnection();
		if (!get_handshake()) {
			setHandshake();
			LoggerHandler.getInstance().connectionFrom(node.getNetwork().getPeerId(), peer_id);
			msg_thread_broadcast.generateMsg(new Object[] { link_active, MessageBody.Type.HANDSHAKE, Integer.MIN_VALUE });
		}
		if (file_id.hasAnyPieces()) {
			responseMessageType = MessageBody.Type.BITFIELD;
		}
		if (null != responseMessageType) {
			msg_thread_broadcast.generateMsg(new Object[] { link_active, responseMessageType, fileChunkIndex });
		}
	}

	private boolean isInterested() {
		for (int i = 0; i < PeerProperties.numberOfChunks; i++) {
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

	private MessageBody.Type getInterestedNotInterested() {
		if (isInterested()) {
			return MessageBody.Type.INTERESTED;
		}
		return MessageBody.Type.NOTINTERESTED;
	}

	private MessageBody.Type determineType(byte type) {
		MessageHandler messageManager = MessageHandler.getInstance();
		if (!isDownloaded()) {
			set_handshake();
			return MessageBody.Type.HANDSHAKE;
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
