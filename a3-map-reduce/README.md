# MapReduce Implementation with Sort/Merge

Team Members:  	
- Danh Nguyen (nguy1952)
- Wen Chuan Lee (leex7095)


For a quick overview on how to run the project and test, please go to the **How to Run the Project section**, as well reading the **Client** section regarding the UI commands.

# Overall Design

We designed the entire system to perform map-reduce in a non-blocking manner. After considering all the possible failure scenarios this was the key
to the design of a robust system that is able to continually function as long as the server is alive and there is at least one compute node. 

On a high level, the Client first makes a request to perform a computation on a file listed in `data` on the server after connecting to the server. The client will then block
until it receives the output file name containing the result. This is the only blocking call.

The assumptions on this entire set up are that: 

(a) The server will not crash
(b) The all distributed processes have a shared/common filesystem on NFS (Networked File System)

The server will then first perform an analysis on the data file based on the chunk size provided by the client to compute how many sort tasks have to be carried out. The larger the chunk size, the smaller the number of sort tasks to be carried out. The server then assigns these sort tasks to all compute nodes in a FIFO manner, and wait for all sorting to be complete. This is done by having the compute node perform an RPC call back to the server. Once all tasks (including any tasks that have been failed and reassigned with a heartbeat algorithm) are complete. The server then calculates the number of merges to be done based on the number of intermediate files, and the number of files per merge (provided by the client). 

Merges are then assigned to the same compute nodes, and each compute node will perform n-way merging. **To ensure the system is able to merge large files**, we open each file as a special stream that is peekable. This ensures we do not have to read in all files in memory, reducing the possibility of running out of memory. By utilizing lazy streams into a priority queue we can ensure that we are able to merge up to `k` number of files.

Intermediate files in the `intermediate_dir` that have been successfully merged and sorted are deleted to save space and the final output is sent to the `output_dir`.

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

- running in parallel with the server
- checks a shared list of actively running nodes
- nodes that dont respond are taken out and have their tasks redistributed

# Compute Nodes

- maintains a queue
- rpc calls are a alias for putting the task in the queue.
- compute node contains a seperate thread that processes the tasks.
- a task that is popped from the queue is assigned to a new sort merge task.
- the new thread performs a sort or a merge based on the task object
- when it is finished it alerts the server it is done.

# Client

- The Client is a terminal to the Server. 
- The Client will establish a connection to the main server.
- If the Client is successful, an simple interactive terminal asking for user input is then launched.
- If the Client is unsuccessful, the Client will go to sleep for one second before retrying indefinitely.
- The terminal contains a few simple commands to interact with the File Server.
    - `sort <filename> <chunk size> <number of merges>` - performs a map-reduce merge sort on file `filename` with `chunk_size` byte chunk sizes and that are merged with `number of merges` number of files per merge.
    - `ls` - lists all files in the `data` directory
    - `exit` - closes the connection to the Server and quits the interactive terminal

- **Example command**: `sort 200000 200 12` tells the entire cluster to perform merge-sort on a file named `200000` with chunk sizes of `200` bytes, which are finally merged with `12` files per merge. 


## SortTask / MergeTask

## SortMerge

We start off a thread for every sort/merge task on the compute node, ensuring that we are able to perform concurrent sorts and merges on each compute node. We have a `QueueWatcher` thread that maintains a lock-free access on a concurrent linked queue within a `computeNode`. The QueueWatcher starts up a sort/merge task in an individual thread, and then performs an RPC call to the server to `announce` that the task has been completed. This ensures that we are able to perform multiple tasks concurrently. Since each task does not depend on the previous task like in most map reduce cases, each task is individually carried out.

Sorting is first done by advancing (`skip`) a BufferedReader to a specific postion of the data file (which is also being read concurrently by other `ComputeNode` processes, and reading in a specific number of bytes (based on the calibration and chunk size). After spliting the `character` buffer that is transformed into a `String` and parsing the entire `String` array into `Integers`, we sort the numbers (using Java's internal `Colections.sort` to ensure performance efficiency). 

Merging is then done after all sorting is complete. After being assigned a `MergeTask`, merging is executed in another thread.


# Performance Results

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
    
## How To Run the entire project

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
