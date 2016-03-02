include "shared.thrift"

service Node {
	bool write(1: string filename 2: string contents),
    bool findMachine(1: string filename 2: list<i32> chain),
	string read(1: string filename),
	void updateDHT(1:list<shared.Machine> nodeList,2: list<i32> chain),
	i32 ping(),
}
