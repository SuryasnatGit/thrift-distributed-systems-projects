import org.apache.thrift.TException;

public class MulHandler implements MulService.Iface {

    @Override 
    public boolean ping() throws org.apache.thrift.TException {
	return true;
    }

    @Override 
    public int multiply_1(numbers value) throws org.apache.thrift.TException {
	return value.x * value.y;
    }

    @Override
    public int multiply_2(int x, int y) throws org.apache.thrift.TException {
	return x * y;
    }

}
