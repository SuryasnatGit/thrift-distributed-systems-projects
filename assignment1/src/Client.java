import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import java.util.Scanner;

public class Client {

    Scanner sc; 

    //Connect to the superNode
    public Client(String host, Integer port) {
	sc = new Scanner(System.in);
    }

    public boolean connectToNode() {
	return true;
    }

    
    public static void main(String[] args) {
	if(args.length < 2) {
	    System.err.println("Usage: java Client <host> <port>");
	    return;
	}
	
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
