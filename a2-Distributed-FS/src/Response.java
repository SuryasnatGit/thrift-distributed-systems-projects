import java.nio.ByteBuffer;

// Request Data objects
public abstract class Response {
    String type;
    Machine origin;
    Boolean status;
}

class WriteResponse extends Response{
    
    public WriteResponse(Machine origin, Boolean status){
        this.type = "write";
        this.origin = origin;
        this.status = status;
    }
}

class ReadResponse extends Response {
    //holds the data we want to write
    private ByteBuffer contents;
    
    public ReadResponse(Machine origin, Boolean status, ByteBuffer contents){
        this.type = "read";
        this.origin = origin;
	this.status = status;
        this.contents = contents;
    }
}
