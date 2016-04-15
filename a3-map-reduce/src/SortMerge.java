//Thrift imports
import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.*;
import org.apache.thrift.server.*;
 
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
		} catch(TException e){
			e.printStackTrace();
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
