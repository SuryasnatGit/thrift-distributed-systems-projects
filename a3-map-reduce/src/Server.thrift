include "shared.thrift"

service Server {
    bool enroll(1: shared.Machine machine),
    string compute(1: string filename, 2: i32 chunks, 3: i32 num_merge),
    bool announce(1: shared.Machine m, 2: string task_output)
}
