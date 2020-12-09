import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.PriorityQueue;

public class LoggerHandler {

	private static LoggerHandler instance;

	public static PrintWriter printWriter = null;

	private LoggerHandler() {
		try {
			System.out.println("Current Peer:" + Node.getInstance().getNetwork().getPeerId());
			File file = new File(CommonProperties.PEER_LOG_FILE_PATH + Node.getInstance().getNetwork().getPeerId()
				+ CommonProperties.PEER_LOG_FILE_EXTENSION);
			file.getParentFile().mkdirs();
			file.createNewFile();
			FileOutputStream fileOutputStream = new FileOutputStream(file, false);
			printWriter = new PrintWriter(fileOutputStream, true);
		}
		catch (Exception ex) {
			System.out.println("Error: Log Handler "+ ex.getMessage());
		}
	}
	public static LoggerHandler getInstance() {
		synchronized (LoggerHandler.class) {
			if (instance == null) {
				instance = new LoggerHandler();
			}
		}
		return instance;
	}


	public void receiveHave(String timestamp, String to, String from, int pieceIndex) {


		writeToFile(timestamp + "Peer " + to + " received the 'have' message from " + from + " for the piece "
				+ pieceIndex + ".");
	}

	private void writeToFile(String message) {
		synchronized (this) {
			printWriter.println(message);
		}
	}

	public void connectionTo(String peerFrom, String peerTo) {
		String msg=CommonProperties.getTime() + "Peer " + peerFrom + " makes a TCP Connection to Peer " + peerTo + ".";
		writeToFile(msg);
		
	}

	public void handshakeFrom(String peerFrom, String peerTo) {
		String msg=CommonProperties.getTime() + "Building HANDSHAKE from " + peerFrom + " to "+ peerTo + ".";
		writeToFile(msg);
		// StringBuffer log = new StringBuffer();
		// log.append(CommonProperties.getTime() + "Peer " + peerFrom + " is connected from Peer " + peerTo + ".");
		// writeToFile(log.toString() );
	}

	public void bitfieldFrom(String peerFrom, String peerTo) {
		String msg=CommonProperties.getTime() + "Exchanging BITFIELD message from " + peerFrom + " to "+ peerTo + ".";
		writeToFile(msg);
		// StringBuffer log = new StringBuffer();
		// log.append(CommonProperties.getTime() + "Peer " + peerFrom + " is connected from Peer " + peerTo + ".");
		// writeToFile(log.toString());
	}


	public void connectionFrom(String peerFrom, String peerTo) {
		String msg=CommonProperties.getTime() + "Peer " + peerFrom + " is connected from Peer " + peerTo + ".";
		writeToFile(msg);
		// StringBuffer log = new StringBuffer();
		// log.append(CommonProperties.getTime() + "Peer " + peerFrom + " is connected from Peer " + peerTo + ".");
		// writeToFile(log.toString() + ".");
	}

	public void changePreferredNeighbors(String timestamp, String peerId, PriorityQueue<ConnectionModel> peers) {
		StringBuilder log = new StringBuilder();
		log.append(timestamp);
		log.append("Peer " + peerId + " has the preferred neighbors ");
		String prefix = "";
		Iterator<ConnectionModel> iter = peers.iterator();
		while (iter.hasNext()) {
			log.append(prefix);
			prefix = ", ";
			log.append(iter.next().getRemotePeerId());
		}
		writeToFile(log.toString() + ".");
	}


	public void changeOptimisticallyUnchokeNeighbor(String timestamp, String source, String unchokedNeighbor) {
		writeToFile(timestamp + "Peer " + source + " has the optimistically unchoked neighbor " + unchokedNeighbor + ".");
		
	}
	public void requestFrom(String peer) {
		String msg=CommonProperties.getTime() +  "REQUEST to peer " + peer + ".";
		writeToFile(msg);
		// StringBuffer log = new StringBuffer();
		// log.append(timestamp + "Peer " + source + " has the optimistically unchoked neighbor " + unchokedNeighbor + ".");
		// writeToFile(log.toString() + ".");
	}

	public void unchoked(String timestamp, String peerId1, String peerId2) {
		String msg=timestamp + "Peer " + peerId1 + " is unchoked by " + peerId2 + ".";
		writeToFile(msg);
		// StringBuffer log = new StringBuffer();
		// log.append(timestamp + "Peer " + peerId1 + " is unchoked by " + peerId2 + ".");
		// writeToFile(log.toString() + ".");
	}

	public void choked(String timestamp, String peerId1, String peerId2) {
		String msg=timestamp + "Peer " + peerId1 + " is choked by " + peerId2 + ".";
		writeToFile(msg);
		// StringBuffer log = new StringBuffer();
		// log.append(timestamp + "Peer " + peerId1 + " is choked by " + peerId2 + ".");
		// writeToFile(log.toString() + ".");

	}



	

	public void receiveInterested(String timestamp, String to, String from) {
		// StringBuffer log = new StringBuffer();
		// log.append(timestamp + "Peer " + to + " received the 'interested' message from " + from + ".");
		// writeToFile(log.toString() + ".");
		String msg=timestamp + "Peer " + to + " received the 'interested' message from " + from + ".";
		writeToFile(msg);
	}


	public void receiveNotInterested(String timestamp, String to, String from) {
		// StringBuffer log = new StringBuffer();
		// log.append(':'+" Peer "+to+" has downloaded the piece from "+ from +".");
		// writeToFile(log.toString() + ".");
		String msg=timestamp + "Peer " + to + " received the 'not interested' message from " + from + ".";
		writeToFile(msg);
	}


	public void downloadingPiece(String timestamp, String to, String from, int pieceIndex, int numberOfPieces) {
		String msg = timestamp + "Peer " + to + " has downloaded the 'PIECE' " + pieceIndex + " from " + from + ".";
		msg += "Now the number of pieces it has is " + numberOfPieces;
		writeToFile(msg);

	}

	public void downloadComplete(String timestamp, String peerId) {

		writeToFile(timestamp + "Peer " + peerId + " has downloaded the complete file.");
		if(peerId==CommonProperties.last_peer)
		{
			Node.didEveryoneReceiveTheFile=true;
			Node node1=new Node();
			node1.checkIfAllpeerRecievedFile();


		}
	}



}