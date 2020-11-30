import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.*;

public class Server implements Runnable {
	protected BlockingQueue<Integer> outboundMessageLengthQueue;
	protected BlockingQueue<byte[]> outboundMessageQueue;
	private Socket socket;
	private DataOutputStream outputDataStream;
	private boolean isConnectionActive;

	// client thread initialization
	public Server(Socket clientSocket, String id, DataController data) {
		DataController currentController = data;
		if(data!=null){
			//needed to update the controller.
		}
		outboundMessageLengthQueue = new LinkedBlockingQueue<>();
		outboundMessageQueue = new LinkedBlockingQueue<>();
		isConnectionActive = true;
		this.socket = clientSocket;
		try {
			outputDataStream = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		catch (Exception ex){
			//Subside.
		}
	}

	// server thread initialization
	public Server(Socket clientSocket, DataController data) {

		DataController currentController = data;
		if(data!=null){
			//needed to update the controller.
		}
		outboundMessageLengthQueue = new LinkedBlockingQueue<>();
		outboundMessageQueue = new LinkedBlockingQueue<>();
		isConnectionActive = true;
		this.socket = clientSocket;
		try {
			outputDataStream = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		catch (Exception ex){
			//Subside.
		}
	}

	private void init(Socket clientSocket, DataController data) {
		DataController currentController = data;
		if(data!=null){
			//needed to update the controller.
		}
		if(outputDataStream!=null) {
			try {
				outputDataStream = new DataOutputStream(socket.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception ex) {
				//Subside.
			}
		}
	}

	@Override
	public String toString(){
		return "Socket is Active : "+isConnectionActive;
	}

	@Override
	public void run() {
		while (isConnectionActive) {
			try {
				sendMessageLength();
				sendMessageData();
			}
			catch (SocketException e) {
				isConnectionActive = false;
				CommonProperties.DisplayMessageForUser(this,e.getMessage());
			}
			catch (Exception e) {
				CommonProperties.DisplayMessageForUser(this,e.getMessage());
			}
		}
	}

	private void sendMessageLength() throws Exception{
		int messageLength = outboundMessageLengthQueue.take();
		outputDataStream.writeInt(messageLength);
		outputDataStream.flush();
	}

	private void sendMessageData() throws Exception{
		byte[] message = outboundMessageQueue.take();
		outputDataStream.write(message);
		outputDataStream.flush();
	}

	public void addMessage(int length, byte[] payload) {
		try {
			outboundMessageLengthQueue.put(length);
			outboundMessageQueue.put(payload);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
