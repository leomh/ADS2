package ads2.cw1;

/**
 * Created by wim on 28/11/2017.
 */

import ads2.cw1.Cache;

class Memory {
    protected int[] ram;
    private Cache cache;

    final private int MEM_SZ;
    final private int CACHE_SZ;
    final private int CACHELINE_SZ;

    protected Status status;

    int read(int address) {
        return cache.read(address,ram,status);
    }
    void write(int address,int data) {
        cache.write(address,data,ram,status);
    }
    void flush() {
        cache.flush(ram,status);
    }

    Status getStatus() {
        return status;
    }

    int peek(int address) {
        return ram[address];
    }

    int getMemSize() {
        return MEM_SZ;
    }

    int getCacheSize() {
        return CACHE_SZ;
    }

    int getCacheLineSize() {
        return CACHELINE_SZ;
    }

    Memory(int memSize,int cacheSize, int cacheLineSize) {
        MEM_SZ = memSize;
        CACHE_SZ = cacheSize;
        CACHELINE_SZ = cacheLineSize;

        ram = new int[memSize];
        status = new Status();
        cache = new FullyAssocLiFoCache(cacheSize, cacheLineSize);

    }


}
