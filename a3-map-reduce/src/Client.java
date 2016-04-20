import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.*;

import java.util.Scanner;
import java.util.ArrayList;
import java.util.Random;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.io.File;

public class Client {

    Scanner sc;

    TTransport serverTransport;
    Server.Client server;

    final static String defaultDir = "./data/"; //default data directory
    final static String USAGE_STRING = "Usage: <ls> | <sort> <filename> <chunks_size> <num_files_per_merge> | <exit>";
  
    //Connect to the superNode
    public Client(Machine serverInfo) throws TException{
	sc = new Scanner(System.in);
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
	    System.err.println("Usage: java Client <server> <port>");
	    return;
	}
	try {
	    Machine serverInfo = new Machine();
	    serverInfo.ipAddress = args[0];
	    serverInfo.port = Integer.parseInt(args[1]);

	    Client client = new Client(serverInfo);
	
	    while(!client.connectToServer()) {
		    System.err.println("Client: Failed to connect to a server on cluster, retrying in 1 second ...");
		    Thread.sleep(1000);
	    }
        
	    System.out.println("Contacted server at " + serverInfo.ipAddress + ":" + serverInfo.port);
	    System.out.println("\n\n -------- Welcome to the Terminal for Map Reduce --------\n\n");
	    System.out.println("------" + USAGE_STRING + " ------\n");
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

	case "sort": 
	    fileOperation(input, "sort");
	    return true;

	case "ls":
	    fileOperation(input, "ls");
	    return true;
    
	case "exit":
	    cleanUp();
	    return false;

	default:
	    System.out.println(USAGE_STRING);
	    return true;
	}
    }

    private void cleanUp() {
	System.out.println("------- Client: Leaving Cluster -------");
	serverTransport.close();
    }

    private void fileOperation(String[] input, String op) throws Exception {
	if(input.length < 4 && !op.equals("ls")) {
	    System.out.println(USAGE_STRING);
	    if(op.equals("sort"))
		System.out.println("Input valid chunk size and num files per merge.");
	    return;
	}

        switch(op) {

	case "sort":
	    Integer chunksize;
	    Integer num_merge;
	    try {
		chunksize = Integer.parseInt(input[2]);
		if(chunksize < 1)
		    throw new NumberFormatException();
		num_merge = Integer.parseInt(input[3]);
		if(num_merge < 2)
		    throw new NumberFormatException();
	    } 
	    catch (NumberFormatException e) {
		System.out.println(USAGE_STRING);
		System.out.println("Number of chunks must be >1 and Number of files per merge must be >2");
		return;
	    }
	    System.out.println("Client: Submitting Sort Job on " + defaultDir + input[1] + " with chunksize: " + chunksize);
	    System.out.println(" Success: " + submitJob(defaultDir + input[1].trim(), chunksize, num_merge));
	    break;

	case "ls":
	    System.out.println("Files in " + defaultDir);
	    for(String filename : listAllFiles(new File(defaultDir)))
		System.out.println(filename);
	    break;
	}
    }


    /* From the assignment description
       The number of intermediate files for merge tasks is also passed to the server as a
	parameter. If it is set to 8 and there are 64 sort tasks (intermediated files), there will be 9
	merge tasks (8 merge tasks for the first round and 1 merge task for the second round).
    */
    private boolean submitJob(String filename, Integer chunks, Integer num_merge) throws TException {
	//System.out.println("Reading File: " + filename);

	//check for existence of file
	File file = new File(filename);
	if(!file.exists() || file.isDirectory()) {
	    System.out.println("Not a file or is a directory.");
	    return false;
	}

	String result = server.compute(filename, chunks, numMerge);

	if(result.equals("NULL")) {
	    System.out.println("Job failed");
	    return false;
	}
	else {
	    System.out.println("Output file located at :\n    " + result); //just the filename, no paths allowed
	    return true;
	}
    }

    //Thank you http://stackoverflow.com/a/1846349
    private ArrayList<String> listAllFiles(final File folder) {
	ArrayList<String> filenames = new ArrayList<>();
	if(!folder.exists()) {
	    System.out.println("Folder does not exist.");
	    return filenames;
	}
	for (final File fileEntry : folder.listFiles())
	    filenames.add(fileEntry.getName());
	return filenames;
    }    
}
