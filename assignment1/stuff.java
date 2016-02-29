	public interface SuperNodeInterface{
		
			List<Node> NodesList;
			
			SuperNodeInterface(){
				// Build the dht table
			}
			
			List<Node>	Join(String IP,int Port){
				// Return list of nodes
				// If busy return "NACK"
			 // lol wtf
			}
			
			
			PostJoin(String IP,int Port){
			
			}
			
			
			Node GetNode(){
			// get a random node information
			}
			
		
		}
		
		
		
		public interface Node{
			void Write(String Filename,String Contents){
				
			}
			
			String Read(String Filename){
				
			}
			
			void UpdateDHT(List<Node> NodesList){
				
			}
		}
		
		
		
		
		public SuperNode implements SuperNodeInterface{
			
			List<Node> NodesList;
			
			
			SuperNode(){
				// Alright lets init this DHT Table
				// 1. Sequentially add nodes.
				for(int i =0; i<5; i++){
					Node node = new Node();
					Join()
				}
			}
			
			List<Nodes>	Join(String IP,int Port){
		
			}
		
		
			void PostJoin(String IP,int Port){
			
			}
			
			
			Node	GetNode(){
			}
		}
		
		
		
		public Client{
			
			public static main(String[] args){
				Client app = new Client();
				
				// Get some commands.
				
				// Execute those commands.
				
			}
			
			
			List<String> getFiles(){
				
			}
			
			String read(String filename){
				
			}
			
			void write(String filename,String contents){
				
			}
			
			
		}
