class WriteResponse extends Response {
    
    public WriteResponse(Machine origin, Boolean status){
        this.type = "write";
        this.origin = origin;
        this.status = status;
    }
}
