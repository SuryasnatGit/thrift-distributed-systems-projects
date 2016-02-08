import org.apache.thrift.server.*;
import org.apache.thrift.transport.*;


public class MulServer {
    //server that runs the handler service
    public static void main(String[] argv){

	try {

	    //Create Thrift server socket
	    TServerTransport serverTransport = new TServerSocket(9090);
	    TTransportFactory factory = new TFramedTransport.Factory();

	    //Create service request handler
	    MulHandler handler = new MulHandler();
	    MulService.Processor processor = new MulService.Processor(handler);
 
	    //Set Server Arguments
	    TServer.Args args = new TServer.Args(serverTransport);
	    args.processor(processor); //Set handler
	    args.transportFactory(factory); //Set FramedTransport (for performance)

	    //Run server as single thread
	    TServer server = new TSimpleServer(args);
	    server.serve();
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
