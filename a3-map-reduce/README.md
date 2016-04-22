# MapReduce Implementation with Sort/Merge

Team Members:  	
- Danh Nguyen (nguy1952)
- Wen Chuan Lee (leex7095)


For a quick overview on how to run the project and test, please go to the **How to Run the Project section**, as well reading the **Client** section regarding the UI commands.

# Overall Design

We designed the entire system to perform map-reduce in a non-blocking manner. After considering all the possible failure scenarios this was the key
to the design of a robust system that is able to continually function as long as the server is alive and there is at least one compute node. 

On a high level, the Client first makes a request to perform a computation on the server after connecting to the server. The client will then block
until it receives the output file name containing the result. This is the only blocking call.

The assumptions on this entire set up are that: 

(a) The server will not crash
(b) The all distributed processes have a shared/common filesystem on NFS (Networked File System)

The server will then first perform an analysis on the data file based on the chunk size provided by the client to compute how many sort tasks have to be carried out. The larger the chunk size, the smaller the number of sort tasks to be carried out. The server then assigns these sort tasks to all compute nodes in a FIFO manner, and wait for all sorting to be complete. This is done by having the compute node perform an RPC call back to the server. Once all tasks (including any tasks that have been failed and reassigned with a heartbeat algorithm) are complete. The server then calculates the number of merges to be done based on the number of intermediate files, and the number of files per merge (provided by the client). 

Merges are then assigned to the same compute nodes, and each compute node will perform n-way merging. **To ensure the system is able to merge large files**, we open each file as a special stream that is peekable. This ensures we do not have to read in all files in memory, reducing the possibility of running out of memory.

# The Server

- Server acts as dispatcher to all of the compute nodes.
- Client gives the server a job
- Server breaks the jobs into tasks
- Those tasks are distrobuted to the compute nodes
- the server maintains state.
- the server waits until the compute nodes announce to the server they are done.
- server moves on to next stage 
- server shares responsibility with heartbeat in maintaining that nodes 
that die, have their tasks recovered.

# HeartBeat

The heartbeat thread checks all running compute nodes to see if they 
are down. The heartbeat is a server thread and pings each compute
node. When pinging the compute server if there is a error or exception 
that occurs then the node is down.

After it detects this, the heartbeat calls a recovery function on the 
node. What this does is take the node out of the system and reassigns
the node's running tasks into the task queue. 

# Compute Nodes

The compute node is responsible for processing the sort and merge tasks. 
It has two components; A thread responsible for maintaining the queue of
tasks and a thread pool that puts tasks from the server into the server.
The server makes rpc calls to the compute node to add Sort and Merge tasks
into the requesr queue.

While maintaining the queue the QueueWatcher thread sees if there are 
any tasks to be ran and when there is it creates a SortMerge thread
to handle it. The SortMerge thread will sort a file or merge k files
depending the taks descriptions. When it is done it will announce back 
to the server so that the server can keep track of the progress.

## Fault Induction

Faults are introduced into the compute server when it pops a task from 
the queue. Before actually creating a thread to run the task, it generates
a random number to compare against the given chance to fail. If the number
is less than the given chance to fail then it will call System.exit(). In
Java when a thread calls this the entire process exits. So the QueueWatcher
is able to end execution of the Compute Node and all SortMerge threads.

# Client

The Client is a terminal to the Server. 

The Client will establish a connection to the main server.

If the Client is successful, an simple interactive terminal asking for user input is then launched.

If the Client is unsuccessful, the Client will go to sleep for one second before retrying indefinitely.

The terminal contains a few simple commands to interact with the File Server.

 - `sort <filename> <chunk size> <number of merges>` - 
 - `ls` - lists all files in the `data` directory
 - `exit` - closes the connection to the Server and quits the interactive terminal

# Performance Results


# Stress Testing
=large file, =10 servers
Lots of tasks (small chunksize)

# Performance Testing
Due to time constraints we perform tests on the 20mb (half one), we can assert that if it works on larger files it can work on smaller files


for 15 servers:
				for 10 % 50% and 90% of the file size as chunk size
							k merges where k = 2, k = n /2, k = n-1

# Fault Testing
20mb, create upper and lower from formula, runtime
upper being faults = amount of servers
--> show if faults > num servers all will die

duplicate the largest performance test with x y z faults , calculate percentage impact

Unit Testing
- file not found
- kill node when enrolling
- kill node when assigning tasks
- kill node when assigning merge
- kill node when merging
- kill node when sorting
- run another job after another has completed

# How to Run the Project

The File Server was built with the help of Ant. A prerequisite to run this project requires Ant. Additionally, the generated thrift files were also kept in a version control, but the `Thrift` compiler is also needed if Thrift files need to be generated (else only the Java Thrift library is needed).

Ant was used to handle automatic building and generation of class files (into a separate `bin` directory) as well as creating short targets for rapid developement.

### *To start the Coordinator, Servers, and Client on localhost*

(This starts all processes on one terminal using forked processes and will cause all processes to print to the same `System.out`, which would cause very messy output as all threads attempt to print to `stdout` in an unsynchronized way.)

    ant start-all
    
### *To start the File Server across multiple machines*
    1. ssh username@x32-05.cselabs.umn.edu
    2. cd into project directory
    3. ant start-server
    4. ssh username@x32-XX.cselabs.umn.edu (open another terminal)
    5. cd into project directory
    6. ant start-node
    7. Repeat steps 4 - 6 for the other Nodes
    8. ssh username@x32-XX.cselabs.umn.edu (or open another terminal)
    9. cd into project directory
    10. ant start-client
    
## Note

The compute nodes connect to the server to enroll into the MapReduce server located at `localhost` on port `9090`, **to override this default, on Step 6 do**:

    ant -Dserver.address='x32-XX' -Dserver.port='XXXX' start-node

**Please see Ant Targets and Overriding Properties for more information.**

## Ant Targets

The fastest way to test out the entire DHT is to create 1 SuperNode and multiple Nodes on the same machine (but with a different port). Multiple targets have been provided for automating the process of starting everything. 

Note: Run commands from the project directory, where `build.xml` is localed.

  - `ant start-all`
      - Create a MapReduce server (port 9090) and 10 Nodes and a Client on the same machine.  
      - All processes run in their own Java VM and as a forked process.
      - Client waits for 4 seconds before connecting the MapReduce server
      - Note: see Ant Properties on how to start targets with different values.

- `ant start-server`
  - Starts the coordinator on the current machine on the port specified by `server.port`

- `ant start-node`
  - Starts a node that will connect to the server located at `server.address` and `server.port`.

- `ant start client`
  - Starts the client that connects to the server specified under `server.address` and `server.port`

- `ant thrift`
  - Fires up the thrift compiler to generates Java versions of the `.thrift` files located in `src` and saves them to `src/gen-java`.
  - This target should be called everytime the new *thrift* interfaces have been defined pr to update to a newer version of *thrift*.
  - Targets must be modified if additional *thrift* files are created, as I could not find a way for *Ant* to pass files individually to the *Thrift* compiler without introducing Ant plugins or Maven.

- `ant build`
  - Builds but doesn't run the project. Note all targets that run depend on this target, so there is no need to run `ant build` before running the target.

- `ant clean`
  - Deletes all compiled bytecode in `bin`

- `ant diagnostics`
  - Prints out the Java Class Path defined in *master-classpath* 

### Ant properties (and overriding them)

Many of the above targets depend on some known information about the SuperNode and the ports to connect to. Sometimes these constants should be overridden (for example when starting multiple nodes/clients on the same machine). **To Override these constants**, use the format:

    -D(propertyName)=(value) 

To start the Server with a minimum of `10` nodes:
    
    ant -Dserver.port=5050 start-server

With a different port number `5555`:

    ant -Dserver.port=8 -Dserver.port=5555 start-server
    
Alternatively open up `build.xml` and edit the values:

     <!--Default Coordinator and Server properties used unless provided-->   
     
     <property name="server.address" value="localhost" />
     <property name="server.port" value="9090"/>
     <property name="node.port" value="5000" />

     <property name="src.dir" value="src" />
     <property name="build.dir" value = "bin"/>
     <property name="thrift.install" value="/usr/local/Thrift/" />
     
     <!-- CHANGE THESE VALUES -->   
     <property name="node.chanceToFail" value="0.01"/>
