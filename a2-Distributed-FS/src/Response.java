import java.nio.ByteBuffer;

// Request Data objects
public abstract class Response{
    String type;
    Machine origin;
}

class WriteResponse extends Response{
    boolean status;
    
    public WriteResponse(Machine origin, boolean status){
        this.type = "write";
        this.origin = origin;
        this.status = status;
    }
}

class ReadResponse extends Response {
    //holds the data we want to write
    private ByteBuffer contents;
    
    public ReadResponse(Machine origin, ByteBuffer contents){
        this.type = "read";
        this.origin = origin;
        this.contents = contents;
    }
}