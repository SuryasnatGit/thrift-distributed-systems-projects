public class ServerComm{
	static Machine server;
	
	public static void set(Machine m){
		server = m;
	}
	
	public static Machine get(){
		return server;
	}
}
