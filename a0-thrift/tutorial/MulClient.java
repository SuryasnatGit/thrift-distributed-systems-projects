
import org.apache.thrift.transport.*;
import org.apache.thrift.protocol.*;

public class MulClient {
    public static void main(String[] args) {
	try {
	    TTransport transport = new TSocket("localhost", 9090);
	    TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
	    MulService.Client client = new MulService.Client(protocol);

	    transport.open();
	    int ret = client.multiply_2(3, 5);
	    System.out.println("VALUE IS: " + ret);
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
