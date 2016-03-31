public class  Stats{
    private long t_Reads;
    private long t_Writes;
    private long startTime;
    
    public void logRead(){
        t_Reads += (System.currentTimeMillis() - startTime);
    }
    
    public  void logWrite(){
        t_Writes += (System.currentTimeMillis() - startTime);
    }
    
    public void start(){
        startTime = System.currentTimeMillis();
    }
    
    public long total(){
        return t_Reads + t_Writes;
    }
    
    public void print(){
        System.out.println("===================================");
        System.out.println("Total Time Reads: " + t_Reads +" ms");
        System.out.println("Total Time Writes: " + t_Writes+" ms");
        System.out.println("Total Time: " + total()+" ms");
        System.out.println("===================================");
    }
}