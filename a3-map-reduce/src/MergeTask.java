
class MergeTask extends Task {
    String f1;
    String f2;
    public MergeTask(String f1, String f2, String output) {
	this.f1 = f1;
	this.f2 = f2;
	this.output = output;
    }

    @Override
    public String toString() {
	return "MERGE: " + f1 + " + " + f2 + " ---> " + output; 
    }
}
