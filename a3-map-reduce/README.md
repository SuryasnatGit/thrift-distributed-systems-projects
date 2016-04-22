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

The Server acts as dispatcher to all of the compute nodes. When the client
gives the server a job, it breaks it into 4 steps. 
    1. Break up the file into chunks and turning those chunks into tasks.
    2. Distribute those sort tasks among all compute servers and then watch the queue for 
    any tasks that need to be redistributed because they went down. And when enough annoucements
    have been called it moves on to the next step.
    3. Create merge tasks and distribute the tasks.
    4. Distribute those merge tasks among all compute servers and then watch the queue for 
    any tasks that need to be redistributed because they went down. And when enough annoucements
    have been called it finishes and returns the final file name ot the server.

The key data structures in the server are the TaskQueue and the InProgress Map.
The TaskQueue uses Java's ConcurrentLinkedList so that it supports concurrent operation
by the heartbeat thread and the server thread, and the RPC thread pool. The TaskQueue
is a queue of Task objects and that need to be sent to compute servers to process. The
InProgress map is a mapping from machines to a list of tasks they are working on.

Since the heartbeat is not polling every milisecond there is a chance that if 
a node goes down the server could assign a task to it before the heartbeat recovers
it from the system. To account for this the server actively wraps any calls to a 
compute node with a try catch. If any errors occur then it uses the same recover
function that HeartBeat uses to recover the tasks and the node from the system.

# HeartBeat

The heartbeat thread checks all running compute nodes to see if they 
are down. The heartbeat is a server thread and pings each compute
node. When pinging the compute server if there is a error or exception 
that occurs then the node is down.

After it detects this, the heartbeat calls a recovery function on the 
node. What this does is take the node out of the system and reassigns
the node's running tasks into the task queue. The heartbeat thread performs 
RPC calls to other alive compute nodes and reassign Sort/Merge tasks.

# Compute Nodes

The compute node is responsible for processing the sort and merge tasks. 
It has two components; A thread responsible for maintaining the queue of
tasks and a thread pool that puts tasks from the server into the server.
The server makes RPC calls to the compute node to add Sort and Merge tasks
into the request queue.

While maintaining the queue the `QueueWatcher` thread sees if there are 
any tasks to be executed and when there is, it starts a new SortMerge thread
to handle it. The SortMerge thread will sort a file or merge k files
depending the taks descriptions. When it is done it will announce back 
to the server so that the server can keep track of the progress.

The `Sort` and `Merge` RPC calls made by the server to the compute node do not 
actually perform the sorting and merging, rather they atomically add a sort/merge task 
to the task queue and complete the RPC call, ensuring that the Server does not block when assigning 
multiple tasks to compute nodes that can possibly fail.

## Fault Injection

Faults are introduced into the compute server when it pops a task from 
the queue. Before actually creating a thread to run the task, it generates
a random number to compare against the given chance to fail. If the number
is less than the given chance to fail then it will call System.exit(). In
Java when a thread calls this the entire process exits. So the QueueWatcher
is able to end execution of the Compute Node and all SortMerge threads.

**The chance of a fault** is accessed before a beginning to execute the task 
previously assigned to the `computeNode`. **The probability of a fault can be increased or decreased**
by editing the `build.xml`, at the following parameter.

     <property name="node.chanceToFail" value="0.01"/>

A `computeNode` that has been simulated to fail based on the random number drawn will perform a `System.exit(0)`
without any clean up or notification, creating a `TException`. The `HeartBeat` thread will ensure tasks assigned
to a particular node that has already failed to another ComputeNode.

If all nodes die before an entire merge sort operation is completed, the server aborts the job and mentions that nodes have died.
The assignment description mentions that the main server should not fail and hence we did not introduce fault probability
to the main server.


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

We used object oriented programming principles in designing this framework by having an abstract class called `Task` and 2 classes `SortTask` and `MergeTask`. `SortTask` objects specify which file read as well as the start and end offset, while `MergeTask` holds a list of paths to intermediate files to merge. Both tasks have a unique filename assigned by the server. Through this method we can ensure that tasks that failed can be easily reexecuted and that files being written to are unique (avoiding any possibility of a race condition in a distributed system).

## SortMerge

We start off a thread for every sort/merge task on the compute node, ensuring that we are able to perform concurrent sorts and merges on each compute node. We have a `QueueWatcher` thread that maintains a lock-free access on a concurrent linked queue within a `computeNode`. The QueueWatcher starts up a sort/merge task in an individual thread, and then performs an RPC call to the server to `announce` that the task has been completed. This ensures that we are able to perform multiple tasks concurrently. Since each task does not depend on the previous task like in most map reduce cases, each task is individually carried out.

Sorting is first done by advancing (`skip`) a BufferedReader to a specific postion of the data file (which is also being read concurrently by other `ComputeNode` processes, and reading in a specific number of bytes (based on the calibration and chunk size). After spliting the `character` buffer that is transformed into a `String` and parsing the entire `String` array into `Integers`, we sort the numbers (using Java's internal `Colections.sort` to ensure performance efficiency). 

Merging is then done after all sorting is complete. After being assigned a `MergeTask`, merging is executed in another thread. Previously our design was to perform multiple merges in which only 2 intermediate files are merged each time as we did not completely understand the assignment goal. After rereading the assignment description, we realized we had to perform merging on any number of files, this is known as `k-way` merging. This was achieved by using a special form of a scanner that is able to read the next number in a scanner (stream), in a lazyway, and exploiting a Priority Queue that was able to automatically order each stream from smallest to largest. 

Merging is then done by writing out the smallest number in the heap of a Priority Queue and repeated until a stream no longer has anymore numbers before it is discarded. The resulting file is then announced to the server.


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
    Max acceptable bound for fault tolerance. 
    NumTasks * % Chance to Fail = Number of Servers that will fault.

In our testing we will be testing the above hypothesis with these scenarios.
F = Chance to fault.
N = number of servers.
T = number of tasks.
1. T * F  = 2N 
2. T * F = N
3. T * F = .1 N

With this we will figure out a acceptable 

# Unit Testing
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
