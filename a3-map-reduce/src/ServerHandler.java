//Thrift imports
import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.*;
import org.apache.thrift.server.*;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.ArrayList;
import java.util.LinkedList;
import java.net.InetAddress;
import java.util.Iterator;
import java.io.File;
import java.io.FileInputStream;


public class ServerHandler implements Server.Iface {
    
    List<Machine> computeNodes;
    Machine self;
    private Integer i_complete; // synchronized counter for completed tasks.
    private Integer i_unique;   // synchronized counter for unique intermediate files

    private Queue<Task> tasks;  // task queue.

    
    public ServerHandler(Integer port) throws Exception {
        computeNodes = new ArrayList<Machine>();
        this.tasks = new LinkedList<>();
	this.i_complete = 0;
	this.i_unique = 0;

        //Create a Machine data type representing ourselves
        self = new Machine();
        self.ipAddress = InetAddress.getLocalHost().getHostName().toString();		
        self.port = port;	
    }
    
    public static void main(String[] args) {
        if(args.length < 1) {
            System.err.println("Usage: java ServerHandler <port>");
            return;
        }
        try {
            System.out.println("IP Address is " + InetAddress.getLocalHost().toString());
	    //port number used by this node.
            Integer port = Integer.parseInt(args[0]);
	    ServerHandler server = new ServerHandler(port);
            
	    //spin up server
            server.start();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean enroll(Machine machine) throws TException {
        computeNodes.add(machine);
        System.out.println("\n\nList of Compute Nodes : \n\n" + computeNodes.toString());
        return true;
    }

    @Override
    public String compute(String filename, int chunks) throws TException {
	System.out.println("SERVER: Starting sort job on " + filename + " with chunksize " + chunks);
	try {
	    //process the file by generating chunk metadata
	    this.chunkify(filename, chunks);
	    
	    //start contacting all nodes and queue it all onto compute machines

	    // blocking wait for all tasks for it all to complete.
	}
	catch(Exception e)
	{
	    e.printStackTrace();
	}

	System.out.println("COMPLETE");
	return "NULL";
    }


    @Override
    // RPC Called by the compute nodes when they have done their task
    public boolean announce() throws TException {
	synchronized(i_complete) {
	    i_complete++;
	}
	return true;
    }

    /* ---- PRIVATE HELPER FUNCTIONS ---- */
    private void chunkify(String filename, Integer chunksize) throws Exception {
	// get the file size and do math on the chunks
	// read the file
	File dataFile = new File(filename);
	long filesize = dataFile.length();

	FileInputStream fis = new FileInputStream(dataFile);
	assert fis.available() == filesize; 

	// divide up integer stream into chunks, upperbound
	// note totalchunks == total number of tasks
	long totalChunks = filesize / chunksize + (filesize % chunksize);
	
	System.out.println("CHUNKK " + chunksize);
	long skipped; 
	long start = 0;
	long end = 0;

	while( (skipped = fis.skip(chunksize)) != -1 ) {
	    System.out.println("Skipped " + skipped);
	    end = skipped;
	    
	    //calibrate to ensure we don't chop off integers
	    int b = fis.read();
	    while(b != 32) //code point for a space
	    {
		System.out.print("!!-" + b);
		b = fis.read();
		end++;
	    }
	    
	    //insert chunk into tasks
	    SortTask task = new SortTask(start, end, filename, i_unique.toString());
	    tasks.add(task);

	    //update start point for next chunk
	    start = end;
	}
	System.out.println("what we got ...");
	for(Task t : tasks)
	    System.out.println(t);
	
    }

    // reset state for next job
    private void cleanup() {
	i_complete = 0;
	i_unique = 0;
	tasks.clear();
    }


    //Begin Thrift Server instance for a Node and listen for connections on our port
    private void start() throws TException {
        
        //Create Thrift server socket
        TServerTransport serverTransport = new TServerSocket(self.port);
        TTransportFactory factory = new TFramedTransport.Factory();
        Server.Processor processor = new Server.Processor<>(this);

        //Set Server Arguments
        TThreadPoolServer.Args serverArgs = new TThreadPoolServer.Args(serverTransport);
        serverArgs.processor(processor); //Set handler
        serverArgs.transportFactory(factory); //Set FramedTransport (for performance)

        //Run server with multiple threads
        TServer server = new TThreadPoolServer(serverArgs);
        
	System.out.println("Server is listening ... ");
        server.serve();
    }
}
