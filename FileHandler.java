// import java.nio.ByteBuffer;
// import java.nio.channels.FileChannel;
// import java.nio.file.StandardOpenOption;
// import java.util.Arrays;
// import java.util.BitSet;
// import java.util.HashMap;
// import java.util.Random;
// import java.util.concurrent.*;
// import java.io.*;


// public class FileHandler extends Thread {
// 	private volatile static BitSet shradedFile;
// 	private static FileChannel fileOutputChannel;
// 	private BlockingQueue<byte[]> file_q;
// 	private static FileHandler handlerInstance;
	
// 	private static ConcurrentHashMap<Integer, byte[]> file_map;
	
// 	private volatile HashMap<ConnectionModel, Integer> ask_file_pieces;

// 	static {
// 		file_map = new ConcurrentHashMap<Integer, byte[]>();
// 		shradedFile = new BitSet(CommonProperties.numberOfChunks);
// 		try {
// 			File newFile = new File(CommonProperties.PROPERTIES_CREATED_FILE_PATH + BitTorrentMainController.peerId
// 					+ File.separatorChar + CommonProperties.fileName);
// 			newFile.getParentFile().mkdirs(); 
// 			newFile.createNewFile();
// 			fileOutputChannel = FileChannel.open(newFile.toPath(), StandardOpenOption.WRITE);
// 		} catch (IOException e) {
// 			e.printStackTrace();
// 		}
// 	}



// 	private FileHandler() {
// 		file_q = new LinkedBlockingQueue<>();
// 		ask_file_pieces = new HashMap<>();
// 	}

// 	public static FileHandler getInstance() {
// 		synchronized (FileHandler.class) {
// 			if (null == handlerInstance) {
// 				handlerInstance = new FileHandler();
// 				handlerInstance.start();
// 			}
// 		}
// 		return handlerInstance;
// 	}


// 	public synchronized byte[] getFileChunkByIndex(int pieceIndex) {
// 		return file_map.get(pieceIndex);
// 	}

// 	private void readFileInChunks(int numberOfChunks,int fileSize, DataInputStream dInputStream) throws IOException{
// 		int pieceIndex = 0;
// 		for (int i = 0; i < CommonProperties.numberOfChunks; i++) {
// 			int chunkSize = i != numberOfChunks - 1 ? CommonProperties.pieceSize
// 					: fileSize % CommonProperties.pieceSize;
// 			byte[] piece = new byte[chunkSize];
// 			dInputStream.readFully(piece);
// 			file_map.put(pieceIndex, piece);
// 			shradedFile.set(pieceIndex++);
// 		}
// 	}

// 	public void splitFile() {
// 		File filePtr = new File(CommonProperties.PROPERTIES_FILE_PATH + CommonProperties.fileName);
// 		FileInputStream fInputStream = null;
// 		DataInputStream dInputStream = null;
// 		try {
// 			fInputStream = new FileInputStream(filePtr);
// 			dInputStream = new DataInputStream(fInputStream);
// 			try {
// 				readFileInChunks(CommonProperties.numberOfChunks, (int) CommonProperties.fileSize, dInputStream);
// 			}
// 			catch (IOException fileReadError) {
// 				fileReadError.printStackTrace();
				
// 			}

// 		}
// 		catch (FileNotFoundException e) {
// 			e.printStackTrace();
// 		}
// 		finally {
// 			try {
// 				fInputStream.close();
// 				dInputStream.close();
// 			} catch (IOException e) {
// 				e.printStackTrace();
				
// 			}
// 		}
// 	}



// 	@Override
// 	public void run() {
// 		while (true) {
// 			try {
// 				byte[] info = file_q.take();
// 				int pieceIndex = ByteBuffer.wrap(info, 0, 4).getInt();

// 			} catch (Exception e) {
				
// 			}

// 		}
// 	}

// 	public synchronized void setPiece(byte[] payload) {
// 		shradedFile.set(ByteBuffer.wrap(payload, 0, 4).getInt());
// 		file_map.put(ByteBuffer.wrap(payload, 0, 4).getInt(), Arrays.copyOfRange(payload, 4, payload.length));
// 		try {
// 			file_q.put(payload);
// 		} catch (InterruptedException Ex) {
// 			Ex.printStackTrace();
// 		}
// 	}

// 	private void loadFile(int pieceSize, int FileSize, int PieceSize) throws IOException {
// 		RandomAccessFile file_map = null;
// 		int totalPieces = (int)Math.ceil((double)(FileSize / PieceSize));
// 		for (int i = 0; i < totalPieces; i++) {
// 			int pieceLength = Math.min(PieceSize, FileSize - i * pieceSize);
// 			byte[] data = new byte[pieceLength];
// 			file_map.seek(i*pieceSize);
			
// 		}
// 	}
// 	public BitSet getFilePieces() {
// 		return shradedFile;
// 	}

// 	public synchronized boolean hasAnyPieces() {
// 		return shradedFile.nextSetBit(0) != -1;
// 	}

// 	public synchronized void addRequestedPiece(ConnectionModel connection, int pieceIndex) {
// 		ask_file_pieces.put(connection, pieceIndex);

// 	}

// 	public synchronized void removeRequestedPiece(ConnectionModel connection) {
// 		ask_file_pieces.remove(connection);
// 	}
// 	public synchronized void writeToFile(String peerId) {
// 		String fName = CommonProperties.PROPERTIES_CREATED_FILE_PATH + peerId + File.separatorChar + CommonProperties.fileName;
// 		System.out.println(fName);
// 		FileOutputStream optStream = null;
// 		try {
// 			optStream = new FileOutputStream(fName);
// 			for (int i = 0; i < file_map.size(); i++) {
// 				try {
// 					synchronized(this){
// 						if(optStream==null || file_map==null)
// 							continue;
// 						optStream.write(file_map.get(i));
// 					}
// 				} catch (Exception e) {
// 					System.out.println("Waiting...");
// 					continue;
// 				}
// 			}
// 		}
// 		catch (FileNotFoundException e) {
// 			//pass
// 		}
// 		finally {
// 			try {
// 				optStream.flush();
// 			}
// 			catch (Exception ex){
// 				System.out.println("OutputStreamed failed to clear, beginning retry...");
// 			}
// 		}
// 	}

// 	public synchronized boolean isPieceAvailable(int index) {
// 		return shradedFile.get(index);
// 	}

// 	public synchronized boolean isCompleteFile() {
// 		return shradedFile.cardinality() == CommonProperties.numberOfChunks;
// 	}

// 	public synchronized int getReceivedFileSize() {
// 		return shradedFile.cardinality();
// 	}

// 	protected synchronized int getRequestPieceIndex(ConnectionModel conn) {
// 		if (isCompleteFile()) {
// 			System.out.println("File received");
// 			return Integer.MIN_VALUE;
// 		}
// 		BitSet peerBitset = conn.getBitSetOfPeer();
// 		int numberOfPieces = CommonProperties.numberOfChunks;
// 		BitSet peerClone = (BitSet) peerBitset.clone();
// 		BitSet myClone = (BitSet) shradedFile.clone();
// 		peerClone.andNot(myClone);
// 		if (peerClone.cardinality() == 0) {
// 			return Integer.MIN_VALUE;
// 		}
// 		myClone.flip(0, numberOfPieces);
// 		myClone.and(peerClone);
// 		System.out.println(peerClone + " " + myClone);
// 		int[] missingPieces = myClone.stream().toArray();
// 		return missingPieces[new Random().nextInt(missingPieces.length)];
// 	}

	

// }
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.*;
import java.io.*;


public class FileHandler extends Thread {
	private volatile static BitSet shradedFile;
	private static FileChannel fileOutputChannel;
	private BlockingQueue<byte[]> file_q;
	private static FileHandler handlerInstance;
	
	private static ConcurrentHashMap<Integer, byte[]> file_map;
	
	private volatile HashMap<ConnectionModel, Integer> ask_file_pieces;

	static {
		file_map = new ConcurrentHashMap<Integer, byte[]>();
		shradedFile = new BitSet(CommonProperties.numberOfChunks);
		try {
			File newFile = new File(CommonProperties.PROPERTIES_CREATED_FILE_PATH + BitTorrentMainController.peerId
					+ File.separatorChar + CommonProperties.fileName);
			newFile.getParentFile().mkdirs(); 
			newFile.createNewFile();
			fileOutputChannel = FileChannel.open(newFile.toPath(), StandardOpenOption.WRITE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}



	private FileHandler() {
		file_q = new LinkedBlockingQueue<>();
		ask_file_pieces = new HashMap<>();
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


	public synchronized byte[] readPieceOfFile(int piece_ind) {
		return file_map.get(piece_ind);
	}

	private void readFileInChunks(int no_of_pieces,int fileSize, DataInputStream dInputStream) throws IOException{
		int piece_ind = 0;
		for (int i = 0; i < CommonProperties.numberOfChunks; i++) {
			int chunkSize = i != no_of_pieces - 1 ? CommonProperties.pieceSize
					: fileSize % CommonProperties.pieceSize;
			byte[] piece = new byte[chunkSize];
			dInputStream.readFully(piece);
			file_map.put(piece_ind, piece);
			shradedFile.set(piece_ind++);
		}
	}

	public void parseFile() {
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
				byte[] info = file_q.take();
				int piece_ind = ByteBuffer.wrap(info, 0, 4).getInt();

			} catch (Exception e) {
				
			}

		}
	}

	public synchronized void setFilePiece(byte[] payload) {
		shradedFile.set(ByteBuffer.wrap(payload, 0, 4).getInt());
		file_map.put(ByteBuffer.wrap(payload, 0, 4).getInt(), Arrays.copyOfRange(payload, 4, payload.length));
		try {
			file_q.put(payload);
		} catch (InterruptedException Ex) {
			Ex.printStackTrace();
		}
	}

	private void loadFile(int pieceSize, int FileSize, int PieceSize) throws IOException {
		RandomAccessFile file_map = null;
		int totalPieces = (int)Math.ceil((double)(FileSize / PieceSize));
		for (int i = 0; i < totalPieces; i++) {
			int pieceLength = Math.min(PieceSize, FileSize - i * pieceSize);
			byte[] data = new byte[pieceLength];
			file_map.seek(i*pieceSize);
			
		}
	}
	public BitSet getPieces() {
		return shradedFile;
	}

	public synchronized boolean hasAnyPieces() {
		return shradedFile.nextSetBit(0) != -1;
	}

	// public synchronized void addRequestedPiece(ConnectionModel connection, int piece_ind) {
	// 	ask_file_pieces.put(connection, piece_ind);

	// }

	public synchronized void removeRequestedPiece(ConnectionModel connection) {
		ask_file_pieces.remove(connection);
	}
	public synchronized int getFileLength() {
		return shradedFile.cardinality();
	}

	public synchronized void writeToFile(String peerId) {
		String fName = CommonProperties.PROPERTIES_CREATED_FILE_PATH + peerId + File.separatorChar + CommonProperties.fileName;
		System.out.println(fName);
		FileOutputStream optStream = null;
		try {
			optStream = new FileOutputStream(fName);
			for (int i = 0; i < file_map.size(); i++) {
				try {
					synchronized(this){
						if(optStream==null || file_map==null)
							continue;
						optStream.write(file_map.get(i));
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

	public synchronized boolean is_available(int index) {
		return shradedFile.get(index);
	}

	public synchronized boolean isFullFile() {
		return shradedFile.cardinality() == CommonProperties.numberOfChunks;
	}

	
	protected synchronized int receivedRequestedPiece(ConnectionModel conn) {
		if (isFullFile()) {
			System.out.println("File received");
			return Integer.MIN_VALUE;
		}
		BitSet peer_bit = conn.getBitSetOfPeer();
		int noOfPieces = CommonProperties.numberOfChunks;
		BitSet peer_copy = (BitSet) peer_bit.clone();
		BitSet my_copy = (BitSet) shradedFile.clone();
		peer_copy.andNot(my_copy);
		if (peer_copy.cardinality() == 0) {
			return Integer.MIN_VALUE;
		}
		my_copy.flip(0, noOfPieces);
		my_copy.and(peer_copy);
		System.out.println(peer_copy + " " + my_copy);
		int[] missingPieces = my_copy.stream().toArray();
		return missingPieces[new Random().nextInt(missingPieces.length)];
	}

	

}