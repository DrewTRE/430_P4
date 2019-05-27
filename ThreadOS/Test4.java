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
	private byte[] readBytes;	
	private byte[] writeBytes;	
	private Random random;

	private long startTime;                           
    private long stopTime;                            

    private void getPerformance(String testName) {
    	if (cacheEnabled == true) {
	      SysLib.cout("Test: " + testName + " | Cache: Enabled | " 	+ (stopTime - startTime) + "ms \n");
	    } else {
	      SysLib.cout("Test: " + testName + " | Cache: Disabled | " + (stopTime - startTime) + "ms \n");
		}
  	}

	public Test4(String[] args) {
		writeBytes = new byte[diskBlockSize];               
        readBytes 	= new byte[diskBlockSize]; 
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
    	startTime = new Date().getTime();							
    	switch(testing) {                               
    		case 1:	{
				randomAccess();
				stopTime = new Date().getTime();
				getPerformance("| Random Access | ");	 	
				break;        
			}
    		case 2: {
				localizedAccess();	
				stopTime = new Date().getTime();
				getPerformance("| Localized Access | ");	
				break;        
			}
			case 3: {
				mixedAccess(); 	
				stopTime = new Date().getTime();
				getPerformance("| Mixed Access | ");	
				break;        
			}
    		case 4: {
				adversaryAccess(); 	
				stopTime = new Date().getTime();
				getPerformance("| Adversary Access |");	
				break;        
			}	 
		if(cacheEnabled) {
		SysLib.csync();		
		} else {
			SysLib.sync();
		}
		SysLib.exit( );   		   		  			  			  			  			
    	}                            
    }

	private void read(int blockID, byte[] buffer) {
		if (cacheEnabled) {
			SysLib.cread(blockID, buffer);
		} else {
			SysLib.rawread(blockID, buffer);
		}
	}
	
	private void write(int blockID, byte[] buffer) {
		if (cacheEnabled) {
			SysLib.cwrite(blockID, buffer);
		} else {
			SysLib.rawwrite(blockID, buffer);
		}
	}

	private void randomAccess() {
		random.nextBytes(writeBytes); 
		int[] randomAccessArr = new int[arrayTest];
		
		for(int i = 0; i < arrayTest; i++) {
			randomAccessArr[i] = Math.abs(random.nextInt() % diskBlockSize);
		}

		for (int i = 0; i < arrayTest; i++) {
			write(randomAccessArr[i], writeBytes); 
		}

		for(int i = 0; i < arrayTest; i++) {	
			read(randomAccessArr[i], readBytes);
		}

		if(!(Arrays.equals(writeBytes, readBytes))) {
			SysLib.cerr("ERROR\n");
            SysLib.exit();
		}	
	}

	private void localizedAccess() {    
		random.nextBytes(writeBytes); 
        
        for(int i = 0; i < arrayTest; i++) {                
          for(int j = 0; j < cachedBlocks; j++)	{                  
			write(j, writeBytes); 
			}
		}	

        for(int i = 0; i < arrayTest; i++) {                
        	for(int j = 0; j < cachedBlocks; j++) {  
				read(j, readBytes);
			}
        }
	
		if(!(Arrays.equals(writeBytes, readBytes))) {
			SysLib.cerr("ERROR\n");
            SysLib.exit();
		}
    }

	private void mixedAccess()
	{ 
		random.nextBytes(writeBytes);
		int[] mixedAccessArr = new int[arrayTest];            
        for(int i = 0; i < arrayTest; i++) {   
            if(Math.abs(random.nextInt() % cachedBlocks) <= 8) {
            	mixedAccessArr[i] = Math.abs(random.nextInt() % cachedBlocks);            
            } else {   
                mixedAccessArr[i] = Math.abs(random.nextInt() % diskBlockSize);         
            }            
        }
		
        for(int i = 0; i < arrayTest; i++) {    
             write(mixedAccessArr[i], writeBytes);              
        }		
        
        for(int i = 0; i < arrayTest; i++) {            
            read(mixedAccessArr[i], readBytes);                         
        }		
		
		if(!(Arrays.equals(writeBytes, readBytes))) {
			SysLib.cerr("ERROR\n");
            SysLib.exit();
		}
    }

	private void adversaryAccess() {	
		random.nextBytes(writeBytes); 
     		
        for (int i = cachedBlocks; i < diskBlockSize; i++) {                                 
            write(i, writeBytes);                  
        }        
                 
        for (int i = cachedBlocks; i < diskBlockSize; i++) {              
            read(i, readBytes);                             
        }        		
		
		if(!(Arrays.equals(writeBytes, readBytes))) {
			SysLib.cerr("ERROR\n");
            SysLib.exit();
		}
    }
}