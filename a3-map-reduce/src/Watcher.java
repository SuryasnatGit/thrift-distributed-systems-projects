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

class Watcher extends Thread {
    public final int HEARTBEAT = 5000;  //heartbeat frequency in miliseconds 20 seconds * 1000 = 20 000 ms
    List<Machine> nodes;
    
    public Watcher(List<Machine> nodes) {
        this.nodes = nodes;
    }

    @Override
    public void run(){
        while(true){
            try{
                Thread.sleep(HEARTBEAT);
                
                synchronized(nodes) {
                    for(Machine m : nodes) {
                        TTransport nodeTransport = new TSocket(m.ipAddress, m.port);
                        nodeTransport.open();
                        TProtocol nodeProtocol = new TBinaryProtocol(new TFramedTransport(nodeTransport));
                        ComputeNode.Client node  = new ComputeNode.Client(nodeProtocol);
                        
                        try{
                            node.heartbeat();
                        }catch(TException e){
                           // do something
                        }
                    }
                                
                }
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}
