import java.util.*;
import java.nio.ByteBuffer;

class QueueWatcher extends Thread {
    private final Queue<Task> requests; //synchronized

    public QueueWatcher(Queue<Task> tasks) {
	this.requests = tasks;
    }


    @Override
    public void run() {
	while(true) {
	    try {
		Task task = null;
		synchronized (requests) {

		    while(requests.isEmpty())
			requests.wait();

		    //queue is no longer empty
		    task = requests.remove();
		}
		if(task != null){
			SortMerge handler = new SortMerge(task);
			handler.start();
		}  

	    }
	    catch(Exception e) {
		e.printStackTrace();
		break;
	    }
	}
    }
}
