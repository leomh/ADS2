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
import java.util.Arrays;
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
	private int[] cache_storage;
	// data structure to manage free locations in the cache
	private Stack<Integer> location_stack;
	// And because the cache is Fully Associative:
	// data structure to translate between memory addresses and cache locations (relative to relative)
	private HashMap<Integer, Integer> address_to_cache_loc;
	// data structure to translate between cache locations and memory addresses (relative to absolute)
	private HashMap<Integer, Integer> cache_loc_to_address;

	FullyAssocLiFoCache(int cacheSize, int cacheLineSize) {

		CACHE_SZ = cacheSize;
		CACHELINE_SZ = cacheLineSize;
		CL_MASK = CACHELINE_SZ - 1;
		Double cls = Math.log(CACHELINE_SZ) / Math.log(2);
		CL_SHIFT = cls.intValue();

		last_used_loc = CACHE_SZ / CACHELINE_SZ - 1;
		// WV: Your initialisations here
		cache_storage = new int[CACHE_SZ];
		location_stack = new Stack<Integer>();
		for (int i=(CACHE_SZ/CACHELINE_SZ)-1;i>-1;i--) {
        		location_stack.push(i);
        }
		
        address_to_cache_loc = new HashMap<Integer, Integer>();
		cache_loc_to_address = new HashMap<Integer, Integer>();
	}

	public void flush(int[] ram, Status status) {
		if (VERBOSE) System.out.println("Flushing cache");
		
		// for all in cache, write to memory
        for(Integer cache_address:cache_loc_to_address.keySet()) {
	        	int ram_address = cache_loc_to_address.get(cache_address);
	        	for(int i=0;i<CACHELINE_SZ;i++) {
	        		ram[ram_address+i] = cache_storage[cache_address + i];
	        	}
	        	// push newly created free location address onto location stack
	        	// acts as emptying cache
	        	location_stack.push(cache_address);
        }
        // new cache for convention
        cache_storage = new int[CACHE_SZ]; 
        cache_loc_to_address.clear();
        address_to_cache_loc.clear();
        last_used_loc = CACHE_SZ / CACHELINE_SZ - 1;
        status.setFreeLocations(location_stack.size());
        status.setFlushed(true);
	}

	public int read(int address, int[] ram, Status status) {
		return read_data_from_cache(ram, address, status);
	}

	public void write(int address, int data, int[] ram, Status status) {
		write_data_to_cache(ram, address, data, status);
	}

	private void write_data_to_cache(int[] ram, int address, int data, Status status) {
		status.setReadWrite(false); // i.e. a write
		status.setAddress(address);
		status.setData(data);
		status.setEvicted(false);
		
		// The cache policy is write-back, so the writes are always to the cache.
		// The update policy is write allocate: on a write miss, a cache line is loaded
		// to cache, followed by a write operation.
		
		if (VERBOSE) System.out.println("Writing data " + data + " into cache at address " + address);
		
		// if it's a hit
		if (address_in_cache_line(address)) {
			status.setHitOrMiss(true);
		// else it's a miss: load a cache line from memory
		// to update it afterwards
		} else {
			status.setHitOrMiss(false);
			read_from_mem_on_miss(ram, address, status);
		}
		
		// in both cases, there is a cache_line at the right address
		// that we can update
		update_cache_entry(address, data);
		// look up tables are updated in read_from_mem_miss
		// otherwise the mapping already exists as we're only updating
		status.setFreeLocations(location_stack.size());
		last_used_loc = address_to_cache_loc.get(cache_line_address(address));
	}

	private int read_data_from_cache(int[] ram, int address, Status status) {
		status.setReadWrite(true); // i.e. a read
		status.setAddress(address);
		status.setEvicted(false);
		status.setHitOrMiss(true); // i.e. a hit
		
		// if data is not already in cache, get it from DRAM and write it to cache
		if (!address_in_cache_line(address)) {
			status.setHitOrMiss(false);
			read_from_mem_on_miss(ram, address, status);
		} 
			
		// in both cases, fetches data from location
		int data = fetch_cache_entry(address); 

		status.setData(data);
		status.setFreeLocations(location_stack.size());
		last_used_loc = address_to_cache_loc.get(cache_line_address(address));
		return data;
	}

	//        ///          //
	//    HELPER METHODS  
	//        ///          //
	
	// On read miss, fetch the necessary cache line and store it in cache
	private void read_from_mem_on_miss(int[] ram, int address, Status status) {
		if (VERBOSE) System.out.println("Cache read miss, fetching data from memory");
		int cl_address = cache_line_address(address);
		int loc;
		
		if (cache_is_full()) {
			loc = last_used_loc;
			if (VERBOSE) System.out.println("Cache is full, evicting last cache line: " + loc);
			status.setEvicted(true);
			status.setEvictedCacheLoc(loc);
			status.setEvictedCacheLineAddr(cache_line_address(cache_loc_to_address.get(loc)));
			write_to_mem_on_evict(ram, loc);
		} 
		
		loc = get_next_free_location();
		if (VERBOSE) System.out.println("Loading the new cache line in the cache...");
		for (int i = 0; i < CACHELINE_SZ; i++) {
			cache_storage[loc*CACHELINE_SZ + i] = ram[cl_address*CACHELINE_SZ + i];
		}
		
		// update look up tables (add cache line to cache)
		address_to_cache_loc.put(cl_address, loc);
		cache_loc_to_address.put(loc, address);

		// update last used location as cache location
		last_used_loc = loc;
	}

	// On write, modify a cache line
	private void update_cache_entry(int address, int data) {
		// get the location of the current data in the cache 
		// for the given memory address
		int loc = address_to_cache_loc.get(cache_line_address(address));
		if (VERBOSE) System.out.println("Updating cache entry in cache line " + loc + " for data at address " + address);
		
		cache_storage[loc*CACHELINE_SZ+cache_entry_position(address)] = data;
		
		last_used_loc = loc;
	}

	// fetch a cache entry for a given address
	// and then update the last used location
	private int fetch_cache_entry(int address) {
		int[] cache_line = new int[CACHELINE_SZ];
		int loc = address_to_cache_loc.get(cache_line_address(address));
		if (VERBOSE) System.out.println("Fetching data from cache in cache line " + loc);

		for (int i = 0; i < CACHELINE_SZ; i++) {
			cache_line[i] = cache_storage[loc*CACHELINE_SZ + i];
		}

		last_used_loc = loc;
		return cache_line[cache_entry_position(address)];
	}

	// Should return the next free location in the cache
	// otherwise if cache is full returns -1
	private int get_next_free_location() {
		if (!cache_is_full()) {
			return location_stack.pop();
		} else {
			if (VERBOSE) System.out.println("Cache is full! Probably shouldn't have gone into this method without checking");
			return -1;
		}
	}

	// Given a cache location, evict the cache line stored there
	private void evict_location(int loc) {
		// set the location as free location in the stack
		location_stack.push(loc);
		address_to_cache_loc.remove(cache_line_address(cache_loc_to_address.get(loc)));
		cache_loc_to_address.remove(loc);
	}

	private boolean cache_is_full() {
		return location_stack.empty();
	}

	// When evicting a cache line, write its contents back to main memory
	private void write_to_mem_on_evict(int[] ram, int loc) {
		
		// gets relative address of cache line in memory (* by CACHELINE_SZ later)
		int evicted_cl_address = cache_line_address(cache_loc_to_address.get(loc));
	
		if (VERBOSE) System.out.println("Evicting..." + loc);
		if (VERBOSE) System.out.println("Cache line to RAM: ");
		
		// write data to retrieved memory address
		for (int i = 0; i < CACHELINE_SZ; i++) {
			// Evict the sequence of addresses to RAM
			ram[evicted_cl_address*CACHELINE_SZ + i] = cache_storage[loc*CACHELINE_SZ + i];
		}
		
		// update free location stack and mappings
		evict_location(loc);
	}

	// Is the value for this memory address stored in the cache?
	private boolean address_in_cache_line(int address) {
		boolean inCache = address_to_cache_loc.containsKey(cache_line_address(address));
		if (VERBOSE) System.out.println("Looking for " + Integer.toString(address) + " in cache: " + address_to_cache_loc.containsKey(cache_line_address(address)));
		return inCache;
	}

	//               ///                      //
	// Going about cache lines in main memory //
	//               ///                      //
	
	// Given a main memory address, return the corresponding cache line address
	// e.g. for address 8 and CACHE_SZ 4 this is cache line 2
	// absolute to relative
	private int cache_line_address(int address) {
		return address >> CL_SHIFT;
	}

	// Given a main memory address, return the corresponding index into the cache line
	// e.g. for address 12 and CACHE_SZ 4 this is position 0 into cache line 4
	private int cache_entry_position(int address) {
		return address & CL_MASK;
	}

	// Given a cache line address, return the corresponding main memory address
	// This is the starting address of the cache line in main memory
	// e.g. for cache_line number 2 with CACHE_SZ 4 this is index 7
	private int cache_line_start_mem_address(int cl_address) {
		return cl_address << CL_SHIFT;
	}

}
