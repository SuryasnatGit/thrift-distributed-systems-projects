import java.util.Scanner;
import java.io.File;

//Thanks: http://stackoverflow.com/a/4288861/4512948
public class PeekableScanner implements Comparable<PeekableScanner>
{
    private Scanner scan;
    private Integer next;

    public PeekableScanner( File source ) throws Exception
    {
        scan = new Scanner( source );
        next = (scan.hasNextInt() ? scan.nextInt() : null);
    }

    public boolean hasNext()
    {
        return (next != null);
    }

    public Integer next()
    {
        Integer current = next;
        next = (scan.hasNextInt() ? scan.nextInt() : null);
	return current;
    }

    public Integer peek()
    {
        return next;
    }

    @Override
    public int compareTo(PeekableScanner other) 
    {
	//check if two numbers be equal or not.
	if(peek() == other.peek())
	    return 0;
	else if(peek() > other.peek())
	    return 1;
	else // if(next < other.next)
	    return -1;
    } 
}
