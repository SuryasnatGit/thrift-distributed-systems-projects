include "shared.thrift"

service SuperNode {
	list<shared.Machine> Join(1: shared.Machine node),
	bool PostJoin(1: shared.Machine node),
	shared.Machine getNode(),
}