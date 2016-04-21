# MapReduce Implementation with Sort/Merge

Team Members:  	
- Danh Nguyen (nguy1952)
- Wen Chuan Lee (leex7095)


For a quick overview on how to run the project and test, please go to the **How to Run the Project section**, as well reading the **Client** section regarding the UI commands.

# Overall Design
	
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
