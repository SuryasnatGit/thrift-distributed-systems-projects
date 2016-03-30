import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.channels.FileChannel;
import java.io.FileOutputStream;
import java.io.File;

/*
  Utility class used by server-side 
*/
public final class Utils {

    //disallow instantiation
    private Utils() {
    }

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

	    return ByteBuffer.wrap(contents);
	}
	catch(Exception e) {
	    System.out.println("Util: Failed to read file contents .. returning null");
	    return null;
	}
    }

    //Creates a directory using foldername in the current directory
    // returns the Path name to the dir
    public static String initializeFolder(Machine self) {
	File dir = new File(self.ipAddress + "_" + self.port);
	if(dir.mkdir())
	    return dir.getName() + "/";
	else {
	    System.out.println("Did not mkdir: " + dir.toString() + " maybe it already exists?");
	    return dir.getName() + "/";
	}
    }
}
