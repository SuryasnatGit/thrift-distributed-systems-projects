include "shared.thrift"

service NodeService{
	bool write(1: shared.Machine machine),
	string read(1: string filename),
	void updateDHT(1:list<shared.Machine> nodeList)
}
