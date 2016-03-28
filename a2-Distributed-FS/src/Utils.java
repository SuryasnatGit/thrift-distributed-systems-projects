import java.nio.file;
import java.nio.ByteBuffer;

/*
  Utility class used by server-side 
*/
public static class Utils {

    //raw write to local system, does not do any RPC calls
    public static boolean write (String filepath, ByteBuffer contents) {
 	File file = new File(filepath);

	if(file.isDirectory()) {
	    System.out.println("Util: Filename is a directory.");
	    return false;
	}

	if(file.exists())
	    System.out.println("Util file exists, overwriting.");

	try {
	    FileChannel channel = new FileOutputStream(file, false).getChannel();
	    channel.write(contents);
	    channel.close();
	    return true;
	}
	catch(Exception e) {
	    System.out.println("Client: Failed to read file contents");
	    e.printStackTrace();
	    return false;
	}
    }
    
    //raw read to local system, does not do ant RPC calls
    public static ByteBuffer read(String filepath) {
	byte[] contents = null; //lol initializing, amirite
	File file = new File(filepath);

	if(!file.exists() || file.isDirectory()) {
	    System.out.println("Util: Not a file or file doesn't exist. Returning null, which could crash thrift");
	    return null;
	}
	try {
	    //load the contents into a byte array
	    contents = Files.readAllBytes(Paths.get(filepath));
	}
	catch(Exception e) {
	    System.out.println("Util: Failed to read file contents");
	}
	finally {
	    if(contents != null) {
		filepath = Paths.get(filepath).getFileName().toString(); //strip any relative paths, just get filenames
		//send it over the wire
		System.out.println("New file name " + filepath);
		return server.write(filepath, ByteBuffer.wrap(contents));
	    }
	}
    }
}
