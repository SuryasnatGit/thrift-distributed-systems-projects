//Thrift imports
import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.*;
import org.apache.thrift.server.*;
import java.io.FilterOutputStream
 
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
        
        // Split by space
        
        // Convert to int arrayList
        
        List<Integer> mockList = new ArrayList<>();
        Collections.sort(mockList);
        Writer wr = new FileWriter(new File(task.output));
        
        for(Integer i: mockList){
			wr.write(i + " ");
		}
		
        wr.close();
        return false;
    }
   public boolean merge(MergeTask task) throws TException {
	   Writer wr = new FileWriter(new File(task.output));
	   Scanner sc1 = new Scanner(new File(task.f1));
	   Scanner sc2 = new Scanner(new File(task.f2));
	   
		
	   if(sc1.hasNextInt() && sc2.hasNextInt()){
		   int a = sc1.nextInt();
		   int b = sc2.nextInt();
		   while (sc1.hasNextInt() && sc2.hasNextInt()) {
			if(a < b){
				wr.write(a + " ");
				a.nextInt();
			} else { 
				wr.write(b + " ");
				b.nextInt();
			}
		   }
       }
       
       if(sc1.hasNextInt()){
		   while(sc1.hasNextInt()){
			   int c = sc1.nextInt();
			   wr.write(c + " ");
			}
	   }else{
			while(sc2.hasNextInt()){
			   int c = sc2.nextInt();
			   wr.write(c + " ");
			}
	   }
       wr.close();
       return false;
    }

}
