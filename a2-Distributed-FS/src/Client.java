import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.*;

import java.util.Scanner;
import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.io.File;

public class Client {

    Scanner sc;
    Stats stats;
    TTransport serverTransport;
    Server.Client server;

    String defaultDir = "./upload/"; //default upload directory
    final String encoding = System.getProperty("file.encoding");

    //Connect to the superNode
    public Client(Machine serverInfo) throws TException{
	sc = new Scanner(System.in);
	stats = new Stats();
	serverTransport = new TSocket(serverInfo.ipAddress, serverInfo.port);
    }

    private boolean connectToServer() {
	try {
	    serverTransport.open();
	    TProtocol serverProtocol = new TBinaryProtocol(new TFramedTransport(serverTransport));
	    server = new Server.Client(serverProtocol);
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
	
	    while(!client.connectToServer()) {
		    System.err.println("Client: Failed to connect to a server on cluster, retrying ...");
		    Thread.sleep(1000);
	    }
        
        System.out.println("Contacted server at " + serverInfo.ipAddress + ":" + serverInfo.port);

	    //we are connected.
	    System.out.println("\n\n -------- Welcome to the Distributed File System Terminal, use: <read> <write-all> <ls> <write> <exit> --------\n");
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
	case "read": 
        stats.start();
	    fileOperation(input, "read");
        stats.logRead();
	    return true;

	case "write-all":
	    fileOperation(input, "write-all");
	    return true;

	case "write" :
        stats.start();
	    fileOperation(input, "write");
        stats.logWrite();
	    return true;

	case "ls":
	    fileOperation(input, "ls");
	    return true;
    
    case "stats":
	    stats.print();
	    return true;

	case "exit":
	    cleanUp();
	    return false;

	default:
	    System.out.println("Usage: [<read> OR <write> <filename> ] | <ls> | <write-all> | <stats> | <exit>");
	    return true;
	}
    }

    private void cleanUp() {
	System.out.println("------- Client: Leaving FileSystem -------");
	serverTransport.close();
    }

    private void fileOperation(String[] input, String op) throws Exception {
	if(input.length != 2 && !op.equals("ls") && !op.equals("write-all")) {
	    System.out.println("Usage: [<read> OR <write> <filename> ]");
	    return;
	}

        switch(op) {
	case "read":
	    System.out.println("Client: Reading " + input[1]);
	    ByteBuffer content = server.read(input[1].trim());
	    //else we can't deserialize properly, so convoluted
	    // Thanks: http://www.java2s.com/Code/Java/File-Input-Output/ConvertingtexttoandfromByteBuffers.htm
	    String result = java.nio.charset.Charset.forName(encoding).decode(content).toString();
	    
	    if(result.equals("NULL"))
		System.out.println("File does not exist in DFS");
	    else
		System.out.println("Content :\n    " + result); //just the filename, no paths allowed
	    break;

	case "write":
	    System.out.println("Client: Writing " + input[1]);
	    System.out.println("Success: " + writeFile(input[1].trim()));
	    break;

	case "ls":
	    System.out.println("Files in " + defaultDir);
	    for(String filename : listAllFiles(new File(defaultDir)))
		System.out.println(filename);
	    break;

	case "write-all":
	    System.out.println("Writing all files in " + defaultDir);
	    for(String filename : listAllFiles(new File(defaultDir)))
		if(!writeFile(filename)) {
		    System.out.println("Something went wrong with writing all files`");
		    break;
		}
	    break;
	}
    }

    private boolean writeFile(String filename) throws TException {
	//writing to DFS first requires reading
        ByteBuffer contents = Utils.read(defaultDir + filename);
        if(contents == null)
            return false;
        System.out.println("Performing RPC Call to deez server");
        return server.write(filename, contents);
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
