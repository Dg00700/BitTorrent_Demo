import java.nio.ByteBuffer;


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

		MessageModel.Type resp = null;

		switch (type) {
			case 0:
				resp = MessageModel.Type.CHOKE;
				break;
			case 1:
				resp = MessageModel.Type.UNCHOKE;
				break;
			case 2:
				resp = MessageModel.Type.INTERESTED;
				break;
			case 3:
				resp = MessageModel.Type.NOTINTERESTED;
				break;
			case 4:
				resp = MessageModel.Type.HAVE;
				break;
			case 5:
				resp = MessageModel.Type.BITFIELD;
				break;
			case 6:
				resp = MessageModel.Type.REQUEST;
				break;
			case 7:
				resp = MessageModel.Type.PIECE;
				break;
			default:
				break;
		}

		return resp;
	}
}
