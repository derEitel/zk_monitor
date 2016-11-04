
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;



public class Client {

	static String InstanceManagerRoot = "/nodes/instances";
	static String OrchestratorRoot = "/nodes/orchestrators";
	
	static String InstanceManagerFlags = "/flags/instances";
	static String OrchestratorFlags = "/flags/orchestrators";
	
	private ZooKeeper zkeeper;
	private String rootNode;
	private String rootFlags;
	private List<String> zNodes;
	private List<String> prevNodes = new LinkedList<String>();
	private int prevNodesLength = 0;

    public Client(ZooKeeper zk, String rootNode, String rootFlags) throws KeeperException, IOException {
    	/*
    	 * Client class that will start a watcher for a node type.
    	 * Reports whether nodes have been added, shutdown or killed.
    	 * 
    	 * Use this class to start the project and to setup nodes via keyboard input.
    	 * 
    	 */
    	
    	this.zkeeper = zk;
    	this.rootNode = rootNode;
    	this.rootFlags = rootFlags;

        // Start the watcher
        try {
			zNodes = zkeeper.getChildren(rootNode, startWatcher());
			prevNodesLength = zNodes.size();
			//System.out.println(zNodes);
			if(!zNodes.isEmpty()){
				prevNodes = zNodes;
				zNodes = null;
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    }
    
    public Watcher startWatcher(){ 
    	// Function to create new watchers, once they are triggered
    	Watcher childWatcher = new Watcher() {
	    	public void process(WatchedEvent we){
	    		//System.out.println(we);
	    		
	    		//
	    		// Analyse node change behaviour (started/stopped/died)
	    		//
	    		try {
	    			zNodes = zkeeper.getChildren(rootNode, this);
					System.out.println(zNodes);
					if (prevNodesLength < zNodes.size()) {
						
						// Node was added
						
						if(!zNodes.isEmpty()){
							List<String> changes = new LinkedList<String>(zNodes);
							changes.removeAll(prevNodes);
							System.out.println("Node started: " + changes);
							System.out.println("Prev length: " + prevNodesLength + " current length: " + zNodes.size());
						} else {
							System.out.println("No znodes. Error.");
						}
						
	    			} else if (prevNodesLength > zNodes.size()){
	    				
	    				// Node was removed
	    				
	    				if(!prevNodes.isEmpty()){
	    					List<String> changes = new LinkedList<String>(prevNodes);
							changes.removeAll(zNodes);
							
							// Get flag status to see whether it died or was shutdown
							byte b[] = null;
							for (int i=0; i <= changes.size() -1; i++){
								// Load data from flag
								b = zkeeper.getData(rootFlags + "/" + changes.get(i), null, null);
								String status = new String(b, "UTF-8");
								switch(status){
									case "offline":
										System.out.println("Node was shutdown: " + changes);
										break;
									case "online":
										System.out.println("Problem: Node died: " + changes);
										break;
								}
							}
		    				System.out.println("Prev length: " + prevNodesLength + " current length: " + zNodes.size());
	    				}
	    			}
					prevNodes = zNodes;
					prevNodesLength = prevNodes.size();
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    	}
    	};
    	return childWatcher;
    }
	


    public static void main(String[] args) {
    	ZkConnect connector = new ZkConnect();
        ZooKeeper zk = null;
        
		try {
			zk = connector.connect("127.0.0.1");
			
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        

		// Creating all root nodes necessary
        try{
        	if(!connector.existsNode("/nodes")){
        		connector.createNode("/nodes", "".getBytes(), "persistent");
				System.out.println("Nodes root created.");
			}
        	if(!connector.existsNode(InstanceManagerRoot)){
        		connector.createNode(InstanceManagerRoot, "".getBytes(), "persistent");
				System.out.println("Instance Manager root created.");
			}    
        	if(!connector.existsNode(OrchestratorRoot)){
        		connector.createNode(OrchestratorRoot, "".getBytes(), "persistent");
				System.out.println("Orchestrator root created.");
			} 
        	if(!connector.existsNode("/flags")){
        		connector.createNode("/flags", "".getBytes(), "persistent");
				System.out.println("Flags root created.");
			} 
        	if(!connector.existsNode(InstanceManagerFlags)){
        		connector.createNode(InstanceManagerFlags, "".getBytes(), "persistent");
				System.out.println("Flags IM root created.");
			} 
        	if(!connector.existsNode(OrchestratorFlags)){
        		connector.createNode(OrchestratorFlags, "".getBytes(), "persistent");
				System.out.println("Flags OC root created.");
			} 
        } catch (Exception e){
        	//e.printStackTrace();
        }
		
        // Create an instance of Class Client for each type
		try {
			new Client(zk, InstanceManagerRoot, InstanceManagerFlags);
			new Client(zk, OrchestratorRoot, OrchestratorFlags);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		boolean finished = false;
		System.out.println("Create Instances: (1) Instance Manager (2) Orchestrator (3) Shutdown IM (4) Shutdown OC (5) Kill IM (6) Kill OC (0) Quit");
		// Kill and shutdown will automatically kill the latest node
		
		AtomicInteger IMCount = new AtomicInteger(0);
		AtomicInteger OCCount = new AtomicInteger(0);
		
		while (!finished){
			byte[] bytes = new byte[10];
	    	
			//
			// Listen to user input to create/shutdown/kill nodes
			//
			
	    	try {
	        	System.in.read(bytes);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	
	    	switch(bytes[0]){
	    		case 49:
	    			//press 1
	    			new InstanceManager("/instanceManager" + IMCount.getAndIncrement(), "abc", "ephemeral", connector);
	    			break;
	    		case 50:
	    			//2
	    			new Orchestrator("/orchestrator" + OCCount.getAndIncrement(), "abc", "ephemeral", connector);
	    			break;
	    		case 51:
	    			//3
	    			InstanceManager.shutDown("/instanceManager" + IMCount.decrementAndGet(), connector);
	    			break;
	    		case 52:
	    			//4
	    			Orchestrator.shutDown("/orchestrator" + OCCount.decrementAndGet(), connector);
	    			break;
	    		case 53:
	    			//5
	    			InstanceManager.killNode("/instanceManager" + IMCount.decrementAndGet(), connector);
	    			break;
	    		case 54:
	    			//6
	    			Orchestrator.killNode("/orchestrator" + OCCount.decrementAndGet(), connector);
	    			break;
	    		case 48:
	    			finished = true;
	    			break;
				default:
					System.out.println("Wrong input.");
	    	}
    	}
		
    }

}
