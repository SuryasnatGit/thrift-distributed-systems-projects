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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Iterator;

class HeartBeat extends Thread {
    public final int HEARTBEAT = 5000;  //heartbeat frequency in miliseconds 20 seconds * 1000 = 20 000 ms

    // References
    Queue<Machine> nodes;
    Map<Machine,ConcurrentLinkedQueue<Task>> inProgress;    
    Queue<Task> taskQueue; //impl: ConcurrentLinkedQueue
    
    public HeartBeat(Queue<Machine> nodes,Map<Machine,ConcurrentLinkedQueue<Task>> inProgress, Queue<Task> taskQueue) {
        this.nodes = nodes;
	this.inProgress = inProgress;
	this.taskQueue = taskQueue;
    }

    @Override
    public void run(){
        while(true){
            try{
                Thread.sleep(HEARTBEAT);
                
					int totalServers = nodes.size();
                    for(int i = 0; i < totalServers; i++){
						Machine m = nodes.remove();
                        try{
							TTransport nodeTransport = new TSocket(m.ipAddress, m.port);
							nodeTransport.open();
							TProtocol nodeProtocol = new TBinaryProtocol(new TFramedTransport(nodeTransport));
							ComputeNode.Client node  = new ComputeNode.Client(nodeProtocol);
                            node.heartbeat();
                            nodes.add(m);
                        }catch(TException e){
							System.out.println("DOWN!!!!!!: Machine " + m.port);
                           recover(m);
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
		
		if(tasks != null){
			for(Task t : tasks){
				System.out.println("Heart: Adding Task " + t);
			}
			
			// Dump all the tasks back into the queue
			taskQueue.addAll(tasks);
			System.out.println("Heart: Size of current TaskQueue: " + taskQueue.size());
		}
	}
}
