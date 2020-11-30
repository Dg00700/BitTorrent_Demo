import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.*;

public class FileHandler extends Thread {
	private static ConcurrentHashMap<Integer, byte[]> file;
	private volatile static BitSet filePieces;
	private static FileChannel outputFileChannel;
	private BlockingQueue<byte[]> fileQueue;
	private static FileHandler instance;
	private volatile HashMap<ConnectionModel, Integer> requestedChunks;

	private FileHandler() {
		fileQueue = new LinkedBlockingQueue<>();
		requestedChunks = new HashMap<>();
	}

	public void forceCloseOutputChannel() throws  Exception{
		if(outputFileChannel!=null){
			outputFileChannel.close();
		}
	}

	public static FileHandler getInstance() {
		synchronized (FileHandler.class) {
			if (null == instance) {
				instance = new FileHandler();
				instance.start();
			}
		}
		return instance;
	}

	static {
		file = new ConcurrentHashMap<Integer, byte[]>();
		filePieces = new BitSet(CommonProperties.numberOfChunks);
		try {
			File createdFile = new File(CommonProperties.PROPERTIES_CREATED_FILE_PATH + BitTorrentMainController.peerId
					+ File.separatorChar + CommonProperties.fileName);
			createdFile.getParentFile().mkdirs(); // Will create parent directories if not exists
			createdFile.createNewFile();
			outputFileChannel = FileChannel.open(createdFile.toPath(), StandardOpenOption.WRITE);
		} catch (IOException e) {
			System.out.println("Failed to create new file while receiving the file from host peer");
			e.printStackTrace();
		}
	}

	public synchronized byte[] getFileChunkByIndex(int chunkIndex) {
		return file.get(chunkIndex);
	}

	private void readFileInChunks(int numberOfChunks,int fileSize, DataInputStream dataInputStream) throws IOException{
		int chunkIndex = 0;
		for (int i = 0; i < CommonProperties.numberOfChunks; i++) {
			int chunkSize = i != numberOfChunks - 1 ? CommonProperties.chunkSize
					: fileSize % CommonProperties.chunkSize;
			byte[] piece = new byte[chunkSize];
			dataInputStream.readFully(piece);
			file.put(chunkIndex, piece);
			filePieces.set(chunkIndex++);
		}
	}

	public void splitFile() {
		File filePtr = new File(CommonProperties.PROPERTIES_FILE_PATH + CommonProperties.fileName);
		FileInputStream fileInputStream = null;
		DataInputStream dataInputStream = null;
		try {
			fileInputStream = new FileInputStream(filePtr);
			dataInputStream = new DataInputStream(fileInputStream);
			try {
				CommonProperties.DisplayMessageForUser(this,"Started Splitting the file");
				readFileInChunks(CommonProperties.numberOfChunks,
								(int) CommonProperties.fileSize,
								dataInputStream);
			}
			catch (IOException fileReadError) {
				fileReadError.printStackTrace();
				CommonProperties.DisplayMessageForUser(this,"Error while splitting file");
			}

		}
		catch (FileNotFoundException e) {
			CommonProperties.DisplayMessageForUser(this,"Error reading common.cfg file");
			e.printStackTrace();
		}
		finally {
			try {
				fileInputStream.close();
				dataInputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
				CommonProperties.DisplayMessageForUser(this,"Error while closing fileinputstream after reading file");
			}
		}
	}



	@Override
	public void run() {
		while (true) {
			try {
				byte[] message = fileQueue.take();
				int pieceIndex = ByteBuffer.wrap(message, 0, 4).getInt();

			} catch (Exception e) {
				CommonProperties.DisplayMessageForUser(this,e.getMessage());
			}

		}
	}

	public synchronized void setPiece(byte[] payload) {
		filePieces.set(ByteBuffer.wrap(payload, 0, 4).getInt());
		file.put(ByteBuffer.wrap(payload, 0, 4).getInt(), Arrays.copyOfRange(payload, 4, payload.length));
		try {
			fileQueue.put(payload);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public synchronized void writeToFile(String peerId) {
		String filename = CommonProperties.PROPERTIES_CREATED_FILE_PATH + peerId + File.separatorChar
				+ CommonProperties.fileName;
		System.out.println(filename);
		FileOutputStream outputStream = null;
		try {
			outputStream = new FileOutputStream(filename);
			for (int i = 0; i < file.size(); i++) {
				try {
					synchronized(this){
						if(outputStream==null || file==null)
							continue;
						outputStream.write(file.get(i));
					}
				} catch (Exception e) {
					System.out.println("Waiting...");
					continue;
				}
			}
		}
		catch (FileNotFoundException e) {
			//Subdue exception
		}
		finally {
			try {
				outputStream.flush();
			}
			catch (Exception ex){
				System.out.println("OutputStreamed failed to clear, beginning retry...");
			}
		}
	}

	public synchronized boolean isPieceAvailable(int index) {
		return filePieces.get(index);
	}

	public synchronized boolean isCompleteFile() {
		return filePieces.cardinality() == CommonProperties.numberOfChunks;
	}

	public synchronized int getReceivedFileSize() {
		return filePieces.cardinality();
	}

	protected synchronized int getRequestPieceIndex(ConnectionModel conn) {
		if (isCompleteFile()) {
			System.out.println("File received");
			return Integer.MIN_VALUE;
		}
		BitSet peerBitset = conn.getPeerBitSet();
		int numberOfPieces = CommonProperties.numberOfChunks;
		BitSet peerClone = (BitSet) peerBitset.clone();
		BitSet myClone = (BitSet) filePieces.clone();
		peerClone.andNot(myClone);
		if (peerClone.cardinality() == 0) {
			return Integer.MIN_VALUE;
		}
		myClone.flip(0, numberOfPieces);
		myClone.and(peerClone);
		System.out.println(peerClone + " " + myClone);
		int[] missingPieces = myClone.stream().toArray();
		return missingPieces[new Random().nextInt(missingPieces.length)];
	}

	public BitSet getFilePieces() {
		return filePieces;
	}

	public synchronized boolean hasAnyPieces() {
		return filePieces.nextSetBit(0) != -1;
	}

	public synchronized void addRequestedPiece(ConnectionModel connection, int pieceIndex) {
		requestedChunks.put(connection, pieceIndex);

	}

	public synchronized void removeRequestedPiece(ConnectionModel connection) {
		requestedChunks.remove(connection);
	}

}
