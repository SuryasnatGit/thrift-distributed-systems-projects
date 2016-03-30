import java.nio.ByteBuffer;

// Request Data objects
public abstract class Request{
    String type;
    String filename;
}

class WriteRequest extends Request{
    
    //holds the data we want to write
    public final ByteBuffer contents;

    public WriteRequest(String filename, ByteBuffer contents){
        this.type = "write";
	this.filename = filename;
	this.contents = contents;
    }
}

class ReadRequest extends Request {

    public ReadRequest(String filename){
        this.type = "read";
	this.filename = filename;
    }
}
