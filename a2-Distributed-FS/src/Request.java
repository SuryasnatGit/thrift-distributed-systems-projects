import java.nio.ByteBuffer;

// Request Data objects
public abstract class Request{
    String type;
    String filename;
    Machine origin;
}

class WriteRequest extends Request{
    
    //holds the data we want to write
    public final ByteBuffer contents;

    public WriteRequest(Machine origin, String filename, ByteBuffer contents){
        this.type = "write";
	this.origin = origin;
	this.filename = filename;
	this.contents = contents;
    }
}

class ReadRequest extends Request {

    public ReadRequest(Machine origin, String filename){
        this.type = "read";
	this.origin = origin;
	this.filename = filename;
    }
}
