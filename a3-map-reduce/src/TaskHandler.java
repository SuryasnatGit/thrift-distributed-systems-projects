//Thrift imports
import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.*;
import org.apache.thrift.server.*;
 
class TaskHandler extends Thread { 
    Task task;
    public TaskHandler(Task task) {
        this.task = task;
    }

    @Override
    public void run(){
	if(task instanceof SortTask){
		sort(task);
	} else{
		merge(task);
	}
    }
    public boolean sort(SortTask task) throws TException {
        // Open file as a stream.
        
        // Jump to the startChunk offset
        
        // Read uptil then endChunk
        
        // Split by new line 
        
        // Convert to int arrayList
        
        // collections sort
        
        // dump to output
        
        // return output
        
        return false;
    }
   public boolean merge(MergeTask task) throws TException {
        // Open both f1 and f2 using scanner
        
        // compare the next ints
        
        // write the next smallest
        
        // flush the file.
        return false;
    }

}
