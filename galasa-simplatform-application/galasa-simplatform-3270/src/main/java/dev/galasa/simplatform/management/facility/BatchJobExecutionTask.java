/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.simplatform.management.facility;

import java.util.TimerTask;

import dev.galasa.zosbatch.IZosBatchJob.JobStatus;

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
			batchJob.setStatus(JobStatus.ACTIVE);
			
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
