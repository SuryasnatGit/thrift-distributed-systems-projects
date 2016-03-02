# Distributed Hash Table Implementation

#SuperNode

##Main
##Join
##PostJoin
##GetNode




#Node

# `FindMachine(String Filename,List<Integer> Chain)`

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

# `Write(String Filename,String Contents)`

After finding a machine, write behaves as follows depending who that machine is:
    1. If it is the current machine, write the file and the contents to the the current machine's in memory filesystem.
    2. If it is located across the network, make a write call through the use of RPC on that machine, passing the file and it's contents.

This will accomplish putting the file the user uploaded into the system. 

If the file does not exist in the system, FindMachine will return a invalid machine
and return false to the client.

# `Read(String Filename)`

After finding a machine, read behaves as follows depending who that machine is:
    1. If it is the current machine, read the file from the in memory file system and return it.
    2. If it is located across the network, make a read call through the use of RPC on that machine, passing the filename.
    
This will accomplish returning the contents of a requested resource. 

If the file does not exist in the system, FindMachine will return a invalid machine and read will detect this
and return a blank to the user.

# `UpdateDHT(List<Machine> NodesList,List<Integer> Chain)`

UpdateDHT is split into two parts:
    1. A call to update it's own finger table
    2. A recursive call to update all machine's in it's finger table.
 
Upon calling UpdateDHT, the current machine passes the list of Nodes to it's finger table where the DHT class it houses will calculate
the successors at each index.

Afterwards the current machine will make recursive RPC calls to all machine's in it's finger table to update their finger tables. While traversing machines,
a chain is passed throughout the process to keep track of machines that have already been traversed to prevent loops and redundancy.

## `Main`

# DHT
# `SearchDHT(String filename,Integer target)`
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

# Client


# How to Run the Project

The Chord-based DHT was built with the help of Ant. A prerequisite to run this project requires Ant. Additionally, the generated thrift files were also kept in a version control, but the `Thrift` compiler is also needed if Thrift files need to be generated (else only the Java Thrift library is needed).

Ant was used to handle automatic building and generation of class files (into a separate `bin` directory) as well as creating short targets for rapid developement.

## Using Ant Targets

The fastest way to test out the entire DHT is to create 1 SuperNode and multiple Nodes on the same machine (but with a different port). Multiple targets have been provided for automating the process of starting everything. 

Note: Run commands from the project directory, where `build.xml` is localed.

  - `ant start-all`
      - Create 1 SuperNode (port 9090) and 5 Nodes (ports 8000, 8001, 8002, 8003, 8004), and a Client on the same machine.  
      - All processes run in their own Java VM and as a forked process.
      - Client waits for 4 seconds before deciding to joining the cluster.
      - Note: see Ant Properties on how to start targets with different values.

- `ant start-supernode`
  - Starts the superNode on the current machine on the port specified by `superNode.port` with `superNode.minNode` number of nodes.

- `ant start-node`
  - Starts a node that will connect to the superNode located at `superNode.address` and `superNode.port`.

- `ant start client`
  - Starts the client that connects to the supernode specified under `superNode.address` and `superNode.port`
  - *The client will not be allowed to join until there is a minimum number of nodes in the cluster.*
  - *After the client has joined, no new nodes are allowed to join the DHT.*

- `ant thrift`
  - Fires up the thrift compiler to generates Java versions of the `.thrift` files located in `src` and saves them to `src/gen-java`.
  - This target should be called everytime the new *thrift* interfaces have been defined pr to update to a newer version of *thrift*.
  - Targets must be modified if additional *thrift* files are created, as I could not find a way for *Ant* to pass files individually to the *Thrift* compiler without introducing Ant plugins or Maven.

- `ant build`
  - Build but don't run the project. Note all targets that run depend on this target, so there is no need to run `ant build` before running the target.

- `ant clean`
  - Deletes all compiled bytecode in `bin`

- `ant diagnostics`
  - Prints out the Java Class Path defined in *master-classpath* 

### Ant properties (and overriding them)

Many of the above targets depend on some known information about the SuperNode and the ports to connect to. Sometimes these constants should be overridden (for example when starting multiple nodes/clients on the same machine). **To Override these constants**, use the format:

    -D(propertyName)=(value) 

##### For example:

To start the SuperNode with a minimum of `10` nodes:
    
    ant -DsuperNode.minNode=5 start-supernode

With a different port number `5555`:

    ant -DsuperNode.minode=8 -DsuperNode.port=5555 start-supernode
    
Alternatively open up `build.xml` and edit the properties by hand:

     <!--Default SuperNode and Node properties-->
     <property name="superNode.address" value="localhost" />
     <property name="superNode.port" value="9090"/>
     <property name="superNode.minNode" value="5"/>
     <property name="node.port" value="8080"/>

     <property name="src.dir" value="src" />
     <property name="build.dir" value = "bin"/>
     <property name="thrift.install" value="/usr/local/Thrift/" />



#Test Cases


## Expected Finger Table For 5 Nodes

    Machine 0:
    Index | Machine
    0     | 1 = (0 + 2^0 % 5)
    1     | 2 = (0 + 2^1 % 5)
        
    Machine 1:
    Index | Machine
    0     | 2 = (1 + 2^0 % 5)
    1     | 3 = (1 + 2^1 % 5)
    
    Machine 2:
    Index | Machine
    0     | 3 = (2 + 2^0 % 5)
    1     | 4 = (2 + 2^1 % 5)    
    
    Machine 3:
    Index | Machine
    0     | 3 = (3 + 2^0 % 5)
    1     | 0 = (3 + 2^1 % 5)
    
    
    Machine 4:
    Index | Machine
    0     | 0 = (4 + 2^0 % 5)
    1     | 1 = (4 + 2^1 % 5)

## Sunny Day Scenarios
###Put in the machine 
###Put in a machine in the DHT
###Put in a machine in a network hop

###Get in the machine
###Get in a machine in the DHT
###Get in a machine in a network hop

## Rainy Day Scenarios

###Put a file you dont have.
###Read a file that doesn't exist

