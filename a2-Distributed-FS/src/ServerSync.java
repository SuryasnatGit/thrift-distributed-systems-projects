//Thrift imports
import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.*;
import org.apache.thrift.server.*;


import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.nio.ByteBuffer;
import java.net.InetAddress;
import java.util.ArrayList;

import java.util.Iterator;

class ServerSync extends Thread {
    HashMap<String,String> fs;
    List<Machine> servers;
    
    public ServerSync(List<Machine> servers) {
        fs = new HashMap<>();
        this.servers = servers;
    }

    @Override
    public void run(){
        while(true){
            try{
                Thread.sleep(3000);
            }catch(Exception e){
                e.printStackTrace();
            }
            
            if(servers.size() <= 1)
                continue;
             System.out.println("Syncing");
                // Pause everything
                    // Ask Wenny
                    // Thanks wenny
                    
                // build the global fs
                // loop through each machine and collect fs
                for(Machine m : servers) {
                    try{
                        TTransport serverTransport = new TSocket(m.ipAddress, m.port);
                        serverTransport.open();
                        TProtocol serverProtocol = new TBinaryProtocol(new TFramedTransport(serverTransport));
                        Server.Client server  = new Server.Client(serverProtocol);
                        
                        
                        // collects the files from servers
                        HashMap<String,Integer> serverFS = (HashMap<String,Integer>)server.ls();
                        
                        // compare against current fs.
                        merge(m,serverFS);
                        serverTransport.close();
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                
                // Send a signal to all servers.
                try{
                    TTransport serverTransport = new TSocket(m.ipAddress, m.port);
                    serverTransport.open();
                    TProtocol serverProtocol = new TBinaryProtocol(new TFramedTransport(serverTransport));
                    Server.Client server  = new Server.Client(serverProtocol);
                    // Sync servers.
                    server.sync(fs);
                    serverTransport.close();
                }catch(Exception e){
                    e.printStackTrace();
                }
             }
        }  
    }
    
    private void merge(Machine machine,HashMap<String,Integer> serverFS){
        // compare serverFS and fs
        Iterator it = serverFS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
	        Integer version = (Integer) pair.getValue();
            String filename = (String) pair.getKey();
            
            if(!fs.containsKey(filename)){
                fs.put(filename,syncEntry(machine,version));
            }else{
                // Compare the versions in the file systems
                String[] current = fs.get(filename).split("/");
                if(Integer.parseInt(current[2]) < version){
                    fs.put(filename,syncEntry(machine,version));
                }
            }
        }
    }
    
    private String syncEntry(Machine m,Integer version){
        return m.ipAddress+"/"+m.port+"/"+version;
    }
}