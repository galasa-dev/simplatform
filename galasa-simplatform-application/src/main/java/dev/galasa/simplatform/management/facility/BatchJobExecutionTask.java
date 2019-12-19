/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.simplatform.management.facility;

import java.util.TimerTask;

public class BatchJobExecutionTask extends TimerTask {

	private BatchJob batchJob;

	public BatchJobExecutionTask(BatchJob batchJob) {
		this.batchJob = batchJob;
	}
	
	@Override
	public void run() {		
		batchJob.getTimer().cancel();
		
		try {
	        // Job is now active	    	
			batchJob.setStatus("ACTIVE");
			
			// Check JCL meets our requirements
			batchJob.parseJclPhase1();
			if (batchJob.jclError()) {
	    		return;
			}
			
			// Process the SIMBANK job control cards to update accounts
			batchJob.processSimbankAccounts();
		} catch (Exception e) {
			batchJob.writeStackTraceToOutput(e);
		}
	}
}
