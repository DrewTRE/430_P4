/*
Author: Drew Kwak
Date: 5/26/2019
Description: Test file for: Random, Localized, Mixed, Adversary. 
*/

import java.util.*;
import java.io.*;
import java.util.Date;
import java.util.Random;

public class Test4 extends Thread {
	// Data Members. 
	private int cacheBlocks 	= 10;	
	private int arrayTest 		= 200;	
	private int blockSize 		= 512;	
	private int testing;
	private String status 		= "disabled";
	private boolean enabled 	= false;	
	private byte[] readBytes;	
	private byte[] writeBytes;	
	private Random random;
	private long startTime;                           
    private long stopTime;                            

    // Return Performance of each Test. 
    private void getPerformance(String testName) {
    	if (enabled == true) {
	      SysLib.cout("Test: | " + testName + " | Cache: Enabled | " 	+ (stopTime - startTime) + "ms \n");
	    } else {
	      SysLib.cout("Test: | " + testName + " | Cache: Disabled | " 	+ (stopTime - startTime) + "ms \n");
		}
  	}

  	// Creates disk blocks of size 512 and checks if cache is enabled/disabled. 
	public Test4(String[] args) {
		writeBytes 	= new byte[blockSize];               
        readBytes 	= new byte[blockSize]; 
		random 		= new Random();

		if(args[0].equals("enabled")) {
			enabled = true; 
			status 	= "enabled";	
		} else {
			enabled = false;	 
			status 	= "disabled"; 
		}
        testing 	= Integer.parseInt(args[1]);
        
    }

    // Switch statement to pick which test to run. 
	public void run( ) {
    	SysLib.flush();
    	startTime = new Date().getTime();							
    	switch(testing) {                               
    		case 1:	{
				randomAccess();
				stopTime = new Date().getTime();
				getPerformance("Random Access");	 	
				break;        
			}
    		case 2: {
				localizedAccess();	
				stopTime = new Date().getTime();
				getPerformance("Localized Access");	
				break;        
			}
			case 3: {
				mixedAccess(); 	
				stopTime = new Date().getTime();
				getPerformance("Mixed Access");	
				break;        
			}
    		case 4: {
				adversaryAccess(); 	
				stopTime = new Date().getTime();
				getPerformance("Adversary Access");	
				break;        
			} 	      			  			  			  			  			
    	}
    	// Sync with disk depending on if cache is enabled or disabled. 		
		if(enabled) {
			SysLib.csync();		
		} else {
			SysLib.sync();
		}
		SysLib.exit( );                               
    }

    // Determine to use cread or rawread depending if cache is enabled or disabled. 
	private void read(int blockId, byte[] buffer) {
		if (enabled) {
			SysLib.cread(blockId, buffer);
		} else {
			SysLib.rawread(blockId, buffer);
		}
	}
	
	// Determine to use cwrite or rawwrite depending if cache is enabled or disabled. 
	private void write(int blockId, byte[] buffer) {
		if (enabled) {
			SysLib.cwrite(blockId, buffer);
		} else {
			SysLib.rawwrite(blockId, buffer);
		}
	}

	// Test that just randomly accesses blocks. 
	private void randomAccess() {
		random.nextBytes(writeBytes); 
		int[] randomAccessArr = new int[arrayTest];	
		for(int i = 0; i < arrayTest; i++) {
			randomAccessArr[i] = Math.abs(random.nextInt() % blockSize);
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

	// Localized test that read and writes on the same 10 blocks repeatedly.  
	private void localizedAccess() { 
		for (int i = 0; i < 20; i++) {
	     	for (int j = 0; j < blockSize; j++) {
	        	writeBytes[j] = ((byte)(i + j));
	     	} 
	      	for (int j = 0; j < 1000; j += 100) {
	        	write(j, writeBytes);
	      	}
	     	for (int j = 0; j < 1000; j += 100) {
	        	read(j, readBytes);
		        for (int k = 0; k < blockSize; k++) {
		          	if(!(Arrays.equals(writeBytes, readBytes))) {
						SysLib.cerr("ERROR\n");
	            		SysLib.exit();
					}
	        	}
	      	}
	    }
    }

    // Test of 90% localized and 10% randomized. 
	private void mixedAccess() { 
		random.nextBytes(writeBytes);
		int[] mixedAccessArr = new int[arrayTest];            
        for(int i = 0; i < arrayTest; i++) {   
            if(Math.abs(random.nextInt() % cacheBlocks) <= 8) {
            	mixedAccessArr[i] = Math.abs(random.nextInt() % cacheBlocks);            
            } else {   
                mixedAccessArr[i] = Math.abs(random.nextInt() % blockSize);         
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

    // Test to access blocks that will generate a miss. 
	private void adversaryAccess() {	
        for (int i = cacheBlocks; i < blockSize; i++) {                                 
            write(i, writeBytes);                  
        }                    
        for (int i = cacheBlocks; i < blockSize; i++) {              
            read(i, readBytes);                             
        }        			
		if(!(Arrays.equals(writeBytes, readBytes))) {
			SysLib.cerr("ERROR\n");
            SysLib.exit();
		}
    }
}