//Thrift imports
import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.*;
import org.apache.thrift.server.*;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.net.InetAddress;

public class ServerHandler implements Server.Iface{
    HashMap<String,Integer> fs;
    Machine self;

    @Override
    public boolean write(String filename, ByteBuffer contents) throws org.apache.thrift.TException {
        return false;
    }
    
    
    @Override
    public String read(String filename) throws TException {
        return "";
    }
    
    /* only used by the Coordinator, stub */
    @Override
    public boolean enroll(Machine machine) throws TException {
	System.out.println("This should not happen.");
        return false;
    }
    
    /* Constructor for a Server, a Thrift connection is made to the coordinator as well */
    public ServerHandler(String coordinatorIP, Integer coordinatorPort, Integer port) throws Exception {    
        // connect to the coordinator as a client
        TTransport coordinatorTransport = new TSocket(coordinatorIP, coordinatorPort);
        coordinatorTransport.open();
        TProtocol coordinatorProtocol = new TBinaryProtocol(new TFramedTransport(coordinatorTransport));
        Server.Client coordinator = new Server.Client(coordinatorProtocol);    
                
        //Create a Machine data type representing ourselves
        self = new Machine();
        self.ipAddress = InetAddress.getLocalHost().getHostName().toString();		
        self.port = port; //yay safe type casting
        
        // call enroll on superNode to enroll.
        boolean success = coordinator.enroll(self);

	System.out.println("Out of enroll");
	if(success)
	    	System.out.println("Node has reported to Coordinator");

	coordinatorTransport.close();
    }
    	
    //Begin Thrift Server instance for a Node and listen for connections on our port
    private void start() throws TException {
        //Create Thrift server socket
        TServerTransport serverTransport = new TServerSocket(self.port);
        TTransportFactory factory = new TFramedTransport.Factory();

        Server.Processor processor = new Server.Processor<>(this);

        //Set Server Arguments
        TServer.Args serverArgs = new TServer.Args(serverTransport);
        serverArgs.processor(processor); //Set handler
        serverArgs.transportFactory(factory); //Set FramedTransport (for performance)

        //Run server with multiple threads
        TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).processor(processor));
        
        server.serve();
    }
    
    
      public static void main(String[] args) {
        if(args.length < 3) {
            System.err.println("Usage: java ServerHandler <coordinatorIP> <coordinatorPort> <port>");
            return;
        }
        try {
            System.out.println("Our IP Address is " + InetAddress.getLocalHost().toString());
            String coordinatorIP = args[0];
            Integer coordinatorPort = Integer.parseInt(args[1]);
           
	    //port number used by this node.
            Integer port = Integer.parseInt(args[2]);
            
            ServerHandler server = new ServerHandler(coordinatorIP, coordinatorPort,port);
            
            //spin up server
            server.start();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
