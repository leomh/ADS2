package ads2.cw1;

/**
 * Created by wim on 28/11/2017.
 */
class Status {
    private boolean read_write_; // read = true, write=false
    private boolean hit_; // hit = true, miss = false
    private boolean evicted_; // true if evicted, else false
    private boolean flushed_;
    private int memAddress_;
    private int data_;
    private int free_locs_;
    private int evicted_cache_loc_;
    private int evicted_cacheline_addr;

    String readOrWrite() { return (read_write_ ? "Read" : "Write"); }
    int memAddress() { return memAddress_; }
    int data() { return data_; }
    boolean evicted() { return evicted_; }
    boolean hit() { return hit_; }
    int freeLocations() {return free_locs_; }
    int evictedCacheLoc() { return evicted_cache_loc_; }
    int evictedCacheLineAddr() { return evicted_cacheline_addr; }
    boolean flushed() {return flushed_;}

    boolean setReadWrite(boolean c) { read_write_ = c; return read_write_; }
    int setAddress(int a) { memAddress_=a; return memAddress_; }
    int setData(int d) { data_=d; return data_; }
    boolean setEvicted(boolean e) { evicted_=e; return evicted_; }
    boolean setHitOrMiss(boolean e) { hit_=e; return hit_; }
    int setFreeLocations(int c) {free_locs_ = c ;return free_locs_; }
    int setEvictedCacheLoc(int e) { evicted_cache_loc_=e; return evicted_cache_loc_; }
    int setEvictedCacheLineAddr(int a) { evicted_cacheline_addr = a; return evicted_cacheline_addr; }
    boolean setFlushed(boolean b) {flushed_=b;return flushed_;}

    public String toString() {
        return "R/W: "+ readOrWrite()+" | Addr: "+memAddress()+" | Data: "+data()+" | Hit? "+hit()+" | "+" Eviction: "+evicted()+" | Free locs: "+freeLocations();
    }
}
