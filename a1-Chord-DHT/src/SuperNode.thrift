include "shared.thrift"

service SuperNode {
	list<shared.Machine> join(1: shared.Machine node),
	bool postJoin(1: shared.Machine node),
	shared.Machine getNode(),
}