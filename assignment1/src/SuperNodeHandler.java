//import all the thift things
import org.apache.thrift.server.*;
import org.apache.thrift.transport.*;
import org.apache.thrift.TException;

import java.util.List;
import java.util.ArrayList;

public class SuperNodeHandler implements SuperNode.Iface {

    Integer minNodes;
    List<Machine> cluster;

    @Override
    public List<Machine> Join(Machine node) throws org.apache.thrift.TException {
	return null;
    }

    @Override
    public boolean PostJoin(Machine node) throws org.apache.thrift.TException {
	return false;
    }

    @Override
    public Machine getNode() throws org.apache.thrift.TException {
	return null;
    }

    public SuperNodeHandler(Integer minNodes) {
	this.minNodes = minNodes;
	this.cluster = new ArrayList<>(minNodes);
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
