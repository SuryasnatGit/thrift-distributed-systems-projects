include "shared.thrift"

service ComputeNode {
    bool sort(1:string filename, 2: i64 startChunk, 3: i64 endChunk, 4: string output),
    bool merge(1:list<string> filenames, 2: string output),
    bool heartbeat(),
    string getStats(),
    bool cancel(1: string ouput),
}
