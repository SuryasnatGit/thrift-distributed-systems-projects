import java.util.List;

class MergeTask extends Task {
    List<String> filenames;
    public MergeTask(List<String> filenames, String output) {
	this.output = output;
	this.filenames = filenames;
    }

    @Override
    public String toString() {
	return "MERGE: " + filenames.toString() + " ---> " + output; 
    }
}
