public class ServerStats{    
    static int numFaults;
    static int sortFaults;
    static int mergeFaults;
    static int miscFaults;
    static long timeSort;
    static long timeMerge;
    static long totalTime;
    static int sortTasks;
    static int mergeTasks;
    
    public static synchronized void setTasks(int n,int m){
		sortTasks = n;
		mergeTasks = m;
	}
    
    public static synchronized void addMerge(int n){
		mergeTasks += n;
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
		else if(type.equals("merge")){
			mergeFaults++;
		}else {
			miscFaults++;
		}
	}
    
    public static void print(){
        System.out.println("\n\n");
        System.out.println("==============SERVER================");
        System.out.println("Total Tasks Queued: " + (sortTasks + mergeTasks)); 
        System.out.println("Total Time: " + totalTime +" ms");
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
        "============SERVER=============="+
        "\nTotal Tasks Queued: " + (sortTasks + mergeTasks)+ 
        "\nTotal Time: " + totalTime +" ms"+
        "\nTotal Average Time: " + (totalTime)/(sortTasks+mergeTasks) +" ms/task"+
        "\nNumber of Faults: " + numFaults+
        "\n================SORT==============="+
        "\nTotal Tasks: " + sortTasks+ 
        "\nTotal Average Time: " + timeSort/sortTasks +" ms/task"+
        "\nNumber of Faults: " + sortFaults+
        "\n===============MERGE================"+
        "\nTotal Tasks: " + mergeTasks+ 
        "\nTotal Average Time: " + timeMerge/mergeTasks +" ms/task"+
        "\nNumber of Faults: " + mergeFaults +
        "\n==================================="+ 
        "\n\n";
	}
}
