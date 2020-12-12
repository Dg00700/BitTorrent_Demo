import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Properties;
import java.util.Scanner;
import java.util.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;



public class BitTorrentMainController {
	public static String peerId;

	public static void main(String args[]) throws IOException {
		if(args!=null && args.length>0)
			peerId = args[0];
		else
			peerId = "1001";
		//init();
		CommonProperties.readPeerInfo();
		CommonProperties.loadDataFromConfig();
		CommonProperties.readConfigFile();
		MessageModel.makeHandshake(peerId);
		if (CommonProperties.getPeer(peerId).hasSharedFile) {
			FileHandler.getInstance().splitFile();
		}
		System.out.println("Peer Number:"+ peerId);
		CommonProperties.PrintConfigDetails();
		Node current = Node.getInstance();
		current.startClientModule();
		current.startListeningServer();

	}

}

class NetworkModel {

	public int networkId;
	public String peerId;
	public String hostName;
	public int port;
	public boolean hasSharedFile;

	public String getPeerId() {
		return peerId;
	}

	public void setHasSharedFile(boolean hasSharedFile) {
		this.hasSharedFile = hasSharedFile;
	}

	
}


class CommonProperties {

	public static int numberOfChunks;
	public static int nChunks;
	public static int numberOfPreferredNeighbors;
	public static int unchokingInterval;
	public static int optimisticUnchokingInterval;
	public static String fileName;
	public static long fileSize;
	public static int pieceSize;

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
	public static final String PROPERTIES_CONFIG_PATH = System.getProperty("user.dir") + File.separatorChar + "Common.cfg";
	public static final String PROPERTIES_FILE_PATH = System.getProperty("user.dir") + File.separatorChar;
	public static final String PROPERTIES_CREATED_FILE_PATH = System.getProperty("user.dir") + File.separatorChar + "project/peer_";
	public static final String PEER_PROPERTIES_CONFIG_PATH = System.getProperty("user.dir") + File.separatorChar + "PeerInfo.cfg";
	public static final String PEER_LOG_FILE_EXTENSION = ".log";
	public static final String PEER_LOG_FILE_PATH = System.getProperty("user.dir") + File.separatorChar + "project/log_peer_";




	public static void calculateNumberOfPieces() {
		
		int val = (int) (fileSize % pieceSize);
		nChunks = (int) (fileSize / pieceSize);
		if(val == 0){
			numberOfChunks=nChunks;
		}
		else{
			numberOfChunks = nChunks + 1;
		}
	}
	public static String last_peer;
	public static void readPeerInfo() {
		int num = 1;
		
	
		try {
			Scanner sc = new Scanner(new File(CommonProperties.PEER_PROPERTIES_CONFIG_PATH));
			while (sc.hasNextLine()) {
				String str[] = sc.nextLine().split(" ");
				NetworkModel network = new NetworkModel();
				network.networkId = num;
				num += 1;
				network.peerId= str[0];
				network.hostName = str[1];
				network.port = Integer.parseInt(str[2]);
				network.setHasSharedFile(str[3].equals("1") ? true : false);
				peerList.put(str[0], network);
				last_peer=str[0];
			}
			sc.close();
		} catch (IOException e) {
			System.out.println("PeerInfo.cfg not found/corrupt");
		}


	}

	
	public static void PrintConfigDetails() {
		System.out.println( "PeerProperties");
		System.out.println("numberOfPreferredNeighbors = " + numberOfPreferredNeighbors);  
		System.out.println("unchokingInterval = "+ unchokingInterval);
		System.out.println("optimisticUnchokingInterval = " + optimisticUnchokingInterval);
		System.out.println("fileName = "+ fileName);
		System.out.println("fileSize = " + fileSize);
		System.out.println("pieceSize = " + pieceSize);
	}

	public static void setNumberOfPreferredNeighbors(int numPreferredNeighbors) {

		numberOfPreferredNeighbors = numPreferredNeighbors;
	}

	public static String getTime() {

		String respTime =   Calendar.getInstance().getTime() + ": ";
		try {
			Boolean isTimeTrue = isNullOrEmptyString(respTime);
		}
		catch (Exception ex){}
		return respTime;
	}

	public static boolean isNullOrEmptyString(String data){
		if(data==null || (data!=null && data.length()==0) || (data!=null && data.trim().length()==0)){
			return false;
		}
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
		CommonProperties.pieceSize = Integer.parseInt(properties.getProperty(CommonProperties.PIECESIZE).toString());
		CommonProperties.unchokingInterval = 
				Integer.parseInt(properties.getProperty(CommonProperties.UNCHOKING_INTERVAL).toString());
		CommonProperties.calculateNumberOfPieces();
		System.out.println(CommonProperties.PROPERTIES_FILE_PATH);
		System.out.println(CommonProperties.PROPERTIES_FILE_PATH + CommonProperties.fileName);

	}


	public static void readConfigFile(){
		try{
			
			BufferedReader reader = new BufferedReader(new FileReader(CommonProperties.PROPERTIES_CONFIG_PATH));

            String str = reader.readLine();

            String[] args = str.split("\\s+");
			//int preferredNeighNum = Integer.parseInt(args[1]);
			CommonProperties.numberOfPreferredNeighbors = Integer.parseInt(args[1]);
			

            str = reader.readLine();
            args = str.split("\\s+");
            CommonProperties.unchokingInterval = Integer.parseInt(args[1]);

            str = reader.readLine();
            args = str.split("\\s+");
            CommonProperties.optimisticUnchokingInterval = Integer.parseInt(args[1]);

            str = reader.readLine();
            args = str.split("\\s+");
            CommonProperties.fileName = args[1];

            str = reader.readLine();
            args = str.split("\\s+");
            CommonProperties.fileSize = Integer.parseInt(args[1]);

            str = reader.readLine();
            args = str.split("\\s+");
            CommonProperties.pieceSize = Integer.parseInt(args[1]);

            reader.close();
            
        }
        
        catch(IOException ioEx){
            System.out.println("Val not found");
        }
    }
	

}

class Node {
	public static boolean didEveryoneReceiveTheFile = false;
	private static Node current = new Node();
	private NetworkModel networkModel;
	ConnectionController connectionController;

	public Node() {
		networkModel = CommonProperties.getPeer(BitTorrentMainController.peerId);
		connectionController = ConnectionController.getInstance();
	}

	public void startListeningServer()  {

		ServerSocket socket = null;
		try {
			socket = new ServerSocket(networkModel.port);
			while (!didEveryoneReceiveTheFile) {
				Socket peerSocket = socket.accept();
				connectionController.initiateConnection(peerSocket);
			}
		}
		catch (Exception e) {
			System.out.println("Closed exception");
		}
		finally {
			try{
				socket.close();
			}
			catch (Exception e) {
				System.out.println("Closed exception");
				e.printStackTrace();
			}
		}
	}

	public void startClientModule() {
		HashMap<String, NetworkModel> map = CommonProperties.getPeerList();
		int myNumber = networkModel.networkId;
		//System.out.println("HIIIII"+myNumber);
		for (String peerId : map.keySet()) {
			NetworkModel peerInfo = map.get(peerId);
			if (peerInfo.networkId < myNumber) {
				new Thread() {
					@Override
					public void run() {

						int peerPort = peerInfo.port;
						String peerHost = peerInfo.hostName;
						try {
							Socket clientSocket = new Socket(peerHost, peerPort);
							connectionController.startPeerConnection(clientSocket, peerInfo.getPeerId());
							Thread.sleep(300);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}.start();

			}
		}
	}

	public void checkIfAllpeerRecievedFile(){
		if(didEveryoneReceiveTheFile){
			if(current!=null){
				System.out.println("all peers Have recieved file.");
			}
		}
	}

	public static Node getInstance() {
		return current;
	}


	public NetworkModel getNetwork() {
		return networkModel;
	}

	public void close(){
		try{
			if(didEveryoneReceiveTheFile){
				if(current!=null){
					 System.out.println("all peers Have recieved file.");
				}
			}
		}
		catch (Exception ex){
			ex.printStackTrace();
		}
	}
}