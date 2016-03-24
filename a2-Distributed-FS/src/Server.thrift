include "shared.thrift"

service Server {
	bool write(1: string filename 2: string contents),
    	string read(1: string filename),
	i32 ping(),
}
