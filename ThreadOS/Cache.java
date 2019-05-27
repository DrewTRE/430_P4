import java.util.*;
import java.io.*;

public class Cache { 
	private int 	newVictim;									
	private int 	blockSize;		
	private Entry[] pageTable;
    
    private class Entry { 
		int 		blockFrameNumber;
		boolean 	refBit;
		boolean 	dirtyBit;
		byte[] 		dataBlock;
	
		private Entry(int blockSize) {
			dataBlock 			= new byte[blockSize];
			blockFrameNumber 	= -1;	
        	refBit 				= false;	
        	dirtyBit 			= false;	
		}
    }

    private int findFreePage() {
		for (int i = 0; i < pageTable.length; i++) {	
            if (pageTable[i].blockFrameNumber == -1) {
				return i;	
			}
        }
        return -1;	
    }

	private int findBlock(int findBlockID) {
		for (int i = 0; i < pageTable.length; i++) {	
            if (pageTable[i].blockFrameNumber == findBlockID) {
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
			SysLib.rawwrite(pageTable[victimEntry].blockFrameNumber, pageTable[victimEntry].dataBlock);
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
		foundP = findBlock(blockId);				
		if(foundP != -1) { 	
			pageTable[foundP].blockFrameNumber 	= blockId;	
			System.arraycopy(pageTable[foundP].dataBlock, 0, buffer, 0, blockSize);	
			pageTable[foundP].refBit 			= true; 
			return true; 
		}
		foundP = findFreePage();					
		if(foundP != -1) { 	
			SysLib.rawread(blockId, pageTable[foundP].dataBlock); 
			pageTable[foundP].blockFrameNumber 	= blockId;				
			pageTable[foundP].refBit 			= true;	
			System.arraycopy(pageTable[foundP].dataBlock, 0, buffer, 0, blockSize);	
			return true;
		}
		writeBack(nextVictim());	
		SysLib.rawread(blockId, pageTable[newVictim].dataBlock);	     	
		pageTable[newVictim].blockFrameNumber 	= blockId;				
		pageTable[newVictim].refBit 			= true;
		System.arraycopy(pageTable[newVictim].dataBlock, 0, buffer, 0, blockSize);
		return true;
	}

    public synchronized boolean write(int blockId, byte buffer[]) 
	{
		if(blockId > 1000) {
			return false;
		}
		foundP = findBlock(blockId);					
		if(foundP != -1) { 	
			System.arraycopy(buffer, 0, pageTable[foundP].dataBlock, 0, blockSize);
			pageTable[foundP].blockFrameNumber 	= blockId;
			pageTable[foundP].dirtyBit 			= true;						
			pageTable[foundP].refBit 			= true;	
			return true; 
		}	

		foundP = findFreePage();					
		if(foundP != -1) { 	
			System.arraycopy(buffer, 0, pageTable[foundP].dataBlock, 0, blockSize);
			pageTable[foundP].blockFrameNumber 	= blockId;	
			pageTable[foundP].dirtyBit 			= true;						
			pageTable[foundP].refBit 			= true;	
			return true; 
		}
		writeBack(nextVictim());
		pageTable[newVictim].blockFrameNumber 	= blockId;
		pageTable[newVictim].dirtyBit 			= true;							
		pageTable[newVictim].refBit 			= true;
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
			pageTable[i].blockFrameNumber 	= -1;			
			pageTable[i].refBit 			= false;		
		}
		SysLib.sync();
    }
}