import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.*;

import java.util.Scanner;
import java.util.ArrayList;
import java.io.File;
import java.nio.file.Paths;
import java.nio.file.Files;

public class Client {

    Scanner sc; 
    SuperNode.Client superNode;
    TTransport superNodeTransport;
    Machine machine; 

    TTransport nodeTransport;
    Node.Client node;

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
	machine = superNode.getNode();
	if(machine.ipAddress.equals("NULL")) {
	    return false;
	}
	//we have a node, kill superNode connection
	superNodeTransport.close();

	nodeTransport = new TSocket(machine.ipAddress, machine.port);
	nodeTransport.open();
	TProtocol nodeProtocol = new TBinaryProtocol(new TFramedTransport(nodeTransport));
	node = new Node.Client(nodeProtocol);

	System.out.println("Client: Successfully connected to Node : " + machine.id);
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

	    while(!client.connectToNode()) {
		System.err.println("Client: Failed to connect to a node on cluster, retrying ...");
		Thread.sleep(1000);
	    }
	    //we are connected.
	    System.out.println("\n\n -------- Welcome to the DHT Cluster Terminal, use: <get> <put> <putAll> <exit> --------\n");
	    while(true) {
		if(client.getAndProcessUserInput() == false)
		    break;
	    }
	}
	catch(Exception e) {
	    e.printStackTrace();
	}
    }

    private boolean getAndProcessUserInput() throws Exception {
	String[] input = sc.nextLine().split(" ");
	switch(input[0]) {
	case "get" :
	    fileOperation(input, "get");
	    return true;

	case "put" :
	    fileOperation(input, "put");
	    return true;

	case "put-all":
	    fileOperation(input, "put-all");
	    return true;

	case "ls":
	    fileOperation(input, "ls");
	    return true;

	case "exit":
	    cleanUp();
	    return false;

	default:
	    System.out.println("Usage: [<get> OR <put> <filename> ] | <ls> | <put-all> | <exit>");
	    return true;
	}
    }

    private void cleanUp() {
	System.out.println("------- Client: Leaving DHT -------");
	nodeTransport.close();
    }

    private void fileOperation(String[] input, String op) throws Exception {
	if(input.length != 2 && !op.equals("put-all") && !op.equals("ls")) {
	    System.out.println("Usage: [<get> OR <put> <filename> ]");
	    return;
	}

        switch(op) {
	case "get":
	    System.out.println("Client: Reading " + input[1] + " from DHT");
	    System.out.println(node.read(input[1].trim())); //just the filename, no paths allowed
	    break;
	case "put":
	    System.out.println("Client: Writing " + input[1] + " to DHT");
	    System.out.println("Success: " + writeFile("./upload/" + input[1].trim()));
	    break;
	case "put-all":
	    System.out.println("Loading all files in current directory to DHT..");
	    for(String filename : listAllFiles(new File("./upload/")))
		if(!writeFile(filename))
		    break;
	    break;
	case "ls":
	    System.out.println("Files in ./upload/");
	    for(String filename : listAllFiles(new File("./upload/")))
		System.out.println(filename);
	    break;
	}
    }

    private boolean writeFile(String filename) throws Exception {
	//check if file exists
	byte[] contents = null; //lol initializing, amirite
	File file = new File(filename);

	if(!file.exists() || file.isDirectory()) {
	    System.out.println("Client: Not a file or file doesn't exist.");
	    return false;
	}
	try {
	    //load the contents into a byte array
	    contents = Files.readAllBytes(Paths.get(filename));
	}
	catch(Exception e) {
	    System.out.println("Client: Failed to read file contents");
	}
	finally {
	    if(contents != null) {
		filename = Paths.get(filename).getFileName().toString(); //strip any relative paths, just get filenames
		//send it over the wire
		System.out.println("New file name " + filename);
		return node.write(filename, new String(contents, "utf-8"));
	    }
	    return false;
	}
    }

    //Thank you http://stackoverflow.com/a/1846349
    private ArrayList<String> listAllFiles(final File folder) {
	ArrayList<String> filenames = new ArrayList<>();
	for (final File fileEntry : folder.listFiles()) {
	    /* Scans subdirectories recursively
	      if (fileEntry.isDirectory()) {
		listAllFiles(fileEntry);
	    } else {
		System.out.println(fileEntry.getName());
	    } */
	    filenames.add(fileEntry.getName());
	}
	return filenames;
    }
    
}
