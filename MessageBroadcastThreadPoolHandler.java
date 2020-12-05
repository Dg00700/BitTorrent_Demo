import java.util.concurrent.*;

public class MessageBroadcastThreadPoolHandler extends Thread {
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
