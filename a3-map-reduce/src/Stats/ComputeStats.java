public class ComputeStats{

    static int numOfTasks = 0;
    static int finishedTasks = 0;
    static long totalTime = 0;
    static long sortTime = 0;
    static long mergeTime = 0;
    static int numSort = 0;
    static int numMerge = 0;
    static int finishedSort = 0;
    static int finishedMerge = 0;
    
    
    
    public static  synchronized  void newTask(String type){
        
        numOfTasks++;
        if(type.equals("sort"))
            numSort++;
        else
            numMerge++;
    }
    
    public static synchronized void endTask(long start,long end,String type){
        
        finishedTasks++;    
        totalTime += (end-start);
        if(type.equals("sort")){
            sortTime += (end-start);
            finishedSort++;
        }
        else{ 
            mergeTime += (end-start);
            finishedMerge++;
        }
    }
    
    
    public static void print(){
        System.out.println("\n\n");
        System.out.println("===================================");
        System.out.println("Total Tasks: " + numOfTasks); 
        System.out.println("Total Tasks Finished: " + finishedTasks);
        System.out.println("Total Average Time: " + totalTime/finishedTasks +" ms/task");
        System.out.println("================SORT===============");
        System.out.println("Total Tasks: " + numSort); 
        System.out.println("Total Tasks Finished: " + finishedSort);
        System.out.println("Total Average Time: " + sortTime/finishedSort +" ms/task");
        System.out.println("===============MERGE================");
        System.out.println("Total Tasks: " + numMerge); 
        System.out.println("Total Tasks Finished: " + finishedMerge);
        System.out.println("Total Average Time: " + mergeTime/finishedMerge +" ms/task");
        System.out.println("===================================");
	    System.out.println("\n\n");
    }
    
    @Override
    public String toString(){
        return "\n\n"+
        "==================================="+
        "Total Tasks: " + numOfTasks+ 
        "Total Tasks Finished: " + finishedTasks+
        "Total Average Time: " + totalTime/finishedTasks +" ms/task"+
        "================SORT==============="+
        "Total Tasks: " + numSort+ 
        "Total Tasks Finished: " + finishedSort+
        "Total Average Time: " + sortTime/finishedSort +" ms/task"+
        "===============MERGE================"+
        "Total Tasks: " + numMerge+ 
        "Total Tasks Finished: " + finishedMerge+
        "Total Average Time: " + mergeTime/finishedMerge +" ms/task"+
        "==================================="+
	    "\n\n";
    }
}
