//Thrift imports
import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.*;
import org.apache.thrift.server.*;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.net.InetAddress;

public class NodeHandler implements Node.Iface{
			
    DHT table;
    Machine self; //thrft struct for information about ourselves  
    Integer nodeID;
    Integer port;
	HashMap<String,String> fs;
    
    @Override
    public boolean write(String filename, String contents) throws org.apache.thrift.TException {
        Machine m = findMachine(filename,new ArrayList<Integer>());
        if(m.ipAddress.equals("NULL")) {
            System.out.println("   THIS SHOULD NOT HAPPEN BUT IT HAPPENED, TAKE A LOOK     ");
            return false;
        }
	else if(m.equals(self)) {
            fs.put(filename,contents);
            System.out.println("file name wrote to" + self.id);
            return true;
        }
	else {
            // RPC the write call
            TTransport nodeTransport = new TSocket(m.ipAddress, m.port);
            nodeTransport.open();
            TProtocol nodeProtocol = new TBinaryProtocol(new TFramedTransport(nodeTransport));
            Node.Client node = new Node.Client(nodeProtocol);
            System.out.println("Machine("+nodeID+") Calling write on Machine(" + m.id+")");
            boolean success = node.write(filename,contents);
            nodeTransport.close();
            return success;
        }
    }
    
    @Override
    public Machine findMachine(String filename, List<Integer> chain)throws org.apache.thrift.TException {
	try {
        if(chain.contains(nodeID)){
            //back at step one, return null machine
            
            //TODO print the chain
            
            return new Machine();
        }
        chain.add(nodeID);
        
        // Hash the file name
        int hash = filename.hashCode();
        // Getting which machine the file is ours.
        int target = Math.abs(hash) % table.numMachines;
        
        //we have the file
        if (nodeID == target) 
            //TODO print the chain
            return self;
            
        // Go look in the DHT
        Machine m = table.searchDHT(filename,target);
        
        // Traverse the DHT by RPC
        TTransport nodeTransport = new TSocket(m.ipAddress, m.port);
        nodeTransport.open();
        TProtocol nodeProtocol = new TBinaryProtocol(new TFramedTransport(nodeTransport));
        Node.Client node = new Node.Client(nodeProtocol);
        System.out.println("Machine("+nodeID+") Calling findMachine on Machine(" + m.id+")");
        Machine sucessor = node.findMachine(filename,chain);
        nodeTransport.close();
        //TODO print the chain
        return sucessor;
	} catch (Exception e) {
	    e.printStackTrace();
	}
	return new Machine();
    }
    
    @Override
    public String read(String filename) throws TException {
        Machine m = findMachine(filename,new ArrayList<Integer>());
        if(m.ipAddress.equals("NULL")) {
            System.out.println("   THIS SHOULD NOT HAPPEN BUT IT HAPPENED, TAKE A LOOK     ");
            return "";
        }
	    else if(m.equals(self)) {
			System.out.println("we gots it");
            return fs.get(filename);
        }
	    else {
			System.out.println("M :" + m.toString());
			System.out.println("SELF :" + self.toString());
				
            // RPC the read call
            System.out.println("rpc the read");
            TTransport nodeTransport = new TSocket(m.ipAddress, m.port);
            nodeTransport.open();
            TProtocol nodeProtocol = new TBinaryProtocol(new TFramedTransport(nodeTransport));
            Node.Client node = new Node.Client(nodeProtocol);
            System.out.println("Machine("+nodeID+") Calling read on Machine(" + m.id+")");
            String contents = node.read(filename);
            nodeTransport.close();
            return contents;
        }
    }
    
    
    @Override
    public void updateDHT(List<Machine> NodesList,List<Integer> chain) throws TException{
        // Update your own dht
        table.update(NodesList);
        table.print();
        chain.add(nodeID);
        //Connect to each machine and call UpdateDHT
        for (int i=0; i<NodesList.size() ;i++){
            if(table.contains(i) > -1 && !chain.contains(i)){
                Machine m = NodesList.get(i);
                TTransport nodeTransport = new TSocket(m.ipAddress, m.port);
                nodeTransport.open();
                TProtocol nodeProtocol = new TBinaryProtocol(new TFramedTransport(nodeTransport));
                Node.Client node = new Node.Client(nodeProtocol);
                System.out.println("Machine("+nodeID+") Calling updateDHT on Machine(" + m.id+")");
                node.updateDHT(NodesList,chain);
                nodeTransport.close();
            }
        } 
    }

    @Override
    public int ping() throws TException {
	System.out.println("Ping called on " + nodeID);
	return nodeID;
    }
    
    private Node.Client connectToNode(Machine node) throws TException {
	TTransport nodeTransport = new TSocket(node.ipAddress, node.port);
	nodeTransport.open();
	TProtocol nodeProtocol = new TBinaryProtocol(new TFramedTransport(nodeTransport));
	return new Node.Client(nodeProtocol);
    } 
    
    
    /* Constructor for a Node, a Thrift connection is made to the SuperNode as well */
    public NodeHandler(String superNodeIP, Integer superNodePort, Integer port) throws Exception {
	
	this.port = port;
	
	// connect to the supernode as a client
	TTransport superNodeTransport = new TSocket(superNodeIP, superNodePort);
	superNodeTransport.open();
	TProtocol superNodeProtocol = new TBinaryProtocol(new TFramedTransport(superNodeTransport));
	SuperNode.Client superNode = new SuperNode.Client(superNodeProtocol);
	
	System.out.println("Machine has Connected to the SuperNode.");
	
	//Create a Machine data type representing ourselves
	self = new Machine();
	self.ipAddress = InetAddress.getLocalHost().getHostName().toString();			//not sure if this works.
	self.port = port; //yay safe type casting
	
	// call join on superNode for a list
	List<Machine> listOfNodes = superNode.join(self);
	
	//keep trying until we can join (RPC calls)
	while(!listOfNodes.isEmpty() && listOfNodes.get(0).ipAddress.equals("NULL")){
	    System.err.println(" Could not join, retrying ..");
	    Thread.sleep(1000);
	    listOfNodes = superNode.join(self);
	}


	// New Node ID is +1 in the index of Nodes
	this.nodeID = listOfNodes.size();
	this.table = new DHT(this.nodeID);
    this.fs = new HashMap<String,String>();

	//set selfID
	self.id = this.nodeID;
	
	// add ourselves to the listOfNodes, updateDHT then call postJoin to officially add
	listOfNodes.add(self);

	// populate our own DHT and recursively update others
	updateDHT(listOfNodes,new ArrayList<Integer>());

	// call post join after all DHTs are updated.
	if(!superNode.postJoin(self))
	    System.err.println("Machine("+nodeID+") Could not perform postJoin call");
	
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
	    System.err.println("Usage: java NodeHandler <superNodeIP> <superNodePort> <port>");
	    return;
	}
	try {
	    System.out.println("Our IP Address is " + InetAddress.getLocalHost().toString());
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
