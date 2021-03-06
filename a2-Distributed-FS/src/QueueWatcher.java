//started by Coordinator to monitor the requests queue
//let's just do one for now because more threads and runnables on top of Thrift's mutlithreading
//  is a scary beast I have not tamed.
import java.util.*;
import java.nio.ByteBuffer;

class QueueWatcher extends Thread {
    private static int instance = 0;
    private final Queue<Request> requests; //synchronized
    private final Set<Request> subscriptions; //NOT synchronized, ServerHandler threads wait here.
    private final Coordinator coordinatorInstance;

    public QueueWatcher(Queue<Request> requests, Set<Request> subscriptions, Coordinator coordinatorInstance) {
	this.requests = requests;
	this.subscriptions = subscriptions;
	this.coordinatorInstance = coordinatorInstance;
	setName("QueueWatcher Thread: " + (instance++));
    }


    @Override
    public void run() {
	while(true) {
	    try {
		Request req = null;
		synchronized (requests) {

		    while(requests.isEmpty())
			requests.wait();
		    
		    //queue is no longer empty
		    req = requests.remove();

		    if(req.type.equals("write"))
			while(!subscriptions.isEmpty()); //block any further additions to subsciptions, wait for all read requests to complete

		    subscriptions.add(req);

		}

		//exit critical section and allow more requests to be added to the request queue.
		if(req.type.equals("write"))
		{
		    synchronized(subscriptions) {
			while(subscriptions.contains(req)) {
			    subscriptions.notifyAll();
			    subscriptions.wait(); //wait for blocking write to complete.
			}
		    }
		    //System.out.println("Watcher: released subscriptions\n\n\n");
		}

	    }
	    catch(Exception e) {
		//e.printStackTrace();
		break;
	    }
	}
    }
}
