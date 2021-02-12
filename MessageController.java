import java.nio.ByteBuffer;
import java.util.concurrent.*;
//import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.BitSet;


public class MessageController {
	private static MessageController mController;
	private FileHandler fHandler;

	private MessageController() {
		fHandler = FileHandler.getInstance();
	}
	
	public static MessageController getInstance() {
		synchronized (MessageController.class) {
			if (mController == null) {
				mController = new MessageController();
			}
		}
		return mController;
	}

	public synchronized int findMessageLength(MessageModel.Type type, int pieceIndex) {


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
			MessageModel bf = MessageModel.getInstance();
			return bf.getMessageLength();
		case PIECE:
			System.out.println("Shared file" + fHandler.getFileChunkByIndex(pieceIndex) + " asking for piece " + pieceIndex);
			int payloadLength = 5 + FileHandler.getInstance().getFileChunkByIndex(pieceIndex).length;
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

	public synchronized byte[] setPayload(MessageModel.Type messageType, int pieceIndex) {
		byte[] response = new byte[5];
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
			response[0] = 4;
			byte[] havePieceIndex = ByteBuffer.allocate(4).putInt(pieceIndex).array();
			System.arraycopy(havePieceIndex, 0, response, 1, 4);

		}
		else if(messageType==MessageModel.Type.BITFIELD){
			MessageModel bitfield = MessageModel.getInstance();
			response = bitfield.getMessageData();
		}
		else if(messageType==MessageModel.Type.REQUEST){
			response[0] = 6;
			byte[] index = ByteBuffer.allocate(4).putInt(pieceIndex).array();
			System.arraycopy(index, 0, response, 1, 4);
		}
		else if(messageType==MessageModel.Type.PIECE){
			response = generatePiece(pieceIndex);
		}
		if(messageType==MessageModel.Type.HANDSHAKE){
			return MessageModel.makeMessage();
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
		byte[] slice = fHandler.getFileChunkByIndex(fileChunkIndex);
		int slicelen = slice.length;
		int totalLength = 5 + slicelen;
		response = new byte[totalLength];
		response[0] = 7;
		byte[] data = ByteBuffer.allocate(4).putInt(fileChunkIndex).array();
		System.arraycopy(data, 0, response, 1, 4);
		System.arraycopy(slice, 0, response, 5, slicelen);
		return response;
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
	private BlockingQueue<Object[]> q;
	private MessageController messageController;
	private ConnectionModel connectionModel;
	private MessageModel.Type messageType;
	private int pieceIndex;
	private static MessageBroadcastThreadPoolHandler instance;

	private MessageBroadcastThreadPoolHandler() {
		q = new LinkedBlockingQueue<>();
		messageController = MessageController.getInstance();
		connectionModel = null;
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
			connectionModel = (ConnectionModel) data[0];
			messageType = (MessageModel.Type) data[1];
			pieceIndex = (int) data[2];
			System.out.println(
					"Broadcaster: Building " + messageType + pieceIndex + " to peer " + connectionModel.getRemotePeerId());
			int messageLength = messageController.findMessageLength(messageType, pieceIndex);
			byte[] payload = messageController.setPayload(messageType, pieceIndex);
			connectionModel.sendMessage(messageLength, payload);
			System.out.println("Broadcaster: Sending " + messageType + " to peer " + connectionModel.getRemotePeerId());

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
