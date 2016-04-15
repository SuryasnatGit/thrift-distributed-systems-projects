//Thrift imports
import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.*;
import org.apache.thrift.server.*;
 

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.nio.ByteBuffer;
import java.net.InetAddress;
import java.util.ArrayList;

import java.util.Iterator;

class HeartBeat extends Thread {
    public final int HEARTBEAT = 5000;  //heartbeat frequency in miliseconds 20 seconds * 1000 = 20 000 ms

    // References
    ArrayList<Machine> nodes;
    HashMap<Machine,Queue<Task>> inProgress;    
    Queue<Task> taskQueue;
    
    public HeartBeat(ArrayList<Machine> nodes,HashMap<Machine,Queue<Task>> inProgress,Queue<Task> taskQueue) {
        this.nodes = nodes;
	this.inProgress = inProgress;
	this.taskQueue = taskQueue;
    }

    @Override
    public void run(){
        while(true){
            try{
                Thread.sleep(HEARTBEAT);
                
                synchronized(nodes) {
                    for(Machine m : nodes) {
                        TTransport nodeTransport = new TSocket(m.ipAddress, m.port);
                        nodeTransport.open();
                        TProtocol nodeProtocol = new TBinaryProtocol(new TFramedTransport(nodeTransport));
                        ComputeNode.Client node  = new ComputeNode.Client(nodeProtocol);
                        
                        try{
                            node.heartbeat();
                        }catch(TException e){
                           recover(m);
                        }
                    }
                                
                }
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }

	public void recover(Machine m){
		// Look into the inProgress map
		Queue<Task> tasks = inProgress.get(m);

		// Dump all the tasks back into the queue
		synchronized(taskQueue){
			taskQueue.addAll(tasks);
		}

		// Remove machine from list of machines
		synchronized(nodes){
			nodes.remove(m);
		}
	}
}
