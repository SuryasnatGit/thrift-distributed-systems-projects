import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;

class DHT{
	
    Map<Integer,Machine> table;
    Integer numMachines; //init by updateDHT
    Integer nodeID;
    public DHT(int nodeID){
        this.nodeID = nodeID;
        this.table = new HashMap<Integer,Machine>();
    }
	
    Machine searchDHT(String filename, int target){
        Integer index = contains(target);
        if(index >= 0){
            Machine successor = table.get(index);
            return successor;
        }
        Iterator it = table.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            if(((Machine) pair.getValue()).id <= nodeID){
                return pair.getValue();
            }
        }
        //return the last machine
        return table.get(numMachines - 1);
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
	int numIndexes = (int) (Math.log(numMachines) / Math.log(2));
	for(int i =0; i<numIndexes; i++){
	    int nodeIndex = indexToMachine(i);
	    table.put(i,NodesList.get(nodeIndex));
	}
    }

    void print(){
	System.out.println("=======Machine DHT: "+nodeID+"=======");
	System.out.println(table);
	Iterator it = table.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            System.out.println("[ " + pair.getKey() + " | " + ((Machine)pair.getValue()).id + " ]");
        }
    }

    @Override
    @SuppressWarnings("unchecked") //lol type safety
    public boolean equals(Object that){
	List<Machine> thatList = (List<Machine>) that;
	return thatList.size() == this.table.size();
    }
}
