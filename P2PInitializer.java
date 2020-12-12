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



public class P2PInitializer {
	public static String peerId;

	public static void main(String args[]) throws IOException {
		if(args!=null && args.length>0)
			peerId = args[0];
		else
			peerId = "1001";
		//init();
		PeerProperties.readPeerInfo();
		PeerProperties.loadDataFromConfig();
		PeerProperties.readConfigFile();
		MessageBody.makeHandshake(peerId);
		if (PeerProperties.getPeer(peerId).hasSharedFile) {
			FileHandler.getInstance().parseFile();
		}
		System.out.println("Peer Number:"+ peerId);
		PeerProperties.PrintConfigDetails();
		PeerSetter current = PeerSetter.getInstance();
		current.startClientModule();
		current.startListeningServer();

	}

}

class PeerNetwork {

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


class PeerProperties {

	public static int numberOfChunks;
	public static int nChunks;
	public static int numberOfPreferredNeighbors;
	public static int unchokingInterval;
	public static int optimisticUnchokingInterval;
	public static String fileName;
	public static long fileSize;
	public static int pieceSize;

	private static HashMap<String, PeerNetwork> peerList = new HashMap<>();


	public static PeerNetwork getPeer(String id) {
		return peerList.get(id);
	}

	public static HashMap<String, PeerNetwork> getPeerList() {
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
			Scanner sc = new Scanner(new File(PeerProperties.PEER_PROPERTIES_CONFIG_PATH));
			while (sc.hasNextLine()) {
				String str[] = sc.nextLine().split(" ");
				PeerNetwork network = new PeerNetwork();
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
			FileInputStream in = new FileInputStream(PeerProperties.PROPERTIES_CONFIG_PATH);
			properties.load(in);
		}
		catch (Exception ex) {
			System.out.println("File not found : " + ex.getMessage());
		}

		PeerProperties.fileName = properties.get(PeerProperties.FILENAME).toString();
		PeerProperties.fileSize = Long.parseLong(properties.get(PeerProperties.FILESIZE).toString());
		PeerProperties.setNumberOfPreferredNeighbors(
				Integer.parseInt(properties.get(PeerProperties.NUMBER_OF_PREFERRED_NEIGHBORS).toString()));
				PeerProperties.optimisticUnchokingInterval = 
				Integer.parseInt(properties.get(PeerProperties.OPTIMISTIC_UNCHOKING_INTERVAL).toString());
				PeerProperties.pieceSize = Integer.parseInt(properties.getProperty(PeerProperties.PIECESIZE).toString());
				PeerProperties.unchokingInterval = 
				Integer.parseInt(properties.getProperty(PeerProperties.UNCHOKING_INTERVAL).toString());
				PeerProperties.calculateNumberOfPieces();
		System.out.println(PeerProperties.PROPERTIES_FILE_PATH);
		System.out.println(PeerProperties.PROPERTIES_FILE_PATH + PeerProperties.fileName);

	}


	public static void readConfigFile(){
		try{
			
			BufferedReader reader = new BufferedReader(new FileReader(PeerProperties.PROPERTIES_CONFIG_PATH));

            String str = reader.readLine();

            String[] args = str.split("\\s+");
			//int preferredNeighNum = Integer.parseInt(args[1]);
			PeerProperties.numberOfPreferredNeighbors = Integer.parseInt(args[1]);
			

            str = reader.readLine();
            args = str.split("\\s+");
            PeerProperties.unchokingInterval = Integer.parseInt(args[1]);

            str = reader.readLine();
            args = str.split("\\s+");
            PeerProperties.optimisticUnchokingInterval = Integer.parseInt(args[1]);

            str = reader.readLine();
            args = str.split("\\s+");
            PeerProperties.fileName = args[1];

            str = reader.readLine();
            args = str.split("\\s+");
            PeerProperties.fileSize = Integer.parseInt(args[1]);

            str = reader.readLine();
            args = str.split("\\s+");
            PeerProperties.pieceSize = Integer.parseInt(args[1]);

            reader.close();
            
        }
        
        catch(IOException ioEx){
            System.out.println("Val not found");
        }
    }
	

}

class PeerSetter {
	public static boolean didEveryoneReceiveTheFile = false;
	private static PeerSetter current = new PeerSetter();
	private PeerNetwork peerNetwork;
	ConnectionHandler connectionHandler;

	public PeerSetter() {
		peerNetwork = PeerProperties.getPeer(P2PInitializer.peerId);
		connectionHandler = ConnectionHandler.getInstance();
	}

	public void startListeningServer()  {

		ServerSocket socket = null;
		try {
			socket = new ServerSocket(peerNetwork.port);
			while (!didEveryoneReceiveTheFile) {
				Socket peerSocket = socket.accept();
				connectionHandler.initiateConnection(peerSocket);
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
		HashMap<String, PeerNetwork> map = PeerProperties.getPeerList();
		int myNumber = peerNetwork.networkId;
		//System.out.println("HIIIII"+myNumber);
		for (String peerId : map.keySet()) {
			PeerNetwork peerInfo = map.get(peerId);
			if (peerInfo.networkId < myNumber) {
				new Thread() {
					@Override
					public void run() {

						int peerPort = peerInfo.port;
						String peerHost = peerInfo.hostName;
						try {
							Socket clientSocket = new Socket(peerHost, peerPort);
							connectionHandler.startPeerConnection(clientSocket, peerInfo.getPeerId());
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

	public static PeerSetter getInstance() {
		return current;
	}


	public PeerNetwork getNetwork() {
		return peerNetwork;
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