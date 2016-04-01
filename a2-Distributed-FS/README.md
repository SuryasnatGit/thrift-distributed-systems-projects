# Quorum Based Replicated File Server

Team Members:  	
- Danh Nguyen (nguy1952)
- Wen Chuan Lee (leex7095)


For a quick overview on how to run the project as as well test, please go to the **How to Run the Project section**, as well reading the **Client** section regarding the UI commands.

# Overall Design

# The Coordinator

# Servers

# Client

The Client is a terminal to the File Server. 

The Client will establish a connection to any server.

If the Client is successful, an simple interactive terminal asking for user input is then launched.

If the Client is unsuccessful, the Client will go to sleep for one second before retrying indefinitely.

The terminal contains a few simple commands to interact with the File Server.

 - `read <filename>` - requests a file which will be the most updated file from the File Server.
 - `write <filename>` - reads a file stored in `uploads` and uploads it to the File Server and replicating it across NW machines.
 - `ls` - lists all files in the `uploads` directory
 - `load-test` - performs a batch upload operation to put all files stored in `uploads` into the File Server.
 - `stats` - Lists the stats of the session. This displays the read and write of all requests.
 - `exit` - closes the connection to the Node and quits the interactive terminal


# How to Run the Project

The File Server was built with the help of Ant. A prerequisite to run this project requires Ant. Additionally, the generated thrift files were also kept in a version control, but the `Thrift` compiler is also needed if Thrift files need to be generated (else only the Java Thrift library is needed).

Ant was used to handle automatic building and generation of class files (into a separate `bin` directory) as well as creating short targets for rapid developement.

### *To start the Coordinator, Servers, and Client on localhost*

(This starts all processes on one terminal using forked processes and will cause all processes to print to the same `System.out`)

    ant start-all
### *To start the File Server across multiple machines*
    1. ssh username@x32-05.cselabs.umn.edu
    2. cd into project directory
    3. ant start-coordinator
    4. ssh username@x32-XX.cselabs.umn.edu (open another terminal)
    5. cd into project directory
    6. ant start-server
    7. Repeat steps 4 - 6 for the other Nodes
    8. ssh username@x32-XX.cselabs.umn.edu (or open another terminal)
    9. cd into project directory
    10. ant start-client
    
The servers connect to the coordinator to enroll into the File Server located at `x32-05` on port `9090`, **to override this, on Step 6 do**:

    ant -Dcoordinator.address='x32-XX' -Dcoordinator.port='XXXX' start-server

And on **Step 10 do** to change which server to connect to:

    ant -Dserver.address='x32-XX' -DsuperNode.port='XXXX' start-client


**Please see Ant Targets and Overriding Properties for more information.**

## Ant Targets

The fastest way to test out the entire DHT is to create 1 SuperNode and multiple Nodes on the same machine (but with a different port). Multiple targets have been provided for automating the process of starting everything. 

Note: Run commands from the project directory, where `build.xml` is localed.

  - `ant start-all`
      - Create 1 Coordinator (port 9090) and 10 Nodes and a Client on the same machine.  
      - All processes run in their own Java VM and as a forked process.
      - Client waits for 4 seconds before deciding to joining the cluster.
      - Note: see Ant Properties on how to start targets with different values.

- `ant start-coordinator`
  - Starts the coordinator on the current machine on the port specified by `coordinator.port`

- `ant start-server`
  - Starts a node that will connect to the coordinator located at `coordinator.address` and `coordinator.port`.

- `ant start client`
  - Starts the client that connects to a server specified under `server.address` and `server.port`
  - *After the client has joined, no new nodes should be allowed to join the DHT.*

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

##### Examples:

# Test Cases

## Positive Scenarios

## Negative Scenarios


