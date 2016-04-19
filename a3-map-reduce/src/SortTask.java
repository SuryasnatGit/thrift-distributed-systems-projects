class SortTask extends Task {
    
    long startOffset;
    long endOffset;
    String filename;
    
    public SortTask(long startOffset, long endOffset, String filename, String output) {
	this.startOffset = startOffset;
	this.endOffset = endOffset;
	this.filename = filename;
	this.output = output;
    }


    public SortTask(long startOffset, long endOffset, String filename) {
	this.startOffset = startOffset;
	this.endOffset = endOffset;
	this.filename = filename;
	this.output = null;
    }

    @Override
    public String toString() {
	return "SORT: START " + startOffset + " END: " + endOffset + " FILE: " + filename + " ---> " + output;
    }
}
