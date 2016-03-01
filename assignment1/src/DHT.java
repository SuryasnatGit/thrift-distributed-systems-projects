import java.util.HashMap;
import java.util.List;

class DHT{
	
	Map<Integer,Machine> table;
    int numMachines;
    int nodeID;
    public DHT(int nodeID){
        this.nodeID = nodeID;
        this.table = new HashMap<Integer,Machine>();
    }
	
		// Void but should be a connection...		
	Machine getMachine(String filename, int target, int origin, boolean stop){
	    // Hash the file name
		String hash = filename.hashCode();
		
	    // Getting which machine the file is ours.
		target = hash % numMachines;
		
	    // If file  reside in this machine,
	    if(nodeID == target){
	        // Do something once we found what we wanted
	        // return a connection to this machine
	    }else if(int index = inDHT(target) && index >= 0){
            Machine successor = table.get(index);
            return successor;
	    }
	    else{
	   	    Iterator it = table.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                if(pair.getValue().id <= machineValue){
                    // connect to the machine
                    if(stop == true && origin == nodeID)
                        return null;
                    return getMachine(filename,target,origin,true);
                }
            }
	    }
	}
	
	Integer contains(int num){
	    Iterator it = table.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            if(pair.getValue().id == machineValue){
                return pair.getValue();
            }
        }
        
        return -1;
	}
	
	int indexToMachine(int index){
		return (nodeID + Math.pow(2,index)) % nodeID;
	}
	
	void update(List<Node> NodesList){
	    numMachines = NodesList.size();
	    numIndexes = Math.log(numMachines) / Math.log(2);
	    for(int i =0; i<numIndexes; i++){
	    	int nodeIndex = indexToMachine(i);
	    	table.set(i,NodesList.get(nodeIndex);
	    } 
	}
	
	
	@Override 
	public boolean equals(List<Node> that){
	    return that.size() == this.table.size();
	}
}