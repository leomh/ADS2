package ads2.cw1;

/**
 * Created by wim on 28/11/2017.
 */

interface Cache {

    int read(int address,int[] ram,Status status);
    void write(int address,int data, int[] ram,Status status);
    void flush(int[] ram,Status status);

//    Cache(int cacheSize, int cacheLineSize);

}
