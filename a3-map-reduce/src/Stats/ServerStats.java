public class ServerStats{    
    static int numFaults;
    static int sortFaults;
    static int mergeFaults;
    static long timeSort;
    static long timeMerge;
    static long totalTime;
    static int sortTasks;
    static int mergeTasks;
    
    public static synchronized void setTasks(int n,int m){
		sortTasks = n;
		mergeTasks = m;
	}
    
    public static synchronized void recordTasks(long start,long end, String type){
		totalTime += (end - start);
		if(type.equals("sort")){
			timeSort += (end - start);
		} else{
			timeMerge += (end - start);
		}
    }
    
    public static synchronized void fault(String type){
		numFaults++;
		if(type.equals("sort")){
			sortFaults++;
		} 
		if(type.equals("merge")){
			mergeFaults++;
		}
	}
    
    public static void print(){
        System.out.println("\n\n");
        System.out.println("===================================");
        System.out.println("Total Tasks Queued: " + (sortTasks + mergeTasks)); 
        System.out.println("Total Time: " + totalTime +" ms/task");
        System.out.println("Total Average Time: " + (totalTime)/(sortTasks+mergeTasks) +" ms/task");
        System.out.println("Number of Faults: " + numFaults);
        System.out.println("================SORT===============");
        System.out.println("Total Tasks: " + sortTasks); 
        System.out.println("Total Average Time: " + timeSort/sortTasks +" ms/task");
        System.out.println("Number of Faults: " + sortFaults);
        System.out.println("===============MERGE================");
        System.out.println("Total Tasks: " + mergeTasks); 
        System.out.println("Total Average Time: " + timeMerge/mergeTasks +" ms/task");
        System.out.println("Number of Faults: " + mergeFaults);
        System.out.println("===================================");
	    System.out.println("\n\n");
    }
    
    public static String stats(){
	return "\n\n"+
        "==================================="+
        "Total Tasks Queued: " + (sortTasks + mergeTasks)+ 
        "Total Time: " + totalTime +" ms/task"+
        "Total Average Time: " + (totalTime)/(sortTasks+mergeTasks) +" ms/task"+
        "Number of Faults: " + numFaults+
        "================SORT==============="+
        "Total Tasks: " + sortTasks+ 
        "Total Average Time: " + timeSort/sortTasks +" ms/task"+
        "Number of Faults: " + sortFaults+
        "===============MERGE================"+
        "Total Tasks: " + mergeTasks+ 
        "Total Average Time: " + timeMerge/mergeTasks +" ms/task"+
        "Number of Faults: " + mergeFaults +
        "==================================="+ 
        "\n\n";
	}
}
