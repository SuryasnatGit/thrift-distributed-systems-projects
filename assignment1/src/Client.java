import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.*;

import java.util.Scanner;

public class Client {

    Scanner sc; 
    SuperNode.Client superNode;
    TTransport superNodeTransport;

    //Connect to the superNode
    public Client(String host, Integer port) throws TException{
	sc = new Scanner(System.in);
	
	superNodeTransport = new TSocket(host, port);
	superNodeTransport.open();
	TProtocol superNodeProtocol = new TBinaryProtocol(new TFramedTransport(superNodeTransport));
	superNode = new SuperNode.Client(superNodeProtocol);

    }

    public boolean connectToNode() throws TException {
	//perform call to superNode for a node.
	//get a node, and kill the connection to the supernode
	Machine node = superNode.getNode();
	if(node.ipAddress.equals("NULL"))
	   System.out.println("NULL NODE!!!!!");
	return true;
    }

    
    public static void main(String[] args) {
	if(args.length < 2) {
	    System.err.println("Usage: java Client <host> <port>");
	    return;
	}
	try {
	    String host = args[0];
	    Integer port = Integer.parseInt(args[1]);

	    Client client = new Client(host, port);
	    System.out.println("Contacted SuperNode at " + host + ":" + port);

	    if(!client.connectToNode()) {
		System.err.println("Failed to connect to a node on cluster, shutting down.:");
		return;
	    }
	    System.out.println("Connected to Node at : ");
	    while(true) {
		if(client.getAndProcessUserInput() == false)
		    break;
	    }
	}
	catch(Exception e) {
	    e.printStackTrace();
	}
    }

    private boolean getAndProcessUserInput(){
	String[] input = sc.nextLine().split(" ");
	switch(input[0]) {
	case "get" :
	    return true;

	case "put" :
	    return true;

	case "exit":
	    return false;

	default:
	    System.out.println("Usage: <get>/<put> <filename> OR <exit>");
	    return true;
	}
    }
}
