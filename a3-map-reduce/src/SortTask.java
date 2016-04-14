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
