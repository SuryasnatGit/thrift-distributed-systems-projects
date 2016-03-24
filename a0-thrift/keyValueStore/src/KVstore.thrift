service KVStore{
    bool put(1: string key, 2: string value),
    string get(1: string key),
}