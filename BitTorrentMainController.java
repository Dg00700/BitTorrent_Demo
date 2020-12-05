import java.io.IOException;

public class BitTorrentMainController {
	public static String peerId;

	public static void main(String args[]) throws IOException {
		if(args!=null && args.length>0)
			peerId = args[0];
		else
			peerId = "1003";
		init();

	}

	private static void init() {
		CommonProperties.loadDataFromConfig();
		MessageModel.setId(peerId);
		if (CommonProperties.getPeer(peerId).hasSharedFile) {
			FileHandler.getInstance().splitFile();
		}
		System.out.println("I am peer :"+ peerId);
		CommonProperties.DisplayConfigDetails();
		Node current = Node.getInstance();
		current.startOutGoingConnections();
		current.startMonitoringIncomingConnections();
	}

}
