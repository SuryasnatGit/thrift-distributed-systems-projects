include "shared.thrift"

service Server {
    bool write(1: string filename 2: binary contents),
    binary read(1: string filename),
    bool enroll(1: shared.Machine machine)
}
