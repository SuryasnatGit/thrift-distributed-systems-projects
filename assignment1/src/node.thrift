// I dunno whats in this
struct node{
	1: string ip,
	2: i16 port,
}



service NodeService{
	bool write(1: string ip,2: int port),
	string read(1: string filename),
	void updateDHT(1:list<node>)
}
