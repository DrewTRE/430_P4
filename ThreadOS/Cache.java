/*
Author: Drew Kwak
Date: 5/26/2019
Description: Cache program that implements a circular queue in order to utilize a second 
Second Chance algorithm to move data from Cache to Disk. 
*/

import java.util.*;
import java.io.*;

public class Cache { 
	// Data Members
	private int 	blockSize;	
	// Victim to get sacrificed. 
	private int 	newVictim;								
	// Cache Entries.	
	private Entry[] pageTable;
	 // Needed Enhanced Second Chance and is set with a victim. 
	private int 	foundPage; 
    
    private class Entry { 
    	// Data Members. 
		int 		frame;
		boolean 	refBit;
		boolean 	dirtyBit;
		byte[] 		dataBlock;
	
		private Entry(int blockSize) {
			dataBlock 	= new byte[blockSize];
			frame 		= -1;	
			// If a block has been accessed, set to true. 
        	refBit 		= false;	
        	// If there is a request to write, set to true. 
        	dirtyBit 	= false;	
		}
    }

    // Locates a free page in the pageTable. If it's free, return that index. 
    private int findFreePage() {
		for (int i = 0; i < pageTable.length; i++) {	
            if (pageTable[i].frame == -1) {
				return i;	
			}
        }
        return -1;	
    }

    // Locates a block frame number of the same ID on the pageTable. 
    // Returns index of the location. 
	private int findBlock(int findBlockID) {
		for (int i = 0; i < pageTable.length; i++) {	
            if (pageTable[i].frame == findBlockID) {
				return i;
			}
        }
        return -1;
    }

    // Loop through and keep looking for a victim with a reference bit set to 0. 
    // If it has a reference bit of 1, it's set to 0 and survives for that loop. 
	private int nextVictim() {
        while(true)	{
			newVictim = ((newVictim++) % pageTable.length); 
			if(pageTable[newVictim].refBit != true) {
				return newVictim;	
			}
			pageTable[newVictim].refBit = false;	
		}
    }

    // Check if victim has a dirty bit of 1. Write back to Disk if it does and set 
    // back to 0. 
    private void writeBack(int victimEntry) {
		if(pageTable[victimEntry].dirtyBit) {
			SysLib.rawwrite(pageTable[victimEntry].frame, pageTable[victimEntry].dataBlock);
			pageTable[victimEntry].dirtyBit = false; 
		}
    }

    // Cache constructor. 
    public Cache(int blockSize, int cacheBlocks) {
		this.blockSize 		= blockSize;	
		pageTable 			= new Entry[cacheBlocks];	
		for (int i = 0; i < pageTable.length; i++) {
			pageTable[i] 	= new Entry(this.blockSize); 
		}	
    }

  	// Read Method. 
    public synchronized boolean read(int blockId, byte buffer[]) {
		if(blockId > 1000) {
			return false;
		}
		foundPage = findBlock(blockId);	
		// If found. Search through array for given ID and copy items.		
		if(foundPage != -1) { 	
			pageTable[foundPage].frame 	= blockId;	
			System.arraycopy(pageTable[foundPage].dataBlock, 0, buffer, 0, blockSize);	
			pageTable[foundPage].refBit = true; 
			return true; 
		}
		foundPage = findFreePage();	
		// Not found. Search via rawread for frame and update. 				
		if(foundPage != -1) { 	
			SysLib.rawread(blockId, pageTable[foundPage].dataBlock); 
			pageTable[foundPage].frame 	= blockId;				
			pageTable[foundPage].refBit = true;	
			System.arraycopy(pageTable[foundPage].dataBlock, 0, buffer, 0, blockSize);	
			return true;
		}
		// Find new victim using second chance. If victim has a dirty bit of 1, write to disk and reset. 
		writeBack(nextVictim());	
		SysLib.rawread(blockId, pageTable[newVictim].dataBlock);	     	
		pageTable[newVictim].frame 	= blockId;				
		pageTable[newVictim].refBit = true;
		System.arraycopy(pageTable[newVictim].dataBlock, 0, buffer, 0, blockSize);
		return true;
	}

	// Write Method. 
    public synchronized boolean write(int blockId, byte buffer[]) {
		if(blockId > 1000) {
			return false;
		}
		// Look through array and set dirty and ref to 1. 
		foundPage = findBlock(blockId);					
		if(foundPage != -1) { 	
			System.arraycopy(buffer, 0, pageTable[foundPage].dataBlock, 0, blockSize);
			pageTable[foundPage].frame 		= blockId;
			pageTable[foundPage].dirtyBit 	= true;						
			pageTable[foundPage].refBit 	= true;	
			return true; 
		}	
		// Not found. Look for invalid element whose frame number is -1. 
		foundPage = findFreePage();					
		if(foundPage != -1) { 	
			System.arraycopy(buffer, 0, pageTable[foundPage].dataBlock, 0, blockSize);
			pageTable[foundPage].frame 		= blockId;	
			pageTable[foundPage].dirtyBit 	= true;						
			pageTable[foundPage].refBit 	= true;	
			return true; 
		}

		// Find new victim using Second Chance. 
		writeBack(nextVictim());
		pageTable[newVictim].frame 			= blockId;
		pageTable[newVictim].dirtyBit 		= true;							
		pageTable[newVictim].refBit 		= true;
		System.arraycopy(buffer, 0, pageTable[newVictim].dataBlock, 0, blockSize);
		return true;		
    }

    // Write back dirty blocks to disk. 
    public synchronized void sync() {
		for(int i = 0; i < pageTable.length; i++) {
			writeBack(i);
		}
		SysLib.sync();
    }

    // Write all dirty blocks to disk and reset everything to default. 
	public synchronized void flush() {
		for(int i = 0; i < pageTable.length; i++) { 	
			writeBack(i);
			pageTable[i].frame 	= -1;			
			pageTable[i].refBit = false;		
		}
		SysLib.sync();
    }
}