include "shared.thrift"

service Server {
    bool write(1: string filename 2: binary contents),
    binary read(1: string filename),
    bool enroll(1: shared.Machine machine),
    i32 getLatestVersion(1: string filename),
    bool update(1: string filename,2: i32 version,3: binary contents),
    binary directRead(1: string filename)
}
