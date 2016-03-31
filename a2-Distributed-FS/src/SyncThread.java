class Sync extends Thread {
    HashMap<String,String> fs;
    List<Machine> servers;
    
    public Sync(List<Machine> servers) {
        fs = new HashMap<>();
        this.servers = servers;
    }

    @Override
    public void run() {
        while(true){
            Thread.sleep(2000);
            
            // Sync
                // Pause everything
                    // Ask Wenny
                    // Thanks wenny
                    
                  
                // build the global fs
                // loop through each machine and collect fs
                for(Machine m : servers) {
                	TTransport serverTransport = new TSocket(m.ipAddress, m.port);
                    serverTransport.open();
                    TProtocol serverProtocol = new TBinaryProtocol(new TFramedTransport(serverTransport));
                    Server.Client server  = new Server.Client(serverProtocol);
                    
                    // collects the files from servers
                    HashMap<String,Integer> serverFS = server.ls();
                    
                    // compare against current fs.
                    merge(m,serverFS);
                    serverTransport.close();
                }
                // Send a signal to all servers.
                
                for(Machine m : servers) {
                	TTransport serverTransport = new TSocket(m.ipAddress, m.port);
                    serverTransport.open();
                    TProtocol serverProtocol = new TBinaryProtocol(new TFramedTransport(serverTransport));
                    Server.Client server  = new Server.Client(serverProtocol);
                    
                    server.sync(fs);
                    serverTransport.close();
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
            
            if(fs.containsKey(filename)){
                fs.put(filename,syncEntry(machine,version));
            }else{
                // Compare the versions in the file systems
                String[] current = fs.get(filename).split("/");
                if(Integer.parseInt(current[2]) < version){
                    fs.put(filename,syncEntry(machine,version))
                }
            }
        }
    }
    
    private String syncEntry(Machine m,Integer version){
        return m.ipAddress+"/"+m.port+"/"+version;
    }
}