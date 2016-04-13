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
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.Iterator;

public class ServerHandler implements Server.Iface {
    
    List<Machine> servers;
    Machine self;
    

    public ServerHandler(Integer port) throws Exception {
        servers = new ArrayList<Machine>();
        
        //Create a Machine data type representing ourselves
        self = new Machine();
        self.ipAddress = InetAddress.getLocalHost().getHostName().toString();		
        self.port = port;
        
    }

    @Override
    public boolean enroll(Machine machine) throws TException {
        servers.add(machine);
        System.out.println("\n\nList of Compute Nodes In the DFS : \n\n" + servers.toString());
        return true;
    }
    
    
    public static void main(String[] args) {
        if(args.length < 1) {
            System.err.println("Usage: java Coordinator <port>");
            return;
        }
        try {
            System.out.println("IP Address is " + InetAddress.getLocalHost().toString());
	        //port number used by this node.
            Integer port = Integer.parseInt(args[0]);
            
	       Coordinator coordinator = new Coordinator(port,NW,NR);
            
	       //spin up server
            coordinator.start();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
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
