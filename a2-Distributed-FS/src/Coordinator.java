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

public class Coordinator implements Server.Iface {
    private HashMap<String,Integer> fs;
    private ArrayList<Machine> servers;
    private Queue<Request> requests; // Request Queue
    private Set<Request> subscriptions; //place to keep checking for subscriptions
    private String directory;

    Random rand;
    Machine self;
    
    // Variables used for quorum
    private Integer nr;
    private Integer nw;

    public Coordinator(Integer port,Integer nw, Integer nr) throws Exception {
        // Init Coordinator Data Structures
	servers = new ArrayList<>();
        requests = new LinkedList<>();
	rand = new Random();
	subscriptions = new HashSet<>();

        // Init Quorum variables
        this.nr = nr;
        this.nw = nw;
        
        System.out.println("I am the Coordinator.");
        
        //Create a Machine data type representing ourselves
        self = new Machine();
        self.ipAddress = InetAddress.getLocalHost().getHostName().toString();		
        self.port = port;
        
        servers.add(self);

        directory = Utils.initializeFolder(self);
	fs = new HashMap<>();
    }

    @Override
    public boolean write(String filename, ByteBuffer contents) throws org.apache.thrift.TException {

	System.out.println("Writing: " + filename);

	WriteRequest req = new WriteRequest(filename, contents);
	//put the request into the request queue
	synchronized(requests) {
	    requests.add(req);
	    requests.notifyAll();
	}
	
	synchronized(subscriptions) {
	
	    try {
		while(!subscriptions.contains(req))
		    subscriptions.wait(); //wait
	    }
	    catch(Exception e) {
		e.printStackTrace();
	    }

	    // Get Nw Machines
	    List<Machine> quorum = getMachines(nw);
        
	    // Check versions of each machine.
	    Integer mostUpdated = -1;
	    Machine updatedMachine = null;
	    // Loop through each machine in NW and get the latest version.
	    for(Machine m : quorum) {
		Integer version = null;
		
		if(!m.equals(self)) {
		    TTransport serverTransport = new TSocket(m.ipAddress, m.port);
		    serverTransport.open();
		    TProtocol serverProtocol = new TBinaryProtocol(new TFramedTransport(serverTransport));
		    Server.Client server  = new Server.Client(serverProtocol);

		    // Most Updated Version Number/Machine
		    version = server.getLatestVersion(filename);
		    serverTransport.close();
		}
		else 
		    version = this.getLatestVersion(filename);

		System.out.println(m.toString()+":"+filename+"("+version+")");
		if(version > mostUpdated){
		    updatedMachine = m;
		    mostUpdated = version;
		} 
	    }
	    // Update the most updated number
	    mostUpdated += 1;
        
	    // Loop through each machine in NW and update
	    for(Machine m : quorum) {

		if(!m.equals(self)) {
		    TTransport serverTransport = new TSocket(m.ipAddress, m.port);
		    serverTransport.open();
		    TProtocol serverProtocol = new TBinaryProtocol(new TFramedTransport(serverTransport));
		    Server.Client server  = new Server.Client(serverProtocol);
            
		    // Updates all contents in NW.
		    ByteBuffer duped = contents.duplicate();
		    System.out.println("Updating: " +m.toString()+":"+filename+"("+mostUpdated+")");
		    server.update(filename, mostUpdated, duped);

		    serverTransport.close();
		}
		else{
		    ByteBuffer duped = contents.duplicate();
		    this.update(filename, mostUpdated, duped);
		}
	    }
	    subscriptions.remove(req);
	    subscriptions.notifyAll(); //wake sleeping monitors
	}
        return true;
    }

    @Override
    public ByteBuffer read(String filename) throws TException {
    System.out.println("Reading: " + filename);
	ReadRequest req = new ReadRequest(filename);
	//put the request into the request queue
	synchronized(requests) {
	    requests.add(req);
	    requests.notifyAll();
	}
	

	while(!subscriptions.contains(req)); //wait

	//remove since we got the signal
	subscriptions.remove(req);

        // Get Nr Machines
    System.out.println("GETTING QUORUM with nr=" + nr);
	List<Machine> quorum = getMachines(nr);
        
        // Check versions of each machine.
        Integer mostUpdated = -1;
        Machine updatedMachine = null;
        // Loop through each machine in NR and get the latest version.
        for(Machine m : quorum) {

	    Integer version = null;

	    if(!m.equals(self)) {
		TTransport serverTransport = new TSocket(m.ipAddress, m.port);
		serverTransport.open();
		TProtocol serverProtocol = new TBinaryProtocol(new TFramedTransport(serverTransport));
		Server.Client server  = new Server.Client(serverProtocol);
            
		// Most Updated Version Number/Machine
		version = server.getLatestVersion(filename);
		serverTransport.close();
	    }
	    else 
		version = getLatestVersion(filename);

	    System.out.println(m.toString()+":"+filename+"("+version+")");
            if(version > mostUpdated){
                updatedMachine = m;
                mostUpdated = version;
            }       
        }

	ByteBuffer contents = null;

	if(mostUpdated == -1){ //doesn't exist
	    System.out.println(filename + " doesn't exist in quorumed servers");
	    return ByteBuffer.wrap("NULL".getBytes());
	}
        
	if(updatedMachine.equals(self))
	   contents = this.directRead(filename);
	else {
	    TTransport serverTransport = new TSocket(updatedMachine.ipAddress, updatedMachine.port);
	    serverTransport.open();
	    TProtocol serverProtocol = new TBinaryProtocol(new TFramedTransport(serverTransport));
	    Server.Client server  = new Server.Client(serverProtocol);
        
	    // Get the contents of the most updated Machine
	    contents = server.directRead(filename);
	    serverTransport.close();
	}
        return contents;   
    }

    
    @Override
    public boolean update(String filename, int version, ByteBuffer contents) throws TException {
       fs.put(filename,version);
       return Utils.write(directory+filename,contents);
    }

    @Override
    public boolean enroll(Machine machine) throws TException {
        System.out.println("ENROLL CALLED ON COORDINATOR");
        servers.add(machine);
        
        // Set the quorum variables.
        /*
        nw = servers.size() / 2 + 1;
        nr = nw;
        */
        System.out.println(servers.toString());
        return true;
    }
    
    @Override
    public ByteBuffer directRead(String filename) {
        ByteBuffer contents = Utils.read(directory+filename);
        if(contents == null)
            return ByteBuffer.wrap("NULL".getBytes());
        return contents;
    }


    @Override
    public int getLatestVersion(String filename){
        return (fs.containsKey(filename)) ? fs.get(filename) : -1;
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
                this.update(filename,version,contents);
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
                    this.update(filename,version,contents);
                }
            }   
        }
	this.exportFileSystemState();
        return true;  
    }


    //function that exports the FS hashtable of filename to version numbers
    //called everytime we sync filesystems
    private void exportFileSystemState() {
	StringBuffer sb = new StringBuffer(fs.size() * 2);
	sb.append("\t FILENAME \t\t VERSION NO\n\n");
	Set set = fs.entrySet();
	Iterator it = set.iterator();
	while (it.hasNext()) {
	    Map.Entry entry = (Map.Entry) it.next();
	    sb.append(entry.getKey() + " \t:\t " + entry.getValue());
	    sb.append('\n');
	}
	Utils.write(directory + "FILE_VERSIONS_" + self.hashCode() + ".txt", ByteBuffer.wrap(sb.toString().getBytes()));
	//add hashcode to minimize possible overwriting, also lol bytebufferwrapping.
    }

    // We return a array of references to random machines
    // used to assemble a quorum
    public List<Machine> getMachines(int n){
	if(n > servers.size())
	    n = servers.size(); //avoid requesting for more than what we have
	System.out.println("getMachines : " + n);
	List<Machine> machineList = new ArrayList<>();
        while(n > 0) {
	    Machine machine = servers.get(rand.nextInt(servers.size()));
	    while(machineList.contains(machine))
		machine = servers.get(rand.nextInt(servers.size()));

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
            Integer NW = Integer.parseInt(args[1]);
            Integer NR = Integer.parseInt(args[2]);
	    Coordinator coordinator = new Coordinator(port,NW,NR);
            
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
	QueueWatcher watcher = new QueueWatcher(this.requests, this.subscriptions, this);
	watcher.start();
	System.out.println("Watcher thread started ..");
    
	ServerSync syncThread = new ServerSync(servers);
	syncThread.start();
	System.out.println("Sync thread started .. will sync every " + syncThread.SYNC_FREQUENCY + " seconds.");

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
        
	System.out.println("Coordinator is listening ... ");
        server.serve();
    }
}
