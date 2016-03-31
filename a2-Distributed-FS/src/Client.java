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
    Random rand;

    TTransport serverTransport;
    Server.Client server;


    final String defaultDir = "./upload/"; //default upload directory
    final String encoding = System.getProperty("file.encoding");

    //Connect to the superNode
    public Client(Machine serverInfo) throws TException{
	sc = new Scanner(System.in);
	rand = new Random();
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
	System.out.println("\n\n -------- Welcome to the Distributed File System Terminal --------\n\n");
	System.out.println("-------- Usage : <read> <load-test> <ls> <write> <exit> --------\n");
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
	case "read" :
	    fileOperation(input, "read");
	    return true;

	case "load-test":
	    fileOperation(input, "load-test");
	    return true;

	case "write" :
	    fileOperation(input, "write");
	    return true;

	case "ls":
	    fileOperation(input, "ls");
	    return true;

	case "exit":
	    cleanUp();
	    return false;

	default:
	    System.out.println("Usage: [<read> OR <write> <filename> ] | <ls> | <load-test> <no. reads> <no. writes> <no. files> | <exit>");
	    return true;
	}
    }

    private void cleanUp() {
	System.out.println("------- Client: Leaving FileSystem -------");
	serverTransport.close();
    }

    private void fileOperation(String[] input, String op) throws Exception {
	if(input.length < 2 && !op.equals("ls")) {
	    System.out.println("Usage: [<read> OR <write> <filename> | <load-test> <no. files> <no. reads> <no. writes> ]");
	    return;
	}

        switch(op) {
	case "read":
	    System.out.println("Client: Reading " + input[1]);
	    System.out.println("Success: " + readFile(input[1].trim()));
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

	case "load-test":
	    if(input.length == 4) { 
		Integer num_files = Integer.parseInt(input[1]);
		Integer num_reads = Integer.parseInt(input[2]);
		Integer num_writes = Integer.parseInt(input[3]); 
		System.out.println("Load testing with " + num_reads + " reads : " + num_writes + " writes across " + num_files + " files in " + defaultDir);
		loadTest(num_files, num_reads, num_writes);
	    }
	    else 
		System.out.println("Usage: <load-test> <no. reads> <no. writes> <no. files>");
	    break;
	}
    }

    private boolean writeFile(String filename) throws TException {
	//writing to DFS first requires reading
        ByteBuffer contents = Utils.read(defaultDir + filename);
        if(contents == null)
            return false;
        return server.write(filename, contents);
    }

    private boolean readFile(String filename) throws TException {
	ByteBuffer content = server.read(filename);
	//else we can't deserialize properly, so convoluted
	// Thanks: http://www.java2s.com/Code/Java/File-Input-Output/ConvertingtexttoandfromByteBuffers.htm
	String result = java.nio.charset.Charset.forName(encoding).decode(content).toString();
	    
	if(result.equals("NULL")) {
	    System.out.println("File does not exist in DFS");
	    return false;
	}
	else {
	    System.out.println("Content :\n    " + result); //just the filename, no paths allowed
	    return true;
	}
    }

    //Thank you http://stackoverflow.com/a/1846349
    private ArrayList<String> listAllFiles(final File folder) {
	ArrayList<String> filenames = new ArrayList<>();
	for (final File fileEntry : folder.listFiles())
	    filenames.add(fileEntry.getName());
	return filenames;
    }

    private void loadTest(int num_files, int num_reads, int num_writes) throws TException {
	ArrayList<String> files = listAllFiles(new File(defaultDir)); 
	while(num_reads + num_writes > 0) {
	    Boolean isRead = null;
	    isRead = rand.nextBoolean();
	    if(isRead && num_reads > 0) num_reads--;
	    else {
		num_writes--;
		isRead = false; //since it might be isRead = true but num_reads <0
	    }
	    
	    int i = rand.nextInt(num_files % files.size()); //avoid crashing when num_files > files.size()
	    if(!isRead && !writeFile(files.get(i))) {
		System.out.println("Something went wrong with writing file: " + files.get(i));
		break; //abort the entire operation since all files should be present
	    }
	    if(isRead && readFile(files.get(i))) {
		System.out.println("Could not read file from DFS: " + files.get(i));
		//don't break since this is a common occurence, requesting a file that doesn't exist in the DFS
	    }
	}
	System.out.println("-------- LOAD TEST COMPLETE -------- ");
    }
    
}
