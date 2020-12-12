
import java.io.*;
import java.util.*;

public class StartRemotePeers {

	public Vector<RemotePeerInfo> peerInfoVector;

    public void getConfiguration() {
        String st;
        int i1=0;
        peerInfoVector = new Vector<RemotePeerInfo>();
        try {
            BufferedReader in = new BufferedReader(new FileReader("PeerInfo.cfg"));
            while ((st = in.readLine()) != null) {

                String[] tokens = st.split("\\s+");
                
                peerInfoVector.addElement(new RemotePeerInfo(tokens[0], tokens[1], tokens[2],i1));
                i1++;
            }

            in.close();
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        try {
            StartRemotePeers myStart = new StartRemotePeers();
            myStart.getConfiguration();
            // for (int index = 0; index < peerInfoVector.size(); index++)
            // {
            // System.out.println(peerInfoVector.get(index));
            // }		
			// get current path
			String path = System.getProperty("user.dir");
			
			//start clients at remote hosts
			for (int i = 0; i < myStart.peerInfoVector.size(); i++) {
				RemotePeerInfo pInfo = (RemotePeerInfo) myStart.peerInfoVector.elementAt(i);
				
				System.out.println("Start remote peer " + pInfo.peerId +  " at " + pInfo.peerAddress );
				
			    Runtime.getRuntime().exec("ssh " + pInfo.peerAddress + " cd " + path + "; java P2PInitializer " + pInfo.peerId);
				
			}		
			//System.out.println("Starting all remote peers has done." );

        }
    
		catch (Exception ex) {
			System.out.println(ex);
		}
	}

}
class RemotePeerInfo {
    public String peerId;
    public String peerAddress;
    public String portno;
    public String hasFile;
    public int pIndex;

    public RemotePeerInfo(String peerId,String peerAddress,String portno,int pIndex) {
        this.peerId=peerId;
        this.peerAddress=peerAddress;
        this.portno=portno;
        this.pIndex=pIndex;
    }
}