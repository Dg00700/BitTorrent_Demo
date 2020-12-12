
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Random;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.*;
import java.io.RandomAccessFile;

public class FileHandler extends Thread {
	private static ConcurrentHashMap<Integer, byte[]> file;
	private volatile static BitSet shradedFile;
	private static FileChannel fileOutputChannel;
	private BlockingQueue<byte[]> fileQueue;
	private static FileHandler handlerInstance;
	
	
	private volatile HashMap<ConnectionModel, Integer> requestedChunks;

	static {
		file = new ConcurrentHashMap<Integer, byte[]>();
		shradedFile = new BitSet(CommonProperties.numberOfChunks);
		try {
			File newFile = new File(CommonProperties.PROPERTIES_CREATED_FILE_PATH + BitTorrentMainController.peerId
					+ File.separatorChar + CommonProperties.fileName);
			newFile.getParentFile().mkdirs(); // Will create parent directories if not exists
			newFile.createNewFile();
			fileOutputChannel = FileChannel.open(newFile.toPath(), StandardOpenOption.WRITE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}



	private FileHandler() {
		fileQueue = new LinkedBlockingQueue<>();
		requestedChunks = new HashMap<>();
	}

	public void forceCloseOutputChannel() throws  Exception{
		if(fileOutputChannel!=null){
			fileOutputChannel.close();
		}
	}

	public static FileHandler getInstance() {
		synchronized (FileHandler.class) {
			if (null == handlerInstance) {
				handlerInstance = new FileHandler();
				handlerInstance.start();
			}
		}
		return handlerInstance;
	}


	public synchronized byte[] getFileChunkByIndex(int pieceIndex) {
		return file.get(pieceIndex);
	}

	private void readFileInChunks(int numberOfChunks,int fileSize, DataInputStream dInputStream) throws IOException{
		int pieceIndex = 0;
		for (int i = 0; i < CommonProperties.numberOfChunks; i++) {
			int chunkSize = i != numberOfChunks - 1 ? CommonProperties.pieceSize
					: fileSize % CommonProperties.pieceSize;
			byte[] piece = new byte[chunkSize];
			dInputStream.readFully(piece);
			file.put(pieceIndex, piece);
			shradedFile.set(pieceIndex++);
		}
	}

	public void splitFile() {
		File filePtr = new File(CommonProperties.PROPERTIES_FILE_PATH + CommonProperties.fileName);
		FileInputStream fInputStream = null;
		DataInputStream dInputStream = null;
		try {
			fInputStream = new FileInputStream(filePtr);
			dInputStream = new DataInputStream(fInputStream);
			try {
				readFileInChunks(CommonProperties.numberOfChunks, (int) CommonProperties.fileSize, dInputStream);
			}
			catch (IOException fileReadError) {
				fileReadError.printStackTrace();
				
			}

		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		finally {
			try {
				fInputStream.close();
				dInputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
				
			}
		}
	}



	@Override
	public void run() {
		while (true) {
			try {
				byte[] info = fileQueue.take();
				int pieceIndex = ByteBuffer.wrap(info, 0, 4).getInt();

			} catch (Exception e) {
				
			}

		}
	}

	public synchronized void setPiece(byte[] payload) {
		shradedFile.set(ByteBuffer.wrap(payload, 0, 4).getInt());
		file.put(ByteBuffer.wrap(payload, 0, 4).getInt(), Arrays.copyOfRange(payload, 4, payload.length));
		try {
			fileQueue.put(payload);
		} catch (InterruptedException Ex) {
			Ex.printStackTrace();
		}
	}

	private void loadFile(int pieceSize, int FileSize, int PieceSize) throws IOException {
		RandomAccessFile file = null;
		int totalPieces = (int)Math.ceil((double)(FileSize / PieceSize));
		for (int i = 0; i < totalPieces; i++) {
			int pieceLength = Math.min(PieceSize, FileSize - i * pieceSize);
			byte[] data = new byte[pieceLength];
			file.seek(i*pieceSize);
			
		}
	}
	public BitSet getFilePieces() {
		return shradedFile;
	}

	public synchronized boolean hasAnyPieces() {
		return shradedFile.nextSetBit(0) != -1;
	}

	public synchronized void removeRequestedPiece(ConnectionModel connection) {
		requestedChunks.remove(connection);
	}
	public synchronized void writeToFile(String peerId) {
		String fName = CommonProperties.PROPERTIES_CREATED_FILE_PATH + peerId + File.separatorChar + CommonProperties.fileName;
		System.out.println(fName);
		FileOutputStream optStream = null;
		try {
			optStream = new FileOutputStream(fName);
			for (int i = 0; i < file.size(); i++) {
				try {
					synchronized(this){
						if(optStream==null || file==null)
							continue;
						optStream.write(file.get(i));
					}
				} catch (Exception e) {
					System.out.println("Waiting...");
					continue;
				}
			}
		}
		catch (FileNotFoundException e) {
			//pass
		}
		finally {
			try {
				optStream.flush();
			}
			catch (Exception ex){
				System.out.println("OutputStreamed failed to clear, beginning retry...");
			}
		}
	}

	public synchronized boolean isPieceAvailable(int index) {
		return shradedFile.get(index);
	}

	public synchronized boolean isCompleteFile() {
		return shradedFile.cardinality() == CommonProperties.numberOfChunks;
	}

	public synchronized int getReceivedFileSize() {
		return shradedFile.cardinality();
	}

	protected synchronized int getRequestPieceIndex(ConnectionModel conn) {
		if (isCompleteFile()) {
			System.out.println("File received");
			return Integer.MIN_VALUE;
		}
		BitSet peerBitset = conn.getBitSetOfPeer();
		int numberOfPieces = CommonProperties.numberOfChunks;
		BitSet peerClone = (BitSet) peerBitset.clone();
		BitSet myClone = (BitSet) shradedFile.clone();
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

	

}