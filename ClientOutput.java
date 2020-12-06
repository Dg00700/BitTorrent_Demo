import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.*;
import java.io.DataInputStream;
import java.io.EOFException;
import java.nio.ByteBuffer;

public class ClientOutput implements Runnable {
	protected BlockingQueue<Integer> outboundMessageLengthQueue;
	protected BlockingQueue<byte[]> outboundMessageQueue;
	private Socket socket;
	private DataOutputStream outputDataStream;
	private boolean isConnectionActive;

	// client thread initialization
	public ClientOutput(Socket clientSocket, String id, DataController data) {
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
	public ClientOutput(Socket clientSocket, DataController data) {

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

class Client implements Runnable {
	private boolean isDownloadActive;
	private DataInputStream inputDataStream;
	private DataController sharedData;
	private Socket currentSocket;



	public Client(Socket socket, DataController data) {
		this.currentSocket = socket;
		sharedData = data;
		isDownloadActive = true;
		try {
			inputDataStream = new DataInputStream(socket.getInputStream());
		}
		catch (Exception e) {
			CommonProperties.DisplayMessageForUser(this,e.getMessage());
		}
	}

	@Override
	public void run() {

		receiveMessage();
	}

	public void receiveMessage() {
		while (isDownloadActive()) {
			int messageLength = Integer.MIN_VALUE;
			messageLength = receiveMessageLength();
			if (!isDownloadActive()) {
				continue;
			}
			byte[] message = new byte[messageLength];
			receiveMessageData(message);
			sharedData.addPayload(message);
		}

	}
	public void close()
	{
		boolean terminateSuccessfull = terminateClient();
	}

	private synchronized boolean isDownloadActive() {

		return isDownloadActive;
	}

	private int receiveMessageLength() {
		int responseLength = Integer.MIN_VALUE;
		byte[] messageLength = new byte[4];
		try {
			try {
				inputDataStream.readFully(messageLength);
			}
			catch (EOFException e) {
				System.exit(0);
			}
			catch (Exception e) {
				//System.out.println("No data to read");

			}
			responseLength = ByteBuffer.wrap(messageLength).getInt();
		} catch (Exception e) {
			CommonProperties.DisplayMessageForUser(this,e.getMessage());
		}
		return responseLength;
	}

	private void receiveMessageData(byte[] message) {
		try {
			inputDataStream.readFully(message);
		}
		catch (EOFException e) {
			System.exit(0);
		}
		catch (Exception e) {
			//System.out.println("No data to read");

		}
	}



	public boolean terminateClient(){
		try{
			if(currentSocket!=null){
				synchronized (this){
					currentSocket.close();
					return true;
				}
			}
		}
		catch (Exception ex){
			CommonProperties.DisplayMessageForUser(null,"UnHandled Client termination");
			return false;
		}
		return false;
	}

}
