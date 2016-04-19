include "shared.thrift"

service Server {
    bool enroll(1: shared.Machine machine),
    string compute(1: string filename, 2: i32 chunks),
    bool announce(1: shared.Machine m, 2: string task_output)
}
