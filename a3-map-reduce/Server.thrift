include "shared.thrift"

service Server {
    bool enroll(1: shared.Machine machine),
    bool compute(1: string filename,2: i32 chunks)
}
