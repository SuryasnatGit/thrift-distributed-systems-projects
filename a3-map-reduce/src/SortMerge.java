//Thrift imports
import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.*;
import org.apache.thrift.server.*;

import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.nio.file.*;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.PriorityQueue;
 
class SortMerge extends Thread { 
    Task task;
    public SortMerge(Task task) {
        this.task = task;
    }

    @Override
    public void run(){
		try{
			if(task instanceof SortTask){
				sort((SortTask)task);
			} else{
				merge((MergeTask)task);
			}
			
			this.announce(task);
			
			if(task instanceof MergeTask) 
			    this.removeIntermediateFiles((MergeTask) task);
			
		} catch(Exception e){
			e.printStackTrace();
		}
    }
    
    public void announce(Task task) throws TException{
		Machine svr = Comm.getServer();
		Machine self = Comm.getSelf();
		TTransport serverTransport = new TSocket(svr.ipAddress, svr.port);
		serverTransport.open();
		TProtocol serverProtocol = new TBinaryProtocol(new TFramedTransport(serverTransport));
		Server.Client server  = new Server.Client(serverProtocol);
            
		// Most Updated Version Number/Machine
		server.announce(self, task.output);
		serverTransport.close();
	}

    private void removeIntermediateFiles(MergeTask mt) throws Exception {
	for(String p : mt.filenames) {
	    Files.deleteIfExists(Paths.get(p));
	}
    }

    public boolean sort(SortTask task) throws TException {
	//got to wrap it up because of IOExceptions.
	Writer wr = null;
	try {
	    //System.out.println("Sorting task -> " + task );
	    List<Integer> data = readFileToIntList(task);
	    Collections.sort(data); //magic of abstractions!
	    //wr = new BufferedWriter(new FileWriter(new File(task.output)));
	    wr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(task.output),"ascii")); 
	    for(Integer i: data){
		wr.write(String.valueOf(i));
		wr.write(" ");
	    }
	    wr.close(); //this should be in the finally block but nested try-catches on the top level is disgraceful
	    return true;
	}
	catch (Exception e) {
	    e.printStackTrace();
	    return false;
	}
    }

    public boolean merge(MergeTask task) throws TException {
	Writer wr = null;
	try {
	    wr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(task.output), "ascii"));
	    //open a all the streams and stuff it into a priority q
	    PriorityQueue<PeekableScanner> q = new PriorityQueue<>(task.filenames.size());
	    for(String filename : task.filenames) {
		PeekableScanner pks = new PeekableScanner(new File(filename));
		q.add(pks);
	    }
	    
	    //poll for numbers and keep getting the next int
	    PeekableScanner smallest = q.poll();
	    //stop when there's nothing else
	    while(smallest != null) {
		if(smallest.hasNext()) {
		    //write the smallest int
		    wr.write(smallest.peek());
		    //advance scanner
		    smallest.next();
		}
		//else remove from the queue, aka do nothing
		
		//then check if next scanner has numbers, if so add a space else don't
		if(q.peek().hasNext()) wr.write(" ");
		//put current smallest back into queue
	        q.add(smallest);
	    }
	    wr.close();
	    return true;
	}
	catch(Exception e) {
	    e.printStackTrace();
	    return false;
	}
    }

    /*    
    public boolean mergeOLD(MergeTask task) throws TException {
	//System.out.println("MERGING TASK: " + task);
	Writer wr = null;
	try {
	    wr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(task.output), "ascii")); 
	    Scanner sc1 = new Scanner(new File(task.f1));
	    Scanner sc2 = new Scanner(new File(task.f2));

	    Integer a = null;
	    Integer b = null;
	    while(sc1.hasNextInt() || sc2.hasNextInt()) {
		if(a == null && sc1.hasNextInt()) a = sc1.nextInt();
		if(b == null && sc2.hasNextInt()) b = sc2.nextInt();
		
		if(a != null && b != null) {
		    if(a < b) {
			wr.write(String.valueOf(a));
			a = null;
		    }
		    else {
			wr.write(String.valueOf(b));
			b = null;
		    }
		    wr.write(" ");
		}
		else {
		    //write remaining numbers
		    if(a != null) {
			wr.write(String.valueOf(a));
			a = null;
		    }
		    if(b != null) { //aka else
			wr.write(String.valueOf(b));
			b = null;
		    }
		    if(sc1.hasNextInt() || sc2.hasNextInt()) wr.write(" ");
		    //else don't write an int since we're at the end.
		}
	    }

	    //the last number
	    if (a != null) wr.write(String.valueOf(a));
	    if (b != null) wr.write(String.valueOf(b));
	    wr.close();

	    return true;
	}
	catch(Exception e) {
	    e.printStackTrace();
	    return false;
	}
    }
    */

    /* Open file as a stream.
       Jump to the startChunk offset
       Read uptil the endChunk.
       Split by space.
       Convert to int arrayList.
    */
    private List<Integer> readFileToIntList(SortTask t) throws Exception {
	int len = (int) (t.endOffset - t.startOffset + 1); //should never overflow since chunksize isn't going to be that big, I think.
	List<Integer> output = new ArrayList<>((int) len);

	BufferedReader rdr = new BufferedReader(new FileReader(t.filename));
	//advance reader to specific position in file
	rdr.skip(t.startOffset);
	//allocate memory and read into buffer.
	char[] buf = new char[(int) len];
	rdr.read(buf, 0, (int) len);

	//Split space delimted String and parse as integers
	String[] split = (new String(buf)).split(" ");
	for(String s : split)	    
	    output.add(Integer.parseInt(s.trim())); //trim because of EOF. Hidden, but length is +1.
	
	return output;
    }


}
