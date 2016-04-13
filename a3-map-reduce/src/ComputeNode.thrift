include "shared.thrift"

service ComputeNode {
    string sort(1:string filename, 2: i32 startChunk, 3: i32 endChunk, 4: string ouput),
    string merge(1:string f1, 2: string f2, 3: string output),
    bool heartbeat(),
    string getStats(),
    bool cancel(1: string ouput),
}
