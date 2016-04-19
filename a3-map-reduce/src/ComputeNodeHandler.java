//Thrift imports
import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.*;
import org.apache.thrift.server.*;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.LinkedList;
import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ComputeNodeHandler implements ComputeNode.Iface{

    Machine self;
    Machine server;
    String directory; //name of folder to be written/read to by server
    ConcurrentLinkedQueue<Task> taskQueue;
    boolean isDead;
    double chanceToFail = 0.0;
    
    
    /* Constructor for a Server, a Thrift connection is made to the server as well */
    public ComputeNodeHandler(String serverIP, Integer serverPort, Integer port) throws Exception {    
        // connect to the server as a client
        TTransport serverTransport = new TSocket(serverIP, serverPort);
        serverTransport.open();
        TProtocol serverProtocol = new TBinaryProtocol(new TFramedTransport(serverTransport));
        Server.Client serverClient = new Server.Client(serverProtocol);    
        
        this.server = new Machine();
        this.server.ipAddress = serverIP;
        this.server.port = serverPort;
        
        //Create a Machine data type representing ourselves
        self = new Machine();
        self.ipAddress = InetAddress.getLocalHost().getHostName().toString();		
        self.port = port;
        
        Comm.setServer(this.server);
        Comm.setSelf(self);

        isDead = false; 
        
        // call enroll on superNode to enroll.
        boolean success = serverClient.enroll(self);
		taskQueue = new ConcurrentLinkedQueue<>();
        if(success)
            System.out.println("Server has successfully reported to server");
        else
            System.out.println("Could not report to server... damn.");

        serverTransport.close();
    }
    
    @Override
    public boolean sort(String filename, long startChunk, long endChunk, String output) throws TException {
	
	if(isDead)
		throw new TException();
	
	// Serialize 
	SortTask task = new SortTask(startChunk,endChunk,filename,output);
	boolean success = false;
	
	// Add to the queue
	synchronized(taskQueue){
		taskQueue.add(task);
		success = true;
	}
	return success;
    }

    @Override
    public boolean merge(String f1, String f2,String output) throws TException {		
	if(isDead){
	    throw new TException();
	}
	// Serialize
	MergeTask task = new MergeTask(f1,f2,output);
	// Add to the Queue
	synchronized(taskQueue){
	    taskQueue.add(task);
	}
	return false;
    }

    
    @Override
    public boolean heartbeat() throws TException {
		if(isDead)
			throw new TException();
		return true;
    }
    
    @Override
    public String getStats() throws TException {
        return "";
    }
    
    @Override
    public boolean cancel(String output) throws TException {
        return false;
    }
    	
    //Begin Thrift Server instance for a Node and listen for connections on our port
    private void start() throws TException {
		
	QueueWatcher watcher = new QueueWatcher(this,taskQueue);
	watcher.start();
        //Create Thrift server socket
        TServerTransport serverTransport = new TServerSocket(self.port);
        TTransportFactory factory = new TFramedTransport.Factory();

        ComputeNode.Processor processor = new ComputeNode.Processor<>(this);

        //Set Server Arguments
        TThreadPoolServer.Args serverArgs = new TThreadPoolServer.Args(serverTransport);
        serverArgs.processor(processor); //Set handler
        serverArgs.transportFactory(factory); //Set FramedTransport (for performance)

        //Run server with multiple threads
        TServer server = new TThreadPoolServer(serverArgs);
        
        server.serve();
    }
    
    public static void main(String[] args) {
        if(args.length < 3) {
            System.err.println("Usage: java ComputeNodeHandler <serverIP> <serverPort> <port>");
            return;
        }
        try {
            System.out.println("IP Address is " + InetAddress.getLocalHost().toString());
            String serverIP = args[0];
            Integer serverPort = Integer.parseInt(args[1]);
	    
	        //port number used by this node.
            Integer port = Integer.parseInt(args[2]);
            
            ComputeNodeHandler server = new ComputeNodeHandler(serverIP, serverPort,port);
             
            //spin up server
            server.start();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
