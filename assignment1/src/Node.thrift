
service NodeService{
	bool write(1: string ip,2: i16 port),
	string read(1: string filename),
	void updateDHT(1:list<node> nodeList)
}
