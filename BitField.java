import java.util.BitSet;

public class BitField extends MessageModel {

	private static BitField bitfield;
	private FileHandler fileHandler;

	private BitField() {
		init();
	}

	private void init() {
		type = 5;
		message = new byte[CommonProperties.numberOfChunks + 1];
		content = new byte[CommonProperties.numberOfChunks];
		fileHandler = FileHandler.getInstance();
		message[0] = type;
		BitSet filePieces = fileHandler.getFilePieces();
		for (int i = 0; i < CommonProperties.numberOfChunks; i++) {
			if (filePieces.get(i)) {
				message[i + 1] = 1;
			}
		}
	}

	public static BitField getInstance() {
		synchronized (BitField.class) {
			if (bitfield == null) {
				bitfield = new BitField();
			}
		}
		return bitfield;
	}

	@Override
	protected synchronized int getMessageLength() {
		init();
		return message.length;
	}

	@Override
	protected synchronized byte[] getMessageData() {
		return message;
	}

}
