
import java.util.*;
import java.io.*;
import java.util.Date;
import java.util.Random;

public class Test4 extends Thread {
	private int cachedBlocks 		= 10;	
	private int arrayTest 			= 200;	
	private int diskBlockSize 		= 512;	
	private int testing;
	private String status 			= "disabled";
	private boolean cacheEnabled 	= false;	
	private byte[] readBuffer;	
	private byte[] writeBuffer;	
	private Random random;

	private long startWriteTime;                           
    private long stopWriteTime;                            
    private long startReadTime;                            
    private long stopReadTime;  

	public Test4(String[] args) {
		writeBuffer = new byte[diskBlockSize];               
        readBuffer 	= new byte[diskBlockSize]; 
		random 		= new Random();

		if(args[0].equals("enabled")) {
			cacheEnabled  	= true; 
			status 			= "enabled";	
		} else {
			cacheEnabled  	= false;	 
			status 			= "disabled"; 
		}

        testing = Integer.parseInt(args[1]);
        
    }

	public void run( ) {
    	SysLib.flush();							
    	switch(testing) {                               
    		case 1:	{
				randomAccess(); 	
				break;        
			}
    		case 2: {
				localizedAccess();	
				break;        
			}
			case 3: {
				mixedAccess(); 	
				break;        
			}
    		case 4: {
				adversaryAccess(); 	
				break;        
			}	      			  			  			  			  			
    	}
		
		if(cacheEnabled) {
			SysLib.csync();		
		} else {
			SysLib.sync();
		}
		SysLib.exit( );                               
    }

	private void reader(int blockId, byte[] buffer) {
		if (cacheEnabled) {
			SysLib.cread(blockId, buffer);
		} else {
			SysLib.rawread(blockId, buffer);
		}
	}
	
	private void writer(int blockId, byte[] buffer) {
		if (cacheEnabled) {
			SysLib.cwrite(blockId, buffer);
		} else {
			SysLib.rawwrite(blockId, buffer);
		}
	}

	private void randomAccess() {
		random.nextBytes(writeBuffer); 
		int[] randomAccessArr = new int[arrayTest];
		
		for(int i = 0; i < arrayTest; i++) {
			randomAccessArr[i] = Math.abs(random.nextInt() % diskBlockSize);
		}
		
		startWriteTime = new Date().getTime();	

		for (int i = 0; i < arrayTest; i++) {
			writer(randomAccessArr[i], writeBuffer); 
		}

		stopWriteTime = new Date().getTime(); 
		startReadTime = new Date().getTime();	

		for(int i = 0; i < arrayTest; i++) {	
			reader(randomAccessArr[i], readBuffer);
		}
		
		stopReadTime = new Date().getTime();	

		if(!(Arrays.equals(writeBuffer, readBuffer))) {
			 SysLib.cout("DISK VALIDITY ERROR: writerBytes and readBuffer equal\n");
		}	
		SysLib.cout("Testing randomAccess(), using Test :" + testing + "\n");
		SysLib.cout("Cache: [" + status + "] \n");
		SysLib.cout("Avg time for Write : " + ((stopWriteTime - startWriteTime) / 200) + " ms \n");
		SysLib.cout("Avg time for Read  : " + ((stopReadTime - startReadTime) / 200)+ " ms \n");
	}

	private void localizedAccess() {    
		random.nextBytes(writeBuffer); 
		startWriteTime = new Date().getTime();
        
        for(int i = 0; i < arrayTest; i++) {                
          for(int j = 0; j < cachedBlocks; j++)	{                  
			writer(j, writeBuffer); 
			}
		}
        
        stopWriteTime = new Date().getTime(); 		
		startReadTime = new Date().getTime();		

        for(int i = 0; i < arrayTest; i++) {                
        	for(int j = 0; j < cachedBlocks; j++) {  
				reader(j, readBuffer);
			}
        }
		stopReadTime = new Date().getTime();
	
		if(!(Arrays.equals(writeBuffer, readBuffer))) {
			 SysLib.cout("DISK VALIDITY ERROR: writerBytes and readBuffer equal\n");
		}
		SysLib.cout("Testing localizedAccess(), using Test :" + testing + "\n");
		SysLib.cout("Cache: [" + status + "] \n");
		SysLib.cout("Avg time for Write : " + ((stopWriteTime - startWriteTime) / 200) + " ms \n");
		SysLib.cout("Avg time for Read  : " + ((stopReadTime - startReadTime) / 200)+ " ms \n");
    }

	private void mixedAccess()
	{ 
		random.nextBytes(writeBuffer);
		int[] mixedAccessArr = new int[arrayTest];            
        for(int i = 0; i < arrayTest; i++) {   
            if(Math.abs(random.nextInt() % cachedBlocks) <= 8) {
            	mixedAccessArr[i] = Math.abs(random.nextInt() % cachedBlocks);            
            } else {   
                mixedAccessArr[i] = Math.abs(random.nextInt() % diskBlockSize);         
            }            
        }
		
		startWriteTime = new Date().getTime();		
        for(int i = 0; i < arrayTest; i++) {    
             writer(mixedAccessArr[i], writeBuffer);              
        }
        stopWriteTime = new Date().getTime();				
		startReadTime = new Date().getTime();			
        
        for(int i = 0; i < arrayTest; i++) {            
            reader(mixedAccessArr[i], readBuffer);                         
        }
        stopReadTime = new Date().getTime();			
		
		if(!(Arrays.equals(writeBuffer, readBuffer))) {
			 SysLib.cout("DISK VALIDITY ERROR: writerBytes and readBuffer equal\n");
		}

		SysLib.cout("Testing mixedAccess(), using Test :" + testing + "\n");
		SysLib.cout("Cache: [" + status + "] \n");
		SysLib.cout("Avg time for Write : " + ((stopWriteTime - startWriteTime) / 200) + " ms \n");
		SysLib.cout("Avg time for Read  : " + ((stopReadTime - startReadTime) / 200)+ " ms \n");
    }

	private void adversaryAccess() {	
		random.nextBytes(writeBuffer); 
        startWriteTime = new Date().getTime();   		
        for (int i = cachedBlocks; i < diskBlockSize; i++) {                                 
            writer(i, writeBuffer);                  
        }        
        stopWriteTime = new Date().getTime();  			
		
        startReadTime = new Date().getTime();                     
        for (int i = cachedBlocks; i < diskBlockSize; i++) {              
            reader(i, readBuffer);                             
        }        
        stopReadTime = new Date().getTime();    		
		
		if(!(Arrays.equals(writeBuffer, readBuffer))) {
			 SysLib.cout("DISK VALIDITY ERROR: writerBytes and readBuffer equal\n");
		}

		SysLib.cout("Testing adversaryAccess(), using Test :" + testing + "\n");
		SysLib.cout("Cache: [" + status + "] \n");
		SysLib.cout("Avg time for Write : " + ((stopWriteTime - startWriteTime) / 200) + " ms \n");
		SysLib.cout("Avg time for Read  : " + ((stopReadTime - startReadTime) / 200)+ " ms \n");

    }
}