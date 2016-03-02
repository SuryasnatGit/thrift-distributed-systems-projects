# Distributed Hash Table Implementation

#SuperNode

##Main
##Join
##PostJoin
##GetNode




#Node

#`FindMachine(String Filename,List<Integer> Chain)`
A RPC function used to find the machine that has the file specified by the file name. First the filename is hashed then to locate which machine
it would be put into the module of the number of machines in the system is taken.
Read and write depend on the function FindMachine to find a machine that has the resource the user
is requesting to read or write. If the machine is the current machine then no RPC calls have to be made.
Other wise the current machine will have to make hops across the network to find the resource.

FindMachine's recursion to find the resource works as follows:
    1. Check if the current machine is in the call chain to prevent loops.
    2. Check if the current machine has the file
    3. Check if we need to jump to a machine in  the DHT
    4. Make a recursive RPC call until we reach step 1.
    5. Bubble the target machine back to the origin.
    
FindMachine maintains a chain that it appends to with each node it visits. If it has visited a node thats already in the chain,
then it will not call FindMachine again in that node and thus the file does not exist in the system.


If its the case that the file resides in the system there are three scenarios covered in this case:
    1. The file resides in the current machine
    2. The file resides in a machine in the current machine's DHT
    3. The file resides in a successor of a machien in the current machine's DHT
    

This recursive lookup nature was designed to be similar to the CHORD DHT.

#`Write(String Filename,String Contents)`
After finding a machine, write behaves as follows depending who that machine is:
    1. If it is the current machine, write the file and the contents to the the current machine's in memory filesystem.
    2. If it is located across the network, make a write call through the use of RPC on that machine, passing the file and it's contents.

This will accomplish putting the file the user uploaded into the system. 

If the file does not exist in the system, FindMachine will return a invalid machine
and return false to the client.

#`Read(String Filename)`
After finding a machine, read behaves as follows depending who that machine is:
    1. If it is the current machine, read the file from the in memory file system and return it.
    2. If it is located across the network, make a read call through the use of RPC on that machine, passing the filename.
    
This will accomplish returning the contents of a requested resource. 

If the file does not exist in the system, FindMachine will return a invalid machine and read will detect this
and return a blank to the user.

#`UpdateDHT(List<Machine> NodesList,List<Integer> Chain)`
UpdateDHT is split into two parts:
    1. A call to update it's own finger table
    2. A recursive call to update all machine's in it's finger table.
 
Upon calling UpdateDHT, the current machine passes the list of Nodes to it's finger table where the DHT class it houses will calculate
the successors at each index.

Afterwards the current machine will make recursive RPC calls to all machine's in it's finger table to update their finger tables. While traversing machines,
a chain is passed throughout the process to keep track of machines that have already been traversed to prevent loops and redundancy.

##`Main`

# DHT
#`SearchDHT(String filename,Integer target)`
SearchDHT handles two scenarios:
    1. If the machine resides in the DHT 
    2. Return a machine in the fingertable with the ID less than or equal to the target. 

If the machine resides in the DHT then it will return that a object that contains information to connect to that machine.
If the machine does not reside in the DHT then the table will need to make a network hop to a successor machine. So the the fingertable
will return the first machine with a ID less than or equal to the target machine.

SearchDHT is called whenever a node joins the system so the recursive nature of this call allows the nodes in the system to reindex
to account for new members joining the system.

#`Update(List<Machine> NodesList>`
Given a NodesList then the machine will perform the calculation,

`successor = (v + 2^i) % number of machines`, for each index up to the number of indexes. 

Where `number of indexes = lg2 number of machines`.

and put the successor into that index and thus forms the finger table.

#Client


#How To Run The Project

##Ant

#Test Cases

## Sunny Day Scenarios

## Rainy Day Scenarios