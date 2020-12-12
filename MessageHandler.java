import java.nio.ByteBuffer;
import java.util.concurrent.*;
//import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.BitSet;


public class MessageHandler {
	private static MessageHandler mController;
	private FileHandler fHandler;

	private MessageHandler() {
		fHandler = FileHandler.getInstance();
	}
	
	public static MessageHandler getInstance() {
		synchronized (MessageHandler.class) {
			if (mController == null) {
				mController = new MessageHandler();
			}
		}
		return mController;
	}

	public synchronized int findMessageLength(MessageBody.Type type, int pieceIndex) {


		switch (type) {
		case CHOKE:
		case UNCHOKE:
		case INTERESTED:
		case NOTINTERESTED:
			
			return 1;
		case REQUEST:
			
		case HAVE:
			return 5;
		case BITFIELD:
		MessageBody bf = MessageBody.getInstance();
			return bf.getMessageLength();
		case PIECE:
			System.out.println("Shared file" + fHandler.readPieceOfFile(pieceIndex) + " asking for piece " + pieceIndex);
			int payloadLength = 5 + FileHandler.getInstance().readPieceOfFile(pieceIndex).length;
			return payloadLength;
		case HANDSHAKE:
			
			return 32;
			
		}
		return -1;
	}

	public synchronized ByteBuffer checkMessage(byte[] message) {
		if(message!=null) {
			mController.checkLength(message);
			return ByteBuffer.wrap(message);
		}
		else{
			return null;
		}
	}

	public synchronized byte[] setPayload(MessageBody.Type messageType, int pieceIndex) {
		byte[] response = new byte[5];
		if(messageType==MessageBody.Type.CHOKE){
			return new byte[] { 0 };
		}
		if(messageType==MessageBody.Type.UNCHOKE){
			return new byte[] { 1 };
		}
		if(messageType==MessageBody.Type.INTERESTED){
			return new byte[] { 2 };
		}
		if(messageType==MessageBody.Type.NOTINTERESTED){
			return new byte[] { 3 };
		}
		if(messageType==MessageBody.Type.HAVE){
			response[0] = 4;
			byte[] havePieceIndex = ByteBuffer.allocate(4).putInt(pieceIndex).array();
			System.arraycopy(havePieceIndex, 0, response, 1, 4);

		}
		else if(messageType==MessageBody.Type.BITFIELD){
			MessageBody bitfield = MessageBody.getInstance();
			response = bitfield.getMessageData();
		}
		else if(messageType==MessageBody.Type.REQUEST){
			response[0] = 6;
			byte[] index = ByteBuffer.allocate(4).putInt(pieceIndex).array();
			System.arraycopy(index, 0, response, 1, 4);
		}
		else if(messageType==MessageBody.Type.PIECE){
			response = generatePiece(pieceIndex);
		}
		if(messageType==MessageBody.Type.HANDSHAKE){
			return MessageBody.makeMessage();
		}
		return response;
	}

	public synchronized ByteBuffer checkLength(byte[] msg) {
		for(byte i:msg){
			if(i!=0){
				return ByteBuffer.wrap(msg);
			}
			else{
				return null;
			}
		}return null;
	}

	private byte[] generatePiece(int fileChunkIndex){
		byte[] response;
		byte[] slice = fHandler.readPieceOfFile(fileChunkIndex);
		int slicelen = slice.length;
		int totalLength = 5 + slicelen;
		response = new byte[totalLength];
		response[0] = 7;
		byte[] data = ByteBuffer.allocate(4).putInt(fileChunkIndex).array();
		System.arraycopy(data, 0, response, 1, 4);
		System.arraycopy(slice, 0, response, 5, slicelen);
		return response;
	}

	public synchronized MessageBody.Type getType(byte type) {

		MessageBody.Type response= null;

		if (type==0)
		{
			response = MessageBody.Type.CHOKE;
		}
		else if(type==1)
		{
			response = MessageBody.Type.UNCHOKE;
		}
		else if(type==2)
		{
			response = MessageBody.Type.INTERESTED;
		}
		else if(type==3)
		{
			response = MessageBody.Type.NOTINTERESTED;
		}
		else if(type==4)
		{
			response = MessageBody.Type.HAVE;
		}
		else if(type==5)
		{
			response = MessageBody.Type.BITFIELD;
		}
		else if(type==6)
		{
			response = MessageBody.Type.REQUEST;
		}
		else
		{
			response=MessageBody.Type.PIECE;
		}
		
		return response;
	}
}

class ThreadController extends Thread {
	private BlockingQueue<Object[]> q;
	private MessageHandler messageHandler;
	private ConStructure conStructure;
	private MessageBody.Type messageType;
	private int pieceIndex;
	private static ThreadController instance;

	private ThreadController() {
		q = new LinkedBlockingQueue<>();
		messageHandler = MessageHandler.getInstance();
		conStructure = null;
		messageType = null;
		pieceIndex = Integer.MIN_VALUE;
	}

	public static ThreadController getInstance() {
		synchronized (ThreadController.class) {
			if (instance == null) {
				instance = new ThreadController();
				instance.start();
			}
		}
		return instance;
	}

	public synchronized void generateMsg(Object[] data) {
		try {
			q.put(data);
		}
		catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while (true) {
			Object[] data = retrieveMessage();
			conStructure = (ConStructure) data[0];
			messageType = (MessageBody.Type) data[1];
			pieceIndex = (int) data[2];
			System.out.println(
					"Broadcaster: Building " + messageType + pieceIndex + " to peer " + conStructure.getRemotePeerId());
			int messageLength = messageHandler.findMessageLength(messageType, pieceIndex);
			byte[] payload = messageHandler.setPayload(messageType, pieceIndex);
			conStructure.sendMessage(messageLength, payload);
			System.out.println("Broadcaster: Sending " + messageType + " to peer " + conStructure.getRemotePeerId());

		}
	}

	private Object[] retrieveMessage() {
		Object[] data = null;
		try {
			data = q.take();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return data;
	}

}


class MessageBody {

	protected ByteBuffer bytebuffer;
	protected byte type;
	protected byte[] content;
	protected byte[] messageLength = new byte[4];
	protected byte[] message;
	private FileHandler fileHandler;
	private static MessageBody bitfield;


	public static enum Type {
		CHOKE, UNCHOKE, INTERESTED, NOTINTERESTED, HAVE, BITFIELD, REQUEST, PIECE, HANDSHAKE;
	}

	private MessageBody(){
		init();
	}

	private void init() {
		type = 5;
		message = new byte[PeerProperties.numberOfChunks + 1];
		content = new byte[PeerProperties.numberOfChunks];
		fileHandler = FileHandler.getInstance();
		message[0] = type;
		BitSet filePieces = fileHandler.getPieces();
		int i=0;
		while (i < PeerProperties.numberOfChunks) {
			if (filePieces.get(i)) {
				message[i + 1] = 1;
			}
			i++;
		}
	}

	public static MessageBody getInstance() {
		synchronized (MessageBody.class) {
			if (bitfield == null) {
				bitfield = new MessageBody();
			}
		}
		return bitfield;
	}

	
	protected synchronized int getMessageLength() {
		init();
		return message.length;
	}

	protected synchronized byte[] getMessageData() {
		return message;
	}
	private static final String HANDSHAKE_HEADER = "P2PFILESHARINGPROJ0000000000";
	private static String handshakeMessage = "";

	public static synchronized String checkRemotePeerId(byte[] b) {
		int to = b.length;
		int from = to - 4;
		byte[] arraycopy = Arrays.copyOfRange(b, from, to);
		String msg = new String(arraycopy, StandardCharsets.UTF_8);
		//System.out.println("msg="+msg);
		return msg;
	}

	public static synchronized byte[] makeMessage() {
		byte[] handshake = new byte[32];
		ByteBuffer bb = ByteBuffer.wrap(handshakeMessage.getBytes());
		bb.get(handshake);
		return handshake;
	}

	public static synchronized void makeHandshake(String peerId) {
		handshakeMessage += HANDSHAKE_HEADER + peerId;
	}

	public static synchronized String getId(byte[] message) {
		byte[] remotePeerId = Arrays.copyOfRange(message, message.length - 4, message.length);
		return new String(remotePeerId);
	}

	public static synchronized boolean checkPeerInfo(byte[] message, String peerId) {
		if(isValidPeer(message, peerId))
		return true;
		else 
		return false;
	}

	public static synchronized boolean isValidPeer(byte[] message, String peerId) {
		String recvdMessage = new String(message);
		return recvdMessage.indexOf(peerId) != -1 && recvdMessage.contains(HANDSHAKE_HEADER);
	}

}
