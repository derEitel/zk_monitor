import java.util.Date;
import java.util.List;

import org.apache.zookeeper.ZooKeeper;
//import ZkConnect;


public class Orchestrator {

	static String nodeType = "ephemeral";
	static String mainPath = "/nodes/orchestrators";
	static String flags = "/flags/orchestrators";
	
	public Orchestrator(String path, String data, String nodeType, ZkConnect connector){
		/*
		 * Orchestrator class
		 * Can be used to interact with instances
		 * 
		 */
		
		String pathFull = mainPath + path;
		System.out.println("Creating node " + pathFull);
		
		//
		// Setup a flag
		//
		try {
			if(connector.existsNode(flags + path)){
				// If flag already exists update status
				connector.updateNode(flags + path, "started".getBytes());
			} else {
				// Create flag and set status as started
				connector.createNode(flags + path, "started".getBytes(), "persistent");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
		//
        // Register as Orchestrator
		//
		try {
			if(connector.existsNode(pathFull)){
				System.out.println("Node already exsists. Please use different path.");
			} else {
				// Create node with specified type
				connector.createNode(pathFull, data.getBytes(), nodeType);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//
		// Set flag status to online
		//
		try {
			if(connector.existsNode(pathFull)){
				connector.updateNode(flags + path, "online".getBytes());
			} else {
				// If node is not running set flag to offline
				connector.updateNode(flags + path, "offline".getBytes());
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public static void shutDown(String path, ZkConnect connector) {
		//
		// Shutdown an instance with changing the flag status 
		//
		
		// Change flag status
		try {
			if(connector.existsNode(flags + path)){
				// If flag already exists update status
				connector.updateNode(flags + path, "offline".getBytes());
			} else {
				System.out.println("Error: Node does not exist");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Remove node
		try {
			connector.deleteNode(mainPath + path);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void killNode(String path, ZkConnect connector){
		//
		// Kill an instance without changing the flag status 
		//
		
		// Remove node
		try {
			connector.deleteNode(mainPath + path);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
