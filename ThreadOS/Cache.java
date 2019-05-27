import java.util.*;
import java.io.*;

public class Cache { 
	private int 	newVictim;									
	private int 	blockSize;		
	private Entry[] pageTable;
	private int 	foundPage; 
    
    private class Entry { 
		int 		frame;
		boolean 	refBit;
		boolean 	dirtyBit;
		byte[] 		dataBlock;
	
		private Entry(int blockSize) {
			dataBlock 	= new byte[blockSize];
			frame 		= -1;	
        	refBit 		= false;	
        	dirtyBit 	= false;	
		}
    }

    private int findFreePage() {
		for (int i = 0; i < pageTable.length; i++) {	
            if (pageTable[i].frame == -1) {
				return i;	
			}
        }
        return -1;	
    }

	private int findBlock(int findBlockID) {
		for (int i = 0; i < pageTable.length; i++) {	
            if (pageTable[i].frame == findBlockID) {
				return i;
			}
        }
        return -1;
    }

	private int nextVictim() {
        while(true)	{
			newVictim = ((newVictim++) % pageTable.length); 
			if(pageTable[newVictim].refBit != true) {
				return newVictim;	
			}
			pageTable[newVictim].refBit = false;	
		}
    }

    private void writeBack(int victimEntry) {
		if(pageTable[victimEntry].dirtyBit) {
			SysLib.rawwrite(pageTable[victimEntry].frame, pageTable[victimEntry].dataBlock);
			pageTable[victimEntry].dirtyBit = false; 
		}
    }

    public Cache(int blockSize, int cacheBlocks) {
		this.blockSize 		= blockSize;	
		pageTable 			= new Entry[cacheBlocks];	
		for (int i = 0; i < pageTable.length; i++) {
			pageTable[i] 	= new Entry(this.blockSize); 
		}	
    }

    public synchronized boolean read(int blockId, byte buffer[]) {
		if(blockId > 1000) {
			return false;
		}
		foundPage = findBlock(blockId);				
		if(foundPage != -1) { 	
			pageTable[foundPage].frame 	= blockId;	
			System.arraycopy(pageTable[foundPage].dataBlock, 0, buffer, 0, blockSize);	
			pageTable[foundPage].refBit 			= true; 
			return true; 
		}
		foundPage = findFreePage();					
		if(foundPage != -1) { 	
			SysLib.rawread(blockId, pageTable[foundPage].dataBlock); 
			pageTable[foundPage].frame 	= blockId;				
			pageTable[foundPage].refBit 			= true;	
			System.arraycopy(pageTable[foundPage].dataBlock, 0, buffer, 0, blockSize);	
			return true;
		}
		writeBack(nextVictim());	
		SysLib.rawread(blockId, pageTable[newVictim].dataBlock);	     	
		pageTable[newVictim].frame 	= blockId;				
		pageTable[newVictim].refBit = true;
		System.arraycopy(pageTable[newVictim].dataBlock, 0, buffer, 0, blockSize);
		return true;
	}

    public synchronized boolean write(int blockId, byte buffer[]) 
	{
		if(blockId > 1000) {
			return false;
		}
		foundPage = findBlock(blockId);					
		if(foundPage != -1) { 	
			System.arraycopy(buffer, 0, pageTable[foundPage].dataBlock, 0, blockSize);
			pageTable[foundPage].frame 		= blockId;
			pageTable[foundPage].dirtyBit 	= true;						
			pageTable[foundPage].refBit 	= true;	
			return true; 
		}	

		foundPage = findFreePage();					
		if(foundPage != -1) { 	
			System.arraycopy(buffer, 0, pageTable[foundPage].dataBlock, 0, blockSize);
			pageTable[foundPage].frame 		= blockId;	
			pageTable[foundPage].dirtyBit 	= true;						
			pageTable[foundPage].refBit 	= true;	
			return true; 
		}
		writeBack(nextVictim());
		pageTable[newVictim].frame 			= blockId;
		pageTable[newVictim].dirtyBit 		= true;							
		pageTable[newVictim].refBit 		= true;
		System.arraycopy(buffer, 0, pageTable[newVictim].dataBlock, 0, blockSize);
		return true;		
    }

    public synchronized void sync() {
		for(int i = 0; i < pageTable.length; i++) {
			writeBack(i);
		}
		SysLib.sync();
    }

	public synchronized void flush() {
		for(int i = 0; i < pageTable.length; i++) { 	
			writeBack(i);
			pageTable[i].frame 	= -1;			
			pageTable[i].refBit = false;		
		}
		SysLib.sync();
    }
}