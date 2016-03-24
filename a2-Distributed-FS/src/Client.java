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

    TTransport serverTransport;
    Server.Client server;

    String defaultDir = "./upload/"; //default upload directory

    //Connect to the superNode
    public Client(Machine serverInfo) throws TException{
	sc = new Scanner(System.in);
	
	serverTransport = new TSocket(serverInfo.ipAddress, serverInfo.port);
    }

    private boolean connectToServer() {
	try {
	    serverTransport.open();
	    TProtocol serverProtocol = new TBinaryProtocol(new TFramedTransport(serverTransport));
	    server = new SuperNode.Client(serverProtocol);
	    return true;
	}
	catch(TException e) {
	    e.printStackTrace();
	    return false;
	}
    }
    
    public static void main(String[] args) {
	if(args.length < 2) {
	    System.err.println("Usage: java Client <host> <port>");
	    return;
	}
	try {
	    Machine serverInfo = new Machine();
	    serverInfo.ipAddress = args[0];
	    serverInfo.port = Integer.parseInt(args[1]);

	    Client client = new Client(serverInfo);
	    System.out.println("Contacted server at " + serverInfo.ipAddress + ":" + serverInfo.port);

	    while(!client.connectToServer()) {
		    System.err.println("Client: Failed to connect to a server on cluster, retrying ...");
		    Thread.sleep(1000);
	    }
	    //we are connected.
	    System.out.println("\n\n -------- Welcome to the Distributed File System Terminal, use: <get> <ls> <put> <put-all> <exit> --------\n");
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
	    System.out.println("Content :\n    " + node.read(input[1].trim())); //just the filename, no paths allowed
	    break;
	case "put":
	    System.out.println("Client: Writing " + input[1] + " to DHT");
	    System.out.println("Success: " + writeFile(defaultDir + input[1].trim()));
	    break;
	case "put-all":
	    System.out.println("Loading all files in current directory to DHT..");
	    for(String filename : listAllFiles(new File(defaultDir)))
		if(!writeFile(defaultDir + filename))
		    break;
	    break;
	case "ls":
	    System.out.println("Files in " + defaultDir);
	    for(String filename : listAllFiles(new File(defaultDir)))
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
