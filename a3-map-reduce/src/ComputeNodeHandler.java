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
import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.net.InetAddress;
import java.util.Iterator;

public class ComputeNodeHandler implements ComputeNode.Iface{

    Machine self;
    Machine server;
    String directory; //name of folder to be written/read to by server
    Queue<Task> taskQueue;
    boolean isDead;
    
    
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
        
        isDead = false; 
        
        // call enroll on superNode to enroll.
        boolean success = serverClient.enroll(self);

        if(success)
            System.out.println("Server has successfully reported to server");
        else
            System.out.println("Could not report to server... damn.");

        serverTransport.close();
    }
    
    @Override
    public boolean sort(String filename, long startChunk, long endChunk, String output) throws TException {
	// Serialize 
	SortTask task = new SortTask(startChunk,endChunk,filename,output);
	// Add to the queue
	synchronized(taskQueue){
		taskQueue.add(task);
	}
	return false;
    }

    @Override
    public boolean merge(String f1, String f2,String output) throws TException {
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
    
    private List<Integer> readFileToIntList(SortTask t) throws Exception {
	int len = (int) (t.endOffset - t.startOffset + 1); //should never overflow since chunksize isn't going to be that big, I think.
	List<Integer> output = new ArrayList<>((int) len);

	BufferedReader rdr = new BufferedReader(new FileReader(t.filename));
	rdr.skip(t.startOffset);
	char[] buf = new char[(int) len];
	rdr.read(buf, 0, (int) len);

	String[] split = (new String(buf)).split(" ");
	for(String s : split)	    
	    output.add(Integer.parseInt(s.trim())); //trim because of EOF. Hidden, but length is +1.
	
	System.out.println("RESULT : " + output.toString());
	return output;
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
