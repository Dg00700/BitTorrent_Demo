import java.nio.ByteBuffer;
import java.util.concurrent.*;
//import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.BitSet;


public class MessageController {
	private static MessageController messageManager;
	private FileHandler dataController;

	private MessageController() {
		dataController = FileHandler.getInstance();
	}

	public synchronized int processLength(byte[] messageLength) {
		if(messageLength==null) {
			return 0;
		}
		else{
			return ByteBuffer.wrap(messageLength).getInt();
		}
	}

	public static MessageController getInstance() {
		synchronized (MessageController.class) {
			if (messageManager == null) {
				messageManager = new MessageController();
			}
		}
		return messageManager;
	}

	public synchronized int getMessageLength(MessageModel.Type messageType, int pieceIndex) {


		switch (messageType) {
		case CHOKE:
		case UNCHOKE:
		case INTERESTED:
		case NOTINTERESTED:
			
			return 1;
		case REQUEST:
			
		case HAVE:
			return 5;
		case BITFIELD:
			MessageModel bitfield = MessageModel.getInstance();
			return bitfield.getMessageLength();
		case PIECE:
			System.out.println("Shared file" + dataController.getFileChunkByIndex(pieceIndex) + " asking for piece " + pieceIndex);
			int payloadLength = 5 + FileHandler.getInstance().getFileChunkByIndex(pieceIndex).length;
			return payloadLength;
		case HANDSHAKE:
			
			return 32;
			
		}
		return -1;
	}

	public synchronized ByteBuffer processData(byte[] message) {
		if(message!=null) {
			return ByteBuffer.wrap(message);
		}
		else{
			return null;
		}
	}

	public synchronized byte[] getMessagePayload(MessageModel.Type messageType, int pieceIndex) {
		byte[] respMessage = new byte[5];
		if(messageType==MessageModel.Type.CHOKE){
			return new byte[] { 0 };
		}
		if(messageType==MessageModel.Type.UNCHOKE){
			return new byte[] { 1 };
		}
		if(messageType==MessageModel.Type.INTERESTED){
			return new byte[] { 2 };
		}
		if(messageType==MessageModel.Type.NOTINTERESTED){
			return new byte[] { 3 };
		}
		if(messageType==MessageModel.Type.HAVE){
			respMessage[0] = 4;
			byte[] havePieceIndex = ByteBuffer.allocate(4).putInt(pieceIndex).array();
			System.arraycopy(havePieceIndex, 0, respMessage, 1, 4);

		}
		else if(messageType==MessageModel.Type.BITFIELD){
			MessageModel bitfield = MessageModel.getInstance();
			respMessage = bitfield.getMessageData();
		}
		else if(messageType==MessageModel.Type.REQUEST){
			respMessage[0] = 6;
			byte[] index = ByteBuffer.allocate(4).putInt(pieceIndex).array();
			System.arraycopy(index, 0, respMessage, 1, 4);
		}
		else if(messageType==MessageModel.Type.PIECE){
			respMessage = processPiece(pieceIndex);
		}
		if(messageType==MessageModel.Type.HANDSHAKE){
			return MessageModel.getMessage();
		}
		return respMessage;
	}

	private byte[] processPiece(int fileChunkIndex){
		byte[] respMessage;
		byte[] piece = dataController.getFileChunkByIndex(fileChunkIndex);
		int pieceSize = piece.length;
		int totalLength = 5 + pieceSize;
		respMessage = new byte[totalLength];
		respMessage[0] = 7;
		byte[] data = ByteBuffer.allocate(4).putInt(fileChunkIndex).array();
		System.arraycopy(data, 0, respMessage, 1, 4);
		System.arraycopy(piece, 0, respMessage, 5, pieceSize);
		return respMessage;
	}

	public synchronized MessageModel.Type getType(byte type) {

		MessageModel.Type response= null;

		if (type==0)
		{
			response = MessageModel.Type.CHOKE;
		}
		else if(type==1)
		{
			response = MessageModel.Type.UNCHOKE;
		}
		else if(type==2)
		{
			response = MessageModel.Type.INTERESTED;
		}
		else if(type==3)
		{
			response = MessageModel.Type.NOTINTERESTED;
		}
		else if(type==4)
		{
			response = MessageModel.Type.HAVE;
		}
		else if(type==5)
		{
			response = MessageModel.Type.BITFIELD;
		}
		else if(type==6)
		{
			response = MessageModel.Type.REQUEST;
		}
		else
		{
			response=MessageModel.Type.PIECE;
		}
		
		return response;
	}
}

class MessageBroadcastThreadPoolHandler extends Thread {
	private BlockingQueue<Object[]> queue;
	private MessageController messageController;
	private ConnectionModel conn;
	private MessageModel.Type messageType;
	private int pieceIndex;
	private static MessageBroadcastThreadPoolHandler instance;

	private MessageBroadcastThreadPoolHandler() {
		queue = new LinkedBlockingQueue<>();
		messageController = MessageController.getInstance();
		conn = null;
		messageType = null;
		pieceIndex = Integer.MIN_VALUE;
	}

	public static MessageBroadcastThreadPoolHandler getInstance() {
		synchronized (MessageBroadcastThreadPoolHandler.class) {
			if (instance == null) {
				instance = new MessageBroadcastThreadPoolHandler();
				instance.start();
			}
		}
		return instance;
	}

	public synchronized void addMessage(Object[] data) {
		try {
			queue.put(data);
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
			conn = (ConnectionModel) data[0];
			messageType = (MessageModel.Type) data[1];
			pieceIndex = (int) data[2];
			System.out.println(
					"Broadcaster: Building " + messageType + pieceIndex + " to peer " + conn.getRemotePeerId());
			int messageLength = messageController.getMessageLength(messageType, pieceIndex);
			byte[] payload = messageController.getMessagePayload(messageType, pieceIndex);
			conn.sendMessage(messageLength, payload);
			System.out.println("Broadcaster: Sending " + messageType + " to peer " + conn.getRemotePeerId());

		}
	}

	private Object[] retrieveMessage() {
		Object[] data = null;
		try {
			data = queue.take();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return data;
	}

}


class MessageModel {

	protected ByteBuffer bytebuffer;
	protected byte type;
	protected byte[] content;
	protected byte[] messageLength = new byte[4];
	protected byte[] message;
	private FileHandler fileHandler;
	private static MessageModel bitfield;


	public static enum Type {
		CHOKE, UNCHOKE, INTERESTED, NOTINTERESTED, HAVE, BITFIELD, REQUEST, PIECE, HANDSHAKE;
	}

	private MessageModel(){
		init();
	}

	private void init() {
		type = 5;
		message = new byte[CommonProperties.numberOfChunks + 1];
		content = new byte[CommonProperties.numberOfChunks];
		fileHandler = FileHandler.getInstance();
		message[0] = type;
		BitSet filePieces = fileHandler.getFilePieces();
		int i=0;
		while (i < CommonProperties.numberOfChunks) {
			if (filePieces.get(i)) {
				message[i + 1] = 1;
			}
			i++;
		}
	}

	public static MessageModel getInstance() {
		synchronized (MessageModel.class) {
			if (bitfield == null) {
				bitfield = new MessageModel();
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

	public static synchronized String getRemotePeerId(byte[] b) {
		int to = b.length;
		int from = to - 4;
		byte[] bytes = Arrays.copyOfRange(b, from, to);
		String str = new String(bytes, StandardCharsets.UTF_8);
		return str;
	}

	private static synchronized void initHandshake(String peerId) {

		handshakeMessage += HANDSHAKE_HEADER + peerId;
	}

	public static synchronized byte[] getMessage() {
		byte[] handshake = new byte[32];
		ByteBuffer bb = ByteBuffer.wrap(handshakeMessage.getBytes());
		bb.get(handshake);
		return handshake;
	}

	public static synchronized void setId(String peerId) {
		initHandshake(peerId);
	}

	public static synchronized boolean verify(byte[] message, String peerId) {
		String recvdMessage = new String(message);
		return recvdMessage.indexOf(peerId) != -1 && recvdMessage.contains(HANDSHAKE_HEADER);
	}

	public static synchronized String getId(byte[] message) {
		byte[] remotePeerId = Arrays.copyOfRange(message, message.length - 4, message.length);
		return new String(remotePeerId);
	}

}
