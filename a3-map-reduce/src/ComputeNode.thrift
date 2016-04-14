include "shared.thrift"

service ComputeNode {
    bool sort(1:string filename, 2: i32 startChunk, 3: i32 endChunk, 4: string ouput),
    bool merge(1:string f1, 2: string f2, 3: string output),
    bool heartbeat(),
    string getStats(),
    bool cancel(1: string ouput),
}
