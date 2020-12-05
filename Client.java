import java.io.DataInputStream;
import java.io.EOFException;
import java.net.Socket;
import java.nio.ByteBuffer;

public class Client implements Runnable {
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
