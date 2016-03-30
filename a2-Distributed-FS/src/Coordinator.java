//Thrift imports
import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.*;
import org.apache.thrift.server.*;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.ArrayList;
import java.util.LinkedList;
import java.net.InetAddress;
import java.nio.ByteBuffer;


public class Coordinator implements Server.Iface {
    private HashMap<String,Integer> fs;
    private ArrayList<Machine> servers;
    private Queue<Request> requests; // Request Queue
    private HashMap<Request, Response> response; //place to keep checking for response

    Random rand;
    Machine self;
    
    // Variables used for quorum
    int nr = 0;
    int nw = 0;

    public Coordinator(Integer port) throws Exception {
        // Init Coordinator Data Structures
	servers = new ArrayList<>();
        requests = new LinkedList<>();
	rand = new Random();
    response = new HashMap<>();

        System.out.println("I am the Coordinator.");
        
        //Create a Machine data type representing ourselves
        self = new Machine();
        self.ipAddress = InetAddress.getLocalHost().getHostName().toString();		
        self.port = port;
        
        servers.add(self);
    }

    @Override
    public boolean write(String filename, ByteBuffer contents) throws org.apache.thrift.TException {
        // Get Nr Machines
	List<Machine> quorum = getMachines(nw);
        
        // Check versions of each machine.
        Integer mostUpdated = -1;
        Machine updatedMachine = null;
        
        // Loop through each machine in NW and get the latest version.
        for(Machine m : quorum) {
            TTransport serverTransport = new TSocket(m.ipAddress, m.port);
            serverTransport.open();
            TProtocol serverProtocol = new TBinaryProtocol(new TFramedTransport(serverTransport));
            Server.Client server  = new Server.Client(serverProtocol);
            
            // Most Updated Version Number/Machine
            Integer version = Integer.parseInt(server.getLatestVersion(filename));
            if(version > mostUpdated){
                updatedMachine = m;
                mostUpdated = version;
            }
       
            serverTransport.close();
        }
        
        
        // Update the version number
        version += 1;
        
       // Loop through each machine in NW and update
        for(Machine m : quorum) {
            TTransport serverTransport = new TSocket(m.ipAddress, m.port);
            serverTransport.open();
            TProtocol serverProtocol = new TBinaryProtocol(new TFramedTransport(serverTransport));
            Server.Client server  = new Server.Client(serverProtocol);
            
            // Updates all contents in NW.
            server.update(filename,version,contents);
            
            serverTransport.close();
        }

        return true;
    }

    @Override
    public String read(String filename) throws TException {
        // Get Nr Machines
	List<Machine> quorum = getMachines(nr);
        
        // Check versions of each machine.
        Integer mostUpdated = -1;
        Machine updatedMachine = null;
        
        // Loop through each machine in NR and get the latest version.
        for(Machine m : quorum) {
            TTransport serverTransport = new TSocket(m.ipAddress, m.port);
            serverTransport.open();
            TProtocol serverProtocol = new TBinaryProtocol(new TFramedTransport(serverTransport));
            Server.Client server  = new Server.Client(serverProtocol);
            
            // Version number
            Integer version = Integer.parseInt(server.getLatestVersion(filename));
            if(version > mostUpdated){
                updatedMachine = m;
                mostUpdated = version;
            }
            
            serverTransport.close();
        }
        
        TTransport serverTransport = new TSocket(updatedMachine.ipAddress, updatedMachine.port);
        serverTransport.open();
        TProtocol serverProtocol = new TBinaryProtocol(new TFramedTransport(serverTransport));
        Server.Client server  = new Server.Client(serverProtocol);
        
        // Get the contents of the most updated Machine
        ByteBuffer contents = server.directRead(filename);
        
        serverTransport.close();
        return contents;   
    }

    @Override
    public boolean enroll(Machine machine) throws TException {
        System.out.println("ENROLL CALLED ON COORDINATOR");
        servers.add(machine);
        
        // Set the quorum variables.
        nw = servers.size() / 2 + 1;
        nr = nw;
        
        System.out.println(servers.toString());
        return true;
    }
    
    // We return a array of references to random machines
    // used to assemble a quorum
    public List<Machine> getMachines(int n){
	if(n > servers.size())
	    n = servers.size(); //avoid requesting for more than what we have

	List<Machine> machineList = new ArrayList<>();
        while(n > 0) {
	    Machine machine = rand.nextInt(servers.size());
	    while(machineList.contains(machine))
		continue;
	    machineList.add(machine);
	    n--;
	}

	return machineList;
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
            
        // Start Looping through and handling request queue.
            // Depending on request 
            // Handle it differently.
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    //Begin Thrift Server instance for a Node and listen for connections on our port
    private void start() throws TException {
	// start up thread that watches a queue, explicitly pass private references
	QueueWatcher watcher = new QueueWatcher(this.requests, this.response, this);
	watcher.start();

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
}
