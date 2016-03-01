//Thrift imports
import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.*;

import java.util.HashMap;
import java.util.List;

public class NodeHandler implements Node.iface{
			
	DHT table;
	Integer nodeID;
	Integer port; 
	
	@Override
	boolean write(String Filename,String Contents) throws org.apache.thrift.TException {
	    //getMachine(filename).write(filename,contents)
			 	
		// write the file
	}
	
	@Override
	String read(String Filename) throws org.apache.thrift.TException {
		// getMachine(filename)
			
		// return file contents from that machine
	}
	
	
	@Override
	void updateDHT(List<Machine> NodesList) throws org.apache.thrift.TException{
	    if(table.equals(NodesList)){
		    return;    
		}
		// Update your own dht
		table.update(NodesList);
		//Connect to each machine and call UpdateDHT
		for (int i=1; i==NodesList.size() ;i++){
			if(table.contains(i) > -1){
			    connectToNode(NodeList.get(i)).UpdateDHT(NodesList);
		    }
		} 
	}
	
	Node.Client connectToNode(Machine node) throws TException {
	    TTransport nodeTransport = new TSocket(node.ipAddress, node.id);
		nodeTransport.open();
		TProtocol nodeProtocol = new TBinaryProtocol(new TFramedTransport(nodeTransport));
		return new Node.Client(nodeProtocol);
	} 
	
	
	/* Constructor for a Node, a Thrift connection is made to the SuperNode as well */
	public NodeHandler(String superNodeIP, Integer superNodePort, Integer port) throws TException {
		
		this.port = port;
		
		// connect to the supernode as a client
		TTransport superNodeTransport = new TSocket(superNodeIP, superNodePort);
		superNodeTransport.open();
		TProtocol superNodeProtocol = new TBinaryProtocol(new TFramedTransport(superNodeTransport));
		SuperNode.Client superNode = new SuperNode.Client(superNodeProtocol);
		
		System.out.println("Connected to SuperNode.");

		//Create a Machine data type representing ourselves
		Machine self = new Machine();
		self.ipAddress = InetAddress.getLocalHost();			//not sure if this works.
		self.port = this.port;

		// call join on superNode for a list
		List<Machine> listOfNodes = superNode.join(self);
		
		// New Node ID is +1 in the index of Nodes
		this.nodeID = listOfNodes.size();
		
		// populate our own DHT and recursively update others
		updateDHT(listOfNodes);
		
		// call post join after all DHTs are updated.
		if(!superNode.postJoin(self))
			System.err.println("Could not perform postJoin call");
		
		superNodeTransport.close();
	}
	
	//Begin Thrift Server instance for a Node and listen for connections on our port
	private void start() throws TException {
		//Create Thrift server socket
	    TServerTransport serverTransport = new TServerSocket(this.port);
	    TTransportFactory factory = new TFramedTransport.Factory();

	    Node.Processor processor = new Node.Processor<>(this);
 
	    //Set Server Arguments
	    TServer.Args serverArgs = new TServer.Args(serverTransport);
	    serverArgs.processor(processor); //Set handler
	    serverArgs.transportFactory(factory); //Set FramedTransport (for performance)

	    //Run server as single thread
	    TServer server = new TSimpleServer(serverArgs);
	    server.serve();
	}
	
	
	public static void main(String[] args) {
		if(args.length < 3) {
			System.err.println("Usage: java NodeHandler <superNodeIP> <superNodePort> <port>")
			return;
		}
		try {
			System.out.println("Our IP Address is " + InetAddress.getLocalHost());
			String superNodeIP = args[0];
			Integer superNodePort = Integer.parseInt(args[1]);
			//port number used by this node.
			Integer port = Integer.parseInt(args[2]);
			NodeHandler node = new NodeHandler(superNodeIP, superNodePort, port);
			
			//spin up nodeServer
			node.start();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}