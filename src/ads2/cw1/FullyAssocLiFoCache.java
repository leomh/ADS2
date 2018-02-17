package ads2.cw1;

/**
 * Created by wim on 28/11/2017.
 * Modified by 2264897V
 * The public interface of this class is provided by Cache
 * All other methods are private. 
 * You must implement/complete all these methods
 * You are allow to create helper methods to do this, put them at the end of the class 
 */

import ads2.cw1.Cache;

import java.util.Stack;
import java.util.HashMap;

class FullyAssocLiFoCache implements Cache {

	final private static boolean VERBOSE = false;

	final private int CACHE_SZ;
	final private int CACHELINE_SZ;
	final private int CL_MASK;
	final private int CL_SHIFT;

	// WV: because the cache replacement policy is "Last In First Out" you only need
	// to know the "Last Used" location
	// "Last Used" means accessed for either read or write
	// The helper functions below contain all needed assignments to last_used_loc so
	// I recommend you use these.

	// data structure for the last used location in the cache
	private int last_used_loc;
	// data structure for the cache storage
	private int[] cache_storage;
	// data structure to manage free locations in the cache
	private Stack<Integer> location_stack;
	// And because the cache is Fully Associative:
	// data structure to translate between memory addresses and cache locations
	private HashMap<Integer, Integer> address_to_cache_loc;
	// data structure to translate between cache locations and memory addresses
	private HashMap<Integer, Integer> cache_loc_to_address;

	FullyAssocLiFoCache(int cacheSize, int cacheLineSize) {

		CACHE_SZ = cacheSize;
		CACHELINE_SZ = cacheLineSize;
		CL_MASK = CACHELINE_SZ - 1;
		Double cls = Math.log(CACHELINE_SZ) / Math.log(2);
		CL_SHIFT = cls.intValue();

		last_used_loc = CACHE_SZ / CACHELINE_SZ - 1;
		// WV: Your initialisations here
		cache_storage = new int[cacheSize];
		location_stack = new Stack<>();
	}

	public void flush(int[] ram, Status status) {
		if (VERBOSE)
			System.out.println("Flushing cache");
		// WV: Your other data structures here
		// for all in cache, write to memory
        for(Integer cache_address:cache_loc_to_address.keySet()) {
	        	int ram_address = cache_loc_to_address.get(cache_address);
	        	for(int i=0;i<CACHELINE_SZ;i++) {
	        		ram[ram_address+i] = cache_storage[cache_address + i];
	        	}
	        	// push newly created free location address onto location stack
	        	location_stack.push(cache_address);
        }
        cache_storage = new int[CACHELINE_SZ];
        cache_loc_to_address.clear();
        address_to_cache_loc.clear();
        last_used_loc = 0;
        status.setFreeLocations(location_stack.size());
        status.setFlushed(true);
	}

	public int read(int address, int[] ram, Status status) {
		return read_data_from_cache(ram, address, status);
	}

	public void write(int address, int data, int[] ram, Status status) {
		write_data_to_cache(ram, address, data, status);
	}

	// The next two methods are the most important ones as they implement read() and
	// write()
	// Both methods modify the status object that is provided as argument

	private void write_data_to_cache(int[] ram, int address, int data, Status status) {
		status.setReadWrite(false); // i.e. a write
		status.setAddress(address);
		status.setData(data);
		status.setEvicted(false);
		
		// The cache policy is write-back, so the writes are always to the cache.
		// The update policy is write allocate: on a write miss, a cache line is loaded
		// to cache, followed by a write operation.
		int loc;
		
		// check if address is already in the cache
		if (address_in_cache_line(address)) {
			update_cache_entry(address, data);
			loc = address_to_cache_loc.get(address);
			status.setHitOrMiss(true);
		// is the cache full? if yes, evict a cache line
		} else if (cache_is_full()) {
			loc = last_used_loc;
			write_to_mem_on_evict(ram, loc);
			status.setEvicted(true);
			status.setEvictedCacheLoc(loc);
			status.setEvictedCacheLineAddr(cache_loc_to_address.get(loc));
			// and now write in that location
			cache_storage[loc] = data;
		// if not, get free location and write data there
		} else {
			loc = get_next_free_location();
			cache_storage[loc] = data;
		}
		
		// update last_used_loc
		last_used_loc = loc;
		// update look up tables
		address_to_cache_loc.put(address, loc);
		cache_loc_to_address.put(loc, address);
		status.setFreeLocations(location_stack.size());

	}

	private int read_data_from_cache(int[] ram, int address, Status status) {
		status.setReadWrite(true); // i.e. a read
		status.setAddress(address);
		status.setEvicted(false);
		status.setHitOrMiss(true); // i.e. a hit
		
		int data;
		
		// Reads are always to the cache. On a read miss you need to fetch a cache line
		// from the DRAM
		// If the data is not yet in the cache (read miss),fetch it from the DRAM
		// Get the data from the cache
		
		// get cache data location for given address
		// if it does not exist, get it from dram and write it to cache
		if (!address_in_cache_line(address)) {
			status.setHitOrMiss(false);
			read_from_mem_on_miss(ram, address);
		} 
			
		data = fetch_cache_entry(address);

		status.setData(data);
		status.setFreeLocations(location_stack.size());
		last_used_loc = cache_line_address(address);
		return data;
	}

	//        ///          //
	//    HELPER METHODS  
	//        ///          //
	
	// On read miss, fetch a cache line
	private void read_from_mem_on_miss(int[] ram, int address) {
		if (VERBOSE)
			System.out.println("Cache read miss, fetching data from memory");
		int[] cache_line = new int[CACHELINE_SZ];

		if (cache_is_full()) {
			if (VERBOSE)
				System.out.println("Cache is full, evicting last cache line: " + last_used_loc);
			write_to_mem_on_evict(ram, last_used_loc);
		}

		int loc = get_next_free_location();
		// UNSURE IF NECESSARY: get cache line address given the main memory address
		int cl_address = cache_line_address(address);

		for (int i = 0; i < CACHELINE_SZ; i++) {
			// load data into cache line
			cache_line[i] = ram[cl_address + i];
		}

		// update look up tables
		address_to_cache_loc.put(cl_address, loc);
		cache_loc_to_address.put(loc, address);

		// load cache with the cache line
		for (int i = 0; i < CACHELINE_SZ; i++) {
			cache_storage[loc + i] = cache_line[i];
		}

		// update last used location as cache location
		last_used_loc = loc;
	}

	// On write, modify a cache line
	private void update_cache_entry(int address, int data) {
		// get the location of the current data in the cache for the given memory
		// address
		int loc = address_to_cache_loc.get(address);
		// very unsure about this
		cache_storage[loc] = data;

		last_used_loc = loc;
	}

	// fetch a cache entry
	// and then update the last used location
	private int fetch_cache_entry(int address) {
		if (VERBOSE)
			System.out.println("Fetching data from cache");
		int[] cache_line = new int[CACHELINE_SZ];
		int loc = address_to_cache_loc.get(address);

		for (int i = 0; i < CACHELINE_SZ; i++) {
			cache_line[i] = cache_storage[loc + i];
		}

		last_used_loc = loc;
		return cache_line[cache_line_address(address)];
	}

	// Should return the next free location in the cache
	// otherwise if cache is full returns -1
	private int get_next_free_location() {
		if (!cache_is_full()) {
			return location_stack.pop();
		} else {
			return -1;
		}
	}

	// Given a cache location, evict the cache line stored there
	private void evict_location(int loc) {
		// set the location as free location in the stack
		if (VERBOSE)
			System.out.println("Push " + Integer.toString(loc) + " onto the free locations stack");
		location_stack.push(address_to_cache_loc.get(loc));
		cache_loc_to_address.remove(location_stack.peek());
		address_to_cache_loc.remove(loc);
	}

	private boolean cache_is_full() {
		return location_stack.empty();
	}

	// When evicting a cache line, write its contents back to main memory
	private void write_to_mem_on_evict(int[] ram, int loc) {

		int evicted_cl_address = cache_line_start_mem_address(loc);
		int[] cache_line = new int[CACHELINE_SZ];
		if (VERBOSE)
			System.out.println("Cache line to RAM: ");

		// write data to retrieved memory address
		for (int i = 0; i < CACHELINE_SZ; i++) {
			cache_line[i] = cache_storage[loc + i];
			// Evict the sequence of addresses to RAM
			ram[evicted_cl_address + i] = cache_line[i];
		}

		// update free location stack
		evict_location(loc);
	}

	// Is the value for this memory address stored in the cache?
	private boolean address_in_cache_line(int address) {
		boolean inCache = address_to_cache_loc.containsKey(cache_line_address(address));
		if (VERBOSE)
			System.out.println("Looking for " + Integer.toString(address) + " in cache");
		return inCache;
	}

	// Given a main memory address, return the corresponding cache line address
	private int cache_line_address(int address) {
		return address >> CL_SHIFT;
	}

	// Given a main memory address, return the corresponding index into the cache
	// line
	private int cache_entry_position(int address) {
		return address & CL_MASK;
	}

	// Given a cache line address, return the corresponding main memory address
	// This is the starting address of the cache line in main memory
	private int cache_line_start_mem_address(int cl_address) {
		return cl_address << CL_SHIFT;
	}

}
