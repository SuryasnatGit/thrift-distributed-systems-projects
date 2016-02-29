//import all the thift things
import org.apache.thrift.server.*;
import org.apache.thrift.transport.*;
import org.apache.thrift.TException;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class SuperNodeHandler implements SuperNode.Iface {

    Integer minNodes;
    Random random;
    List<Machine> cluster;
    Machine lastJoiningNode; //populated when a node wants to join.

    @Override
    public List<Machine> Join(Machine node) throws org.apache.thrift.TException {
	if(node != null && lastJoiningNode == null) {
	    lastJoiningNode = node;
	    return cluster;
	}
	return new ArrayList<>(); //NACK
    }

    @Override
    /*Function called by Nodes after they have reported to all nodes they have joined */
    public boolean PostJoin(Machine node) throws org.apache.thrift.TException {
	if(node == null || node.equals(lastJoiningNode))
	    return false;
	//Add node to list of nodes and let other machines join.
	cluster.add(node);
	lastJoiningNode = null;

	System.out.println("Current Nodes in DHT");
	System.out.println(cluster.toString());

	return true;
    }

    @Override
    /* Function called by client to get a node to talk to */
    public Machine getNode() throws org.apache.thrift.TException {
	if(cluster.size() < minNodes) //not enough nodes to start up
	    return null;
	return cluster.get(random.nextInt(cluster.size()));
    }

    public SuperNodeHandler(Integer minNodes) {
	this.minNodes = minNodes;
	this.cluster = new ArrayList<>(minNodes);
	this.random = new Random();
	this.lastJoiningNode = null;
    }

    public static void main(String[] args) {
	if(args.length < 1) {
	    System.err.println("Please enter minimum number of nodes to start DHT ..");
	    System.err.println("Usage: java SuperNodeHandler <port> <min-nodes>");
	    return;
	}
	try {
	    //Create Thrift server socket
	    TServerTransport serverTransport = new TServerSocket(Integer.parseInt(args[0]));
	    TTransportFactory factory = new TFramedTransport.Factory();

	    //Create service request handler
	    SuperNodeHandler superNode = new SuperNodeHandler(Integer.parseInt(args[1]));
	    System.out.println("SuperNode started. Minimum Number of Nodes required: " + superNode.minNodes);

	    SuperNode.Processor processor = new SuperNode.Processor<>(superNode);
 
	    //Set Server Arguments
	    TServer.Args serverArgs = new TServer.Args(serverTransport);
	    serverArgs.processor(processor); //Set handler
	    serverArgs.transportFactory(factory); //Set FramedTransport (for performance)

	    //Run server as single thread
	    TServer server = new TSimpleServer(serverArgs);
	    server.serve();
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
    }

}
