import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;

class DHT{
	
    Map<Integer,Machine> table;
    Integer numMachines; //init by updateDHT
    Integer nodeID;
    Integer numIndexes; 
    
    public DHT(int nodeID){
        this.nodeID = nodeID;
        this.table = new HashMap<Integer,Machine>();
    }
	
    Machine searchDHT(String filename, int target) throws Exception {
        Integer index = contains(target);
        
        if(index >= 0){
            Machine successor = table.get(index);
            if(successor == null) throw new Exception("searchDHT returned a null successor");
            return successor;
        }
        Iterator it = table.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            if(((Machine) pair.getValue()).id <= nodeID){
				return (Machine) pair.getValue();
            }
        }
        //return the last machine
        //System.out.println(numIndexes + " ex machina" );
        //if(table.get(numIndexes - 1) == null) throw new Exception("Machine Minus One Occured");
        return table.get(numIndexes - 1); //get the last index
    }
	
    Integer contains(int num){
	Iterator it = table.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            if(((Machine) pair.getValue()).id == num){
                return (int) pair.getKey();
            }
        }
        return -1;
    }
    
    int indexToMachine(int index){
	return (int) (nodeID + Math.pow(2,index)) % numMachines;
    }
    
    void update(List<Machine> NodesList){
	numMachines = NodesList.size();
	numIndexes = (int) (Math.log(numMachines) / Math.log(2));
	for(int i =0; i<numIndexes; i++){
	    int nodeIndex = indexToMachine(i);
	    table.put(i,NodesList.get(nodeIndex));
	}
    }

    void print(){
	System.out.println();
	System.out.println("======= Finger Table of Node: ("+nodeID+") =======");
	Iterator it = table.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
	    Machine i = (Machine) pair.getValue();
            System.out.println("[ " + pair.getKey() + " | " + i.id + " ] ->  " + i.ipAddress + ":" + i.port);
        }
    }

    @Override
    @SuppressWarnings("unchecked") //lol type safety
    public boolean equals(Object that){
	List<Machine> thatList = (List<Machine>) that;
	return thatList.size() == this.table.size();
    }
}
