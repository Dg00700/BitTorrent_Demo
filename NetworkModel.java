public class NetworkModel {

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

	@Override
	public String toString() {
		return "Peer [peerId=" + peerId + ", hostName=" + hostName + ", port=" + port + ", hasSharedFile="
				+ hasSharedFile + "]";
	}
}
