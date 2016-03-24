include "shared.thrift"

service Server {
	bool write(1: string filename 2: string contents),
    string read(1: string filename),
    boolean enroll(1:Machine machine),
	i32 ping(),
}
