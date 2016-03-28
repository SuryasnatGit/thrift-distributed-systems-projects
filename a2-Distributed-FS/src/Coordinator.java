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
import java.nio.ByteBuffer;


public class Coordinator implements Server.Iface {
    HashMap<String,Integer> fs;
    ArrayList<Machine> servers;
    Machine self;

    public Coordinator(Integer port) throws Exception {
	servers = new ArrayList<>();


        System.out.println("I am the Coordinator");
        
        //Create a Machine data type representing ourselves
        self = new Machine();
        self.ipAddress = InetAddress.getLocalHost().getHostName().toString();		
        self.port = port;
        
        servers.add(self);
    }

    @Override
    public boolean write(String filename, ByteBuffer contents) throws org.apache.thrift.TException {
        return false;
    }
    
    @Override
    public String read(String filename) throws TException {
        return "";
    }

    @Override
    public boolean enroll(Machine machine) throws TException {
	System.out.println("ENROLL CALLED ON COORDINAOTR");
	servers.add(machine);
	System.out.println(servers.toString());
	return true;
    }

    public static void main(String[] args) {
        if(args.length < 1) {
            System.err.println("Usage: java Coordinator <port>");
            return;
        }
        try {
            System.out.println("Our IP Address is " + InetAddress.getLocalHost().toString());
	    //port number used by this node.
            Integer port = Integer.parseInt(args[0]);
            
	    Coordinator coordinator = new Coordinator(port);
            
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
        TServer.Args serverArgs = new TServer.Args(serverTransport);
        serverArgs.processor(processor); //Set handler
        serverArgs.transportFactory(factory); //Set FramedTransport (for performance)

        //Run server with multiple threads
        TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).processor(processor));
        
        server.serve();
    }
}
