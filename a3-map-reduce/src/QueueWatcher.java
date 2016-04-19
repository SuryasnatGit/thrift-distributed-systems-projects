import java.util.*;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Random;

class QueueWatcher extends Thread {
    private final ConcurrentLinkedQueue<Task> requests; //synchronized
    private final ComputeNodeHandler instance;
    Random rand;

    public QueueWatcher(ComputeNodeHandler instance,ConcurrentLinkedQueue<Task> tasks) {
		this.requests = tasks;
		this.instance = instance;
		rand = new Random();
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
					tryToFail();
					SortMerge handler = new SortMerge(task);
					handler.start();
				}
			} catch(Exception e) {
				e.printStackTrace();
				break;
			}
		}
	}
	
	private void tryToFail(){
		double roll = rand.nextDouble();
		if(roll < instance.chanceToFail){
			//System.out.println("Roll: " + roll);
			//System.out.println("Chance: " + instance.chanceToFail);
			System.out.println("FAILED");
			System.exit(0);
		}
	}
}
