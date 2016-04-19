public class Comm {
    static Machine server;
    static Machine self;

    public static void setServer(Machine m) {
	server = m;
    }

    public static void setSelf(Machine m) {
	self = m;
    }

    public static Machine getSelf() {
	return self;
    }
	
    public static Machine getServer(){
	return server;
    }
}
