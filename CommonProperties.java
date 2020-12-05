import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Properties;
import java.util.Scanner;

public class CommonProperties {

	public static int numberOfChunks;
	public static int numberOfPreferredNeighbors;
	public static int unchokingInterval;
	public static int optimisticUnchokingInterval;
	public static String fileName;
	public static long fileSize;
	public static int chunkSize;

	private static HashMap<String, NetworkModel> peerList = new HashMap<>();


	public static NetworkModel getPeer(String id) {
		return peerList.get(id);
	}

	public static HashMap<String, NetworkModel> getPeerList() {
		return peerList;
	}

	public static int numberOfPeers() {
		return peerList.size();
	}

	public static final String NUMBER_OF_PREFERRED_NEIGHBORS = "NumberOfPreferredNeighbors";
	public static final String UNCHOKING_INTERVAL = "UnchokingInterval";
	public static final String OPTIMISTIC_UNCHOKING_INTERVAL = "OptimisticUnchokingInterval";
	public static final String FILENAME = "FileName";
	public static final String FILESIZE = "FileSize";
	public static final String PIECESIZE = "PieceSize";
	public static final String PROPERTIES_CONFIG_PATH = System.getProperty("user.dir") + File.separatorChar
			+ "Common.cfg";
	public static final String PROPERTIES_FILE_PATH = System.getProperty("user.dir") + File.separatorChar;
	public static final String PROPERTIES_CREATED_FILE_PATH = System.getProperty("user.dir") + File.separatorChar
			+ "project/peer_";
	public static final String PEER_PROPERTIES_CONFIG_PATH = System.getProperty("user.dir") + File.separatorChar
			+ "PeerInfo.cfg";
	public static final String PEER_LOG_FILE_EXTENSION = ".log";
	public static final String PEER_LOG_FILE_PATH = System.getProperty("user.dir") + File.separatorChar
			+ "project/log_peer_";




	public static void calculateNumberOfPieces() {
		numberOfChunks = (int) (fileSize % chunkSize) == 0 ? (int) (fileSize / chunkSize)
				: (int) (fileSize / chunkSize) + 1;
		System.out.println("CommonProperties.calculateNumberOfPieces - Number of pieces: " + numberOfChunks);
	}

	static {
		int id = 1;
		try {
			Scanner sc = new Scanner(new File(CommonProperties.PEER_PROPERTIES_CONFIG_PATH));
			while (sc.hasNextLine()) {
				String str[] = sc.nextLine().split(" ");
				NetworkModel network = new NetworkModel();
				network.networkId = id++;
				network.peerId= str[0];
				network.hostName = str[1];
				network.port = Integer.parseInt(str[2]);
				network.setHasSharedFile(str[3].equals("1") ? true : false);
				peerList.put(str[0], network);
			}
			sc.close();
		} catch (IOException e) {
			System.out.println("PeerInfo.cfg not found/corrupt");
		}


	}

	public static void DisplayMessageForUser(Object instance, String message){
		try {
			String sender = "";
			if (instance != null)
				sender = instance.toString();
			else
				sender = "Unknown";
			System.out.println("Sender " + sender + " Message : " + message);
		}
		catch (Exception ex){
			System.out.println(message);
		}
	}

	public static void DisplayConfigDetails() {
		System.out.println( "PeerProperties [numberOfPreferredNeighbors=" + numberOfPreferredNeighbors + ", unchokingInterval="
				+ unchokingInterval + ", optimisticUnchokingInterval=" + optimisticUnchokingInterval + ", fileName="
				+ fileName + ", fileSize=" + fileSize + ", pieceSize=" + chunkSize + "]");
	}

	public static void setNumberOfPreferredNeighbors(int numPreferredNeighbors) {

		numberOfPreferredNeighbors = numPreferredNeighbors;
	}

	public static String getTime() {

		String respTime =   Calendar.getInstance().getTime() + ": ";
		try {
			Boolean isTimeTrue = isNullOrEmptyString(respTime);
		}
		catch (Exception ex){
			//skip
		}
		return respTime;
	}

	public static boolean isNullOrEmptyString(String data){
		if(data==null)
			return false;
		if(data!=null && data.length()==0)
			return false;
		if(data!=null && data.trim().length()==0)
			return false;
		else
			return true;
	}
		public static void loadDataFromConfig() {

		Properties properties = new Properties();
		try {
			FileInputStream in = new FileInputStream(CommonProperties.PROPERTIES_CONFIG_PATH);
			properties.load(in);
		}
		catch (Exception ex) {
			System.out.println("File not found : " + ex.getMessage());
		}

		CommonProperties.fileName = properties.get(CommonProperties.FILENAME).toString();
		CommonProperties.fileSize = Long.parseLong(properties.get(CommonProperties.FILESIZE).toString());
		CommonProperties.setNumberOfPreferredNeighbors(
				Integer.parseInt(properties.get(CommonProperties.NUMBER_OF_PREFERRED_NEIGHBORS).toString()));
		CommonProperties.optimisticUnchokingInterval =
				Integer.parseInt(properties.get(CommonProperties.OPTIMISTIC_UNCHOKING_INTERVAL).toString());
		CommonProperties.chunkSize = Integer.parseInt(properties.getProperty(CommonProperties.PIECESIZE).toString());
		CommonProperties.unchokingInterval =
				Integer.parseInt(properties.getProperty(CommonProperties.UNCHOKING_INTERVAL).toString());
		CommonProperties.calculateNumberOfPieces();
		System.out.println(CommonProperties.PROPERTIES_FILE_PATH);
		System.out.println(CommonProperties.PROPERTIES_FILE_PATH + CommonProperties.fileName);

	}

}
