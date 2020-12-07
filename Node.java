import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class Node {
	public static boolean didEveryoneReceiveTheFile = false;
	private static Node current = new Node();
	private NetworkModel networkModel;
	ConnectionController connectionController;

	public Node() {
		networkModel = CommonProperties.getPeer(BitTorrentMainController.peerId);
		connectionController = ConnectionController.getInstance();
	}

	public static Node getInstance() {
		return current;
	}


	public NetworkModel getNetwork() {
		return networkModel;
	}

	/*
	 listens for incoming connections from user.
	 */
	public void startMonitoringIncomingConnections()  {

		ServerSocket socket = null;
		try {
			socket = new ServerSocket(networkModel.port);
			while (!didEveryoneReceiveTheFile) {
				Socket peerSocket = socket.accept();
				connectionController.createConnection(peerSocket);
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

	public void startOutGoingConnections() {
		HashMap<String, NetworkModel> map = CommonProperties.getPeerList();
		int myNumber = networkModel.networkId;
		System.out.println("HIIIII"+myNumber);
		for (String peerId : map.keySet()) {
			NetworkModel peerInfo = map.get(peerId);
			if (peerInfo.networkId < myNumber) {
				new Thread() {
					@Override
					public void run() {

						createConnection(peerInfo);
					}
				}.start();

			}
		}
	}

	public void checkIfAllpeerRecievedFile(){
		if(didEveryoneReceiveTheFile){
			if(current!=null){
				//CommonProperties.DisplayMessageForUser(null, "all peers Have recieved file.");
			}
		}
	}

	private void createConnection(NetworkModel peerInfo) {
		int peerPort = peerInfo.port;
		String peerHost = peerInfo.hostName;
		try {
			Socket clientSocket = new Socket(peerHost, peerPort);
			connectionController.createConnection(clientSocket, peerInfo.getPeerId());
			Thread.sleep(300);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



	public void close(){
		try{
			checkIfAllpeerRecievedFile();
		}
		catch (Exception ex){
			System.out.println("Could not verify if all peers had file or not.");
		}
	}
}
