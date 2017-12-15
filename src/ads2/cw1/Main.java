package ads2.cw1;

import ads2.cw1.Memory;
import ads2.cw1.TestBench;

public class Main {

    public static void main(String[] args) {

        final int MEM_SZ=1024;
        final int CACHE_SZ=8*16;
        final int CACHELINE_SZ=16;

        Memory mem = new Memory(MEM_SZ,CACHE_SZ,CACHELINE_SZ);
        TestBench tb = new TestBench(mem);
        tb.run(4);

    }
}
