import java.util.*;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

class QueueWatcher extends Thread {
    private final ConcurrentLinkedQueue<Task> requests; //synchronized

    public QueueWatcher(ConcurrentLinkedQueue<Task> tasks) {
		this.requests = tasks;
    }


    @Override
    public void run() {
	while(true) {
	    try {
			Task task = null;
			while(requests.isEmpty()){
				Thread.sleep(100);
			}

		    //queue is no longer empty
		    task = requests.remove();
		    
			if(task != null){
				SortMerge handler = new SortMerge(task);
				handler.start();
			}
		} catch(Exception e) {
			e.printStackTrace();
			break;
	    }
    }
	}
}
