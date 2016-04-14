public abstract class Task {
    String output;
}

class SortTask extends Task {
    
    int startOffset;
    int endOffset;
    String filename;
    
    public SortTask(int startOffset, int endOffset, String filename, String output) {
	this.startOffset = startOffset;
	this.endOffset = endOffset;
	this.filename = filename;
	this.output = output;
    }
}

class MergeTask extends Task {
    String f1;
    String f2;
    public MergeTask(String f1, String f2, String output) {
	this.f1 = f1;
	this.f2 = f2;
	this.output = output;
    }
}
