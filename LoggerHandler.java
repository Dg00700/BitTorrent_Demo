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
			System.out.println("Current Peer:" + PeerSetter.getInstance().getNetwork().getPeerId());
			File file = new File(PeerProperties.PEER_LOG_FILE_PATH + PeerSetter.getInstance().getNetwork().getPeerId()
				+ PeerProperties.PEER_LOG_FILE_EXTENSION);
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
		String msg=PeerProperties.getTime() + "Peer " + peerFrom + " makes a TCP Connection to Peer " + peerTo + ".";
		writeToFile(msg);
		
	}

	public void handshakeFrom(String peerFrom, String peerTo) {
		String msg=PeerProperties.getTime() + "Building HANDSHAKE from " + peerFrom + " to "+ peerTo + ".";
		writeToFile(msg);
		
	}

	public void bitfieldFrom(String peerFrom, String peerTo) {
		String msg=PeerProperties.getTime() + "Exchanging BITFIELD message from " + peerFrom + " to "+ peerTo + ".";
		writeToFile(msg);
		
	}


	public void connectionFrom(String peerFrom, String peerTo) {
		String msg=PeerProperties.getTime() + "Peer " + peerFrom + " is connected from Peer " + peerTo + ".";
		writeToFile(msg);
		
	}

	public void changePreferredNeighbors(String timestamp, String peerId, PriorityQueue<ConStructure> peers) {
		StringBuilder log = new StringBuilder();
		log.append(timestamp);
		log.append("Peer " + peerId + " has the preferred neighbors ");
		String prefix = "";
		Iterator<ConStructure> iter = peers.iterator();
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
		String msg=PeerProperties.getTime() +  "REQUEST to peer " + peer + ".";
		writeToFile(msg);
		
	}

	public void unchoked(String timestamp, String peerId1, String peerId2) {
		String msg=timestamp + "Peer " + peerId1 + " is unchoked by " + peerId2 + ".";
		writeToFile(msg);
		
	}

	public void choked(String timestamp, String peerId1, String peerId2) {
		String msg=timestamp + "Peer " + peerId1 + " is choked by " + peerId2 + ".";
		writeToFile(msg);

	}



	

	public void receiveInterested(String timestamp, String to, String from) {
		String msg=timestamp + "Peer " + to + " received the 'interested' message from " + from + ".";
		writeToFile(msg);
	}


	public void receiveNotInterested(String timestamp, String to, String from) {
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
		if(peerId==PeerProperties.last_peer)
		{
			PeerSetter.didEveryoneReceiveTheFile=true;
			PeerSetter node1=new PeerSetter();
			node1.checkIfAllpeerRecievedFile();


		}
	}



}