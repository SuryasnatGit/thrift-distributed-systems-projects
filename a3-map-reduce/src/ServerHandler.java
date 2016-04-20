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
import java.util.concurrent.ConcurrentLinkedQueue;


import java.io.RandomAccessFile;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.*;

public class ServerHandler implements Server.Iface {

    private static final String int_dir = "intermediate_dir/"; //Intermediate Folder
    private static final String out_dir = "output_dir/";

    Queue<Machine> computeNodes; //LinkedList
    Map<Machine,ConcurrentLinkedQueue<Task>> inProgress;
    Machine self;
    private Integer i_complete; // synchronized counter for completed tasks.
    private Long i_unique;   // synchronized counter for unique intermediate files

    private Queue<Task> tasks;  // task queue. ConcurrentLinkedQueue
    private Queue<String> completed; //holds unique identifier (output) for completed sort jobs

    
    public ServerHandler(Integer port) throws Exception {
        this.computeNodes = new ConcurrentLinkedQueue<Machine>();
        this.inProgress = new HashMap<>();
        this.tasks = new ConcurrentLinkedQueue<>();
        this.completed = new ConcurrentLinkedQueue<>();
        
	this.i_complete = 0;
	this.i_unique = 0L;

        //Create a Machine data type representing ourselves
        self = new Machine();
        self.ipAddress = InetAddress.getLocalHost().getHostName().toString();		
        self.port = port;

	//initialize folder(s)
	if(!(new File(int_dir)).mkdir()) //one line folder init mkdir bby!
	    System.out.println("Folder already exists: " + int_dir);
	if(!(new File(out_dir)).mkdir())
	    System.out.println("Folder already exists: " + out_dir);
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

	    //assign unique intermediate output filenames, lol one line type cast and increments
	    for(Task t : tasks)
		((SortTask) t).output = int_dir + String.valueOf(i_unique++); 
	
	    int totalTasks = tasks.size();
	    ServerStats.setTasks(totalTasks,totalTasks-1);
	    long startTime = System.currentTimeMillis();
	    System.out.println("Beginning to perform a total of " + totalTasks + " sorts. This may take awhile" );
	    //start contacting all nodes and queue it all onto compute machines
	    for(int i = 0; i < totalTasks; i++){
		SortTask task = (SortTask) tasks.poll();
		if(task != null) {
		    Machine current = computeNodes.remove();
	
		    // Bring it to the back of the queue
		    computeNodes.add(current);
		    // Make RPC call
		    rpcSort(current, task);
		    // Add to progress
		    addToProgress(current, task);
		}
	    }
	
	    // blocking wait for all tasks for it all to complete.
	    // Watches the queuefor all tasks for it all to complete.
	    System.out.println("Contacted & assigned sort tasks to all compute nodes, waiting for sort tasks to complete.");
	    while(i_complete < totalTasks){
		SortTask task = null;
		if(!tasks.isEmpty()){
			task = (SortTask) tasks.poll();
		}

		if(task != null){
			Machine current = null;
			try{
				current = computeNodes.remove();
			} catch(Exception e){
				System.out.println("All nodes have died.");
				System.exit(1);
			}
		    System.out.println("Reassigning to: " + current);
		    try{
				// Do a RPC call.
				rpcSort(current,task);
				// Bring it to the back of the queue
				computeNodes.add(current);
				// Add to the progress.
				addToProgress(current,task);
			} catch(TException e) {
				ServerStats.fault("sort");
				recover(current);
			}
		}{
			Thread.sleep(100);
		}
	    }

		long endTime = System.currentTimeMillis();
		ServerStats.recordTasks(startTime,endTime,"sort");
	    System.out.println("Sort complete, processing intermediate files for merging.");
	    // Now merge.	
	    this.mergify(); //create MergeTasks

	    System.out.println("Performing initial number of merges :  " + tasks.size());
	    
	    // Assign Merge Tasks to Machine.
	    startTime = System.currentTimeMillis();
	    for(int i = 0; i < tasks.size(); i++){
		MergeTask task = (MergeTask) tasks.poll();
		if(task != null) {
		    Machine current = computeNodes.remove();
		    // Bring it to the back of the queue
		    computeNodes.add(current);
		    // Do a RPC call.
		    rpcMerge(current,task);
		    // Add to the progress.
		    addToProgress(current,task);
		}
		else 
		    System.out.println("TASK IS NULL");
	    }
	    System.out.println("Contacted and assigned merge tasks to all compute nodes, waiting for merge tasks to complete.");
	    //wait for it all to complete
	    while(i_complete > 1){
		if(completed.size() > 1)
		    this.mergify(); //see if we need to create more tasks

		MergeTask task = null;
		synchronized(tasks) {
		    if(!tasks.isEmpty()){
			task = (MergeTask) tasks.poll();
		    }
		}
		if(task != null){
		    Machine current = computeNodes.remove();	
		    // Bring it to the back of the queue
		    computeNodes.add(current);
		    // Make a RPC call
		    rpcMerge(current,task);
		    // Add to the progress.
		    addToProgress(current,task);
		}
	    }
	    endTime = System.currentTimeMillis();
		ServerStats.recordTasks(startTime,endTime,"merge");
		ServerStats.print();
	    System.out.println("FINISHED COMPUTE, RESULT FOUND AT: " + completed);
	    collectStats();
	    System.out.println("Merging complete.");
	    String out_file = this.output(filename);
	    this.cleanup();
	    return out_file;
	}
	catch(Exception e)
	{
		e.printStackTrace();
		return "NULL";
	}
    }

    @Override
    // RPC Called by the compute nodes when they have done their task
    public boolean announce(Machine m, String task_output) throws TException {
	//System.out.println("SERVER: RPC COMPLETED TASK WITH OUTPUT OF: " + task_output);
	//remove the completed task from the machineTask Q in inProgress
	Queue<Task> machineTaskQ = inProgress.get(m);
	Task completedTask = null;
	for(Task t : machineTaskQ) {
	    if(t.output.equals(task_output)) {
		completedTask = t;
		machineTaskQ.remove(t);
		break;
	    }
	}
	
	synchronized(i_complete) {
	    if(completedTask instanceof SortTask)
		i_complete++;
	    else if(completedTask instanceof MergeTask)
		i_complete--;
	    else 
		throw new TException("No instance of task found.");
	}

	//add completed unique filename
	completed.add(task_output);
	
	return true;
    }
    
    // Loops through each machine and collects their stats.
    public void collectStats() throws TException{
		for(Machine m : computeNodes){
			try{
			TTransport computeTransport = new TSocket(m.ipAddress, m.port);
			computeTransport.open();
			TProtocol computeProtocol = new TBinaryProtocol(new TFramedTransport(computeTransport));
			ComputeNode.Client computeNode  = new ComputeNode.Client(computeProtocol);
			
			System.out.println(computeNode.getStats());
			computeTransport.close();
			} catch(Exception e){
				// ignore.
			}	
		}
	}
    
    public void recover(Machine m){
		// Look into the inProgress map
		Queue<Task> temp = inProgress.get(m);
		
		if(temp != null){
			for(Task t : temp){
				System.out.println("Server: Adding Task " + t);
			}
			
			// Dump all the tasks back into the queue
			tasks.addAll(temp);
			System.out.println("Server: Size of current TaskQueue: " + tasks.size());
		}
	}

    private synchronized void mergify() throws Exception {
	//checks the completedTask List to see if we need to merge anything
	synchronized(completed) {
	    //don't merge anything if there's only 1 completed task
	    while(completed.size() > 1) {
		String first = completed.remove();
		String second = completed.remove();
		Task merge = new MergeTask(first, second, int_dir + String.valueOf(i_unique));
		i_unique++;
		tasks.add(merge);
	    }
	}
    }

    private String output(String ori_file_path) throws Exception {
	assert completed.size() == 1;
	//move the completed mergesort to dest dir
	String ori_file = Paths.get(ori_file_path).getFileName().toString();
	String complete = completed.remove();
	Path ret = Files.move(Paths.get(complete), Paths.get(out_dir + ori_file + "_sorted"), StandardCopyOption.REPLACE_EXISTING);
	return ret.toString();
    } 
    
    private void addToProgress(Machine m,Task task){
		ConcurrentLinkedQueue<Task> machineTasks = inProgress.get(m);
		if(machineTasks == null){
			machineTasks = new ConcurrentLinkedQueue<>();
			machineTasks.add(task);
			inProgress.put(m,machineTasks);
		}
		else
		    machineTasks.add(task);
		
	}
    
    private void rpcSort(Machine m,SortTask task) throws TException {
	
	TTransport computeTransport = new TSocket(m.ipAddress, m.port);
	computeTransport.open();
	TProtocol computeProtocol = new TBinaryProtocol(new TFramedTransport(computeTransport));
	ComputeNode.Client computeNode  = new ComputeNode.Client(computeProtocol);
	
	computeNode.sort(task.filename,task.startOffset,task.endOffset,task.output);
	computeTransport.close();
    }
	
	private void rpcMerge(Machine m,MergeTask task) throws TException{
		TTransport computeTransport = new TSocket(m.ipAddress, m.port);
		computeTransport.open();
		TProtocol computeProtocol = new TBinaryProtocol(new TFramedTransport(computeTransport));
		ComputeNode.Client computeNode  = new ComputeNode.Client(computeProtocol);
		
		computeNode.merge(task.f1,task.f2,task.output);
		computeTransport.close();
	}

    /* ---- PRIVATE HELPER FUNCTIONS ---- */
    private void chunkify(String filename, Integer chunksize) throws Exception {
	System.out.println("Processing file for map reduce ..");
	// get the file size and do math on the chunks
	// read the file
	File dataFile = new File(filename);
	long filesize = dataFile.length();

	FileInputStream fis = new FileInputStream(dataFile);
	//assert fis.available() == filesize; 
	
	// note totalchunks == total number of tasks
	// just realized we don't need this
	// long totalChunks = filesize / chunksize + (filesize % chunksize);
	
	// divide up integer stream into chunks, upperbound
	int b = 0;
	long start = 0;
	long end = -1; //offsets start at 0, account for that increment.

	//keep reading until EOF
	while(b != -1) {
	    int counter = chunksize;
	    //move over an entire chunk size
	    while(counter > 0 && b != -1) {
		b = fis.read();
		counter--;
		end++;
	    }

	    //calibrate to ensure we don't chop off integers. 32 is ascii code point for a space.
	    while(b != -1 && b != 32) {
		b = fis.read();
		end++;
	    }
	    //System.out.println("REMAINING " + fis.available());
	    //insert chunk into tasks, assign unique file names in another function.
	    SortTask task = new SortTask(start, end, filename);
	    tasks.add(task);
 
	    //update start point for next chunk
	    start = end + 1;
	}	
    }

    // reset state for next job
    private void cleanup() {
	i_complete = 0;
	i_unique = 0L;
	tasks.clear();
        inProgress.clear();
	completed.clear();
    }


    //Begin Thrift Server instance for a Node and listen for connections on our port
    private void start() throws TException {
        
        HeartBeat heartBeatThread = new HeartBeat(this,computeNodes,inProgress,tasks);
        heartBeatThread.start();
        
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
