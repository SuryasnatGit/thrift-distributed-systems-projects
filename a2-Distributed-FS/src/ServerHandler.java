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
import java.util.Iterator;

public class ServerHandler implements Server.Iface{
    HashMap<String,Integer> fs;
    Machine self;
    String directory; //name of folder to be written/read to by server
    Machine coordinator;
    
    @Override
    public boolean write(String filename, ByteBuffer contents) throws org.apache.thrift.TException {
	System.out.println("Write on server called !!!");
        // Ask the coordinator
        TTransport coordinatorTransport = new TSocket(coordinator.ipAddress, coordinator.port);
        coordinatorTransport.open();
        TProtocol coordinatorProtocol = new TBinaryProtocol(new TFramedTransport(coordinatorTransport));
        Server.Client coord  = new Server.Client(coordinatorProtocol);
        
	System.out.println("TALKING TO COORDINATOR");

        boolean status = coord.write(filename,contents);

        coordinatorTransport.close();
        
        return status;
    }
    
    
    @Override
    public ByteBuffer read(String filename) throws TException {
        // Ask the coordinator for a quorum which will return a machine with the file we want to read from.
        TTransport coordinatorTransport = new TSocket(coordinator.ipAddress, coordinator.port);
        coordinatorTransport.open();
        TProtocol coordinatorProtocol = new TBinaryProtocol(new TFramedTransport(coordinatorTransport));
        Server.Client coord  = new Server.Client(coordinatorProtocol);
        
        // This will block till the quorum is done.
        ByteBuffer contents = coord.read(filename);

        coordinatorTransport.close();
        
        return contents; //can be a ByteBuffer with null byte
    }
    
    
    @Override
    public boolean update(String filename, int version, ByteBuffer contents) throws TException {
	System.out.println("Updating file.");
       fs.put(filename,version);
       return Utils.write(directory+filename,contents);
    }
    
    
    /* only used by the Coordinator, stub */
    @Override
    public boolean enroll(Machine machine) throws TException {
	System.out.println("Enroll called on server. This should not happen.");
        return false;
    }
    
    
    @Override
    public int getLatestVersion(String filename) {
        return (fs.containsKey(filename)) ? fs.get(filename) : -1;
    }
    
    @Override
    public ByteBuffer directRead(String filename) {
        return Utils.read(directory + filename);
    }
    
    @Override
    public Map<String,Integer> ls() {
        return fs;
    }
    
    @Override
    public boolean sync (Map<String,String> globalFS) throws TException {
        // Look into FS
        Iterator it = globalFS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
	        String[] info = ((String) pair.getValue()).split("/");
            Integer version = Integer.parseInt(info[2]);
            String filename = (String) pair.getKey();
            
            if(!fs.containsKey(filename)){
                // instant put
                TTransport serverTransport = new TSocket(info[0], Integer.parseInt(info[1]));
                serverTransport.open();
                TProtocol serverProtocol = new TBinaryProtocol(new TFramedTransport(serverTransport));
                Server.Client server  = new Server.Client(serverProtocol);
                
                ByteBuffer contents = server.directRead(filename);
                serverTransport.close();
                Utils.write(directory+filename, contents);
                fs.put(filename,version);
            }else{
                // compare versions
                if(version > fs.get(filename)){
                    // Update your files if version > current
                    TTransport serverTransport = new TSocket(info[0], Integer.parseInt(info[1]));
                    serverTransport.open();
                    TProtocol serverProtocol = new TBinaryProtocol(new TFramedTransport(serverTransport));
                    Server.Client server  = new Server.Client(serverProtocol);
                    
                    ByteBuffer contents = server.directRead(filename);
                    serverTransport.close();
                    Utils.write(directory+filename, contents);
                    fs.put(filename,version);
                }
            }   
        }
        return true;  
    }
    
    /* Constructor for a Server, a Thrift connection is made to the coordinator as well */
    public ServerHandler(String coordinatorIP, Integer coordinatorPort, Integer port) throws Exception {    
        // connect to the coordinator as a client
        TTransport coordinatorTransport = new TSocket(coordinatorIP, coordinatorPort);
        coordinatorTransport.open();
        TProtocol coordinatorProtocol = new TBinaryProtocol(new TFramedTransport(coordinatorTransport));
        Server.Client coordinatorClient = new Server.Client(coordinatorProtocol);    
        this.coordinator = new Machine();
        this.coordinator.ipAddress = coordinatorIP;
        this.coordinator.port = coordinatorPort;
        
        //Create a Machine data type representing ourselves
        self = new Machine();
        self.ipAddress = InetAddress.getLocalHost().getHostName().toString();		
        self.port = port; //yay safe type casting
        
        // call enroll on superNode to enroll.
        boolean success = coordinatorClient.enroll(self);

        if(success)
            System.out.println("Node has reported to Coordinator");
        else
            System.out.println("Could not report to Coordinator... damn.");
        
        directory = Utils.initializeFolder(self);
	    fs = new HashMap<>();

        coordinatorTransport.close();
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
