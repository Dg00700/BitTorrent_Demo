import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.*;
import java.io.DataInputStream;
import java.io.EOFException;
import java.nio.ByteBuffer;

public class ClientOutput implements Runnable {

	private Socket socket;
	private DataOutputStream stream_output;
	private boolean working;
	public BlockingQueue<Integer> streamSize;
	public BlockingQueue<byte[]> message_out;
	
	// client thread initialization
	public ClientOutput(Socket socket, String id, PeerProcess current) {
		
		streamSize = new LinkedBlockingQueue<>();
		message_out = new LinkedBlockingQueue<>();
		working = true;
		this.socket = socket;
		try {
			stream_output = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// server thread initialization
	public ClientOutput(Socket socket, PeerProcess current) {

		
		streamSize = new LinkedBlockingQueue<>();
		message_out = new LinkedBlockingQueue<>();
		working = true;
		this.socket = socket;
		try {
			stream_output = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		catch (Exception ex){
			System.out.println("Exception at DataOutputStream");
		}
	}

	
	@Override
	public String toString(){
		return "Socket is Active : "+working;
	}

	@Override
	public void run() {
		while (working) {
			try {
				callMessageSize();
				callData();
			}
			catch (SocketException e) {

				working = false;
				
			}
			catch (Exception e) {
				
			}
		}
	}
	public void callData(){
		try{
			byte[] message = message_out.take();
		stream_output.write(message);
		stream_output.flush();
		}
		catch(Exception e)
		{
			System.out.println("Exception Send Message");
		}
		
	}

	public void callMessageSize() throws Exception{
		int messageLength = streamSize.take();
		stream_output.writeInt(messageLength);
		stream_output.flush();
	}

	
	public void addMessage(int length, byte[] payload) {
		try {
			streamSize.put(length);
			message_out.put(payload);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

class Client implements Runnable {
	private boolean isDownload;
	private DataInputStream input_stream;
	private PeerProcess sharedData;
	private Socket currentSocket;



	public Client(Socket socket, PeerProcess current) {
		this.currentSocket = socket;
		sharedData = current;
		isDownload = true;
		try {
			input_stream = new DataInputStream(socket.getInputStream());
		}
		catch (Exception e) {
			//pass
		}
	}

	@Override
	public void run() {

		while (isDownload()) {
			int messageLength = Integer.MIN_VALUE;
			messageLength = getInfoSize();
			if (!isDownload()) {
				continue;
			}
			byte[] message = new byte[messageLength];
			getMessage(message);
			sharedData.add_Data(message);
		}

	}

	
	public void close()
	{
		//terminateClient();
	}

	private synchronized boolean isDownload() {

		return isDownload;
	}

	private int getInfoSize() {
		int responseLength = Integer.MIN_VALUE;
		byte[] messageLength = new byte[4];
		try {
			try {
				input_stream.readFully(messageLength);
			}
			catch (EOFException e) {
				System.exit(0);
			}
			catch (Exception e) {
				//pass

			}
			responseLength = ByteBuffer.wrap(messageLength).getInt();
		} catch (Exception e) {
			
		}
		return responseLength;
	}

	private void getMessage(byte[] message) {
		try {
			input_stream.readFully(message);
		}
		catch (EOFException e) {
			System.exit(0);
		}
		catch (Exception e) {
			//System.exit(0)

		}
	}



	
}


