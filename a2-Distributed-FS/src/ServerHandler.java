//Thrift imports
import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.*;
import org.apache.thrift.server.*;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.net.InetAddress;

public class ServerHandler implements Server.Iface{
    HashMap<String,Integer> fs;
    ArrayList<Machine> servers;
    Boolean isCoordinator;
    
    @Override
    public boolean write(String filename, String contents) throws org.apache.thrift.TException {
        return false;
    }
    
    
    @Override
    public String read(String filename) throws TException {
        return false;
    }
    
    @Override
    public boolean enroll(Machine machine){
        return false
    }
    
    
    @Override
    public int ping() throws TException {
	   System.out.println("Ping called on " + nodeID);
	   return 1;
    }
    
    
    /* Constructor for a Server, a Thrift connection is made to the coordinator as well */
    public ServerHandler(String coordinatorIP, Integer coordinatorPort, Integer port) throws Exception {
        this.isCoordinator = false;
        this.port = port;
        
        // connect to the coordinator as a client
        TTransport coordinatorTransport = new TSocket(coordinatorIP, coordinatorPort);
        coordinatorTransport.open();
        TProtocol superNodeProtocol = new TBinaryProtocol(new TFramedTransport(coordinatorTransport));
        Server.Client coordinator = new Server.Client(coordinatorProtocol);
            
        
        //TODO multithreaded transports
        System.out.println("Node has reported to Coordinator");
        
        //Create a Machine data type representing ourselves
        self = new Machine();
        self.ipAddress = InetAddress.getLocalHost().getHostName().toString();		
        self.port = port; //yay safe type casting
        
        // call enroll on superNode to enroll.
        boolean success = coordinator.enroll(self);

	    coordinatorTransport.close();
    }
    
    /* Constructor for a Coordinator */
     public ServerHandler(Integer port) throws Exception {
        this.isCoordinator = true;
        this.port = port;
        
        //TODO multithreaded transports
        System.out.println("I am the Coordinator");
        
        //Create a Machine data type representing ourselves
        self = new Machine();
        self.ipAddress = InetAddress.getLocalHost().getHostName().toString();		
        self.port = port; //yay safe type casting
        
        servers.add(self);
    }
	
    //Begin Thrift Server instance for a Node and listen for connections on our port
    private void start() throws TException {
        //Create Thrift server socket
        TServerTransport serverTransport = new TServerSocket(this.port);
        TTransportFactory factory = new TFramedTransport.Factory();

        Node.Processor processor = new Node.Processor<>(this);

        //Set Server Arguments
        TServer.Args serverArgs = new TServer.Args(serverTransport);
        serverArgs.processor(processor); //Set handler
        serverArgs.transportFactory(factory); //Set FramedTransport (for performance)

        //Run server with multiple threads
        TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).processor(processor));
        
        server.serve();
    }
    
    
      public static void main(String[] args) {
        if(args.length < 4) {
            System.err.println("Usage: java NodeHandler <coordinatorIP> <coordinatorPort> <port> <isCoordinator>");
            return;
        }
        try {
            System.out.println("Our IP Address is " + InetAddress.getLocalHost().toString());
            String coordinatorIP = args[0];
            Integer coordinatorPort = Integer.parseInt(args[1]);
            Boolean isCoordinator = Boolean.parseBoolean(args[3]);
            //port number used by this node.
            Integer port = Integer.parseInt(args[2]);
            
            if(isCoordinator)
                ServerHandler server = new ServerHandler(port);
            else
                ServerHandler server = new ServerHandler(coordinatorNodeIP, coordinatorPort,port);
            
            //spin up server
            server.start();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}