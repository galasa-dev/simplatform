/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.simplatform.management.facility;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class BatchJob {
	
	private enum Jobfile {
		JESMSGLG(1),
		JESJCL(2),
		JESYSMSG(3),
		SYSOUT(4);
		
		private int number;
		
		Jobfile(int number) {
			this.number = number;
		}
		
		int fileNumber() {
			return this.number;			
		}
	}
	
	private Logger log = Logger.getLogger("Simplatform");
	private Timer timer;
	
	private String owner;
	private String jobid;
	private String jobname;
	private String stepname;
	private String status;
    private String retcode;
	private String jcl;
	private String program;	
	private int programStmtNo;
	private String output;

	private boolean submitted;
	private boolean jclError;	
	private String jclErrorMessage;
	
	private static final String PROP_OWNER = "owner";
	private static final String PROP_JOBID = "jobid";
	private static final String PROP_JOBNAME = "jobname";
	private static final String PROP_STATUS = "status";
	private static final String PROP_RETCODE = "retcode";
	private static final String PROP_MESSAGE = "message";
	private static final String PROP_RC = "rc";
	private static final String PROP_REASON = "reason";
	private static final String PROP_STACK = "stack";
	private static final String PROP_CATEGORY = "category";
	private static final String STATUS_OUTPUT = "OUTPUT";
	
	private Map<Integer, BatchJobOutputFile> jobOutputFiles = new HashMap<>();
	private List<String> datain;
	public BatchJob(String jcl, String user, String jobid) {
		setOwner(user.toUpperCase());
		setJobid(jobid);
		
		// Check if the JCL will run
		parseJclPhase0(jcl);
		if (jclError) {
			log.info(() -> "JOB SUBMIT FAILED - " + jclErrorMessage);
			JsonObject jsonObject = new JsonObject();
			jsonObject.addProperty(PROP_RC, 4);
			jsonObject.addProperty(PROP_REASON, 20);
			jsonObject.addProperty(PROP_STACK, "ERROR: " + jclErrorMessage);
			jsonObject.addProperty(PROP_CATEGORY, 6);
			jsonObject.addProperty(PROP_MESSAGE, jclErrorMessage);
			setOutput(jsonObject.toString());
			submitted = false;
			return;
		}
		
		// Submit the job. Can still result in JCL error if it doesn't meet our requirements
		submitJob();		

		log.info("JOB " + getJobname() + "(" + getJobid() +  ") SUBMITTED");
	}
	
	private void submitJob() {
		// Job in input queue
		setStatus("INPUT");
		setRetcode(null);		
		jobOutputPhase0();
		// Set the JSON response
		refreshJobStatus();
		// Kick off job task but let it sit on the input queue for a second
		setTimer(new Timer());
	    getTimer().schedule(new BatchJobExecutionTask(this), 1000);
		submitted = true;
	}

	private void jobOutputPhase0() {
		BatchJobOutputFile jobOutputFile;
		
		// JESMSGLG
		jobOutputFile = new BatchJobOutputFile("JES2", getJobid(), Jobfile.JESMSGLG.toString(), Jobfile.JESMSGLG.fileNumber(), getJobname(), null);		
		jobOutputFile.addRecord("1               G A L A S A  J O B  L O G  --  S Y S T E M  G L S A  --  N O D E  G A L A S A");
		jobOutputFile.addRecord("0");
		jobOutputFile.addRecord(String.format(" %1$s %2$s ---- %3$s ----", getJesTime(), getJobid(), getJesDate()));
		jobOutputFile.addRecord(String.format(" %1$s %2$s  GALASA01I USERID %3$s IS ASSIGNED TO THIS JOB.", getJesTime(), getJobid(), StringUtils.rightPad(getOwner(), 8)));
		jobOutputFiles.put(jobOutputFile.getId(), jobOutputFile);
	
		// JESJCL
		jobOutputFile = new BatchJobOutputFile("JES2", getJobid(), Jobfile.JESJCL.toString(), Jobfile.JESJCL.fileNumber(), getJobname(), null);
		String[] jclRecords = getJcl().split("\n");		
		jobOutputFile.addRecord("         1 " + StringUtils.rightPad(jclRecords[0], 72) + getJobid());
		for (int i = 1; i < jclRecords.length; i++) {
			if (jclRecords[i].startsWith("//")) {
				jobOutputFile.addRecord(StringUtils.leftPad(Integer.toString(i+1), 10) + " " + jclRecords[i]);
			}
		}
		jobOutputFiles.put(jobOutputFile.getId(), jobOutputFile);
	}

	private void jobOutputPhase1() {
		BatchJobOutputFile jobOutputFile;
		
		// JESMSGLG
		jobOutputFile = jobOutputFiles.get(Jobfile.JESMSGLG.fileNumber());
		jobOutputFile.addRecord(String.format(" %1$s %2$s  GALASA02I %3$-8s STARTED - INIT 21   - CLASS A        - SYS GLSA", getJesTime(), getJobid(), getJobname()));
		jobOutputFile.addRecord(String.format(" %1$s %2$s  GALASA03I %3$-8s - STARTED", getJesTime(), getJobid(), getJobname()));
		
		// JESYSMSG
		jobOutputFile = jobOutputFiles.get(Jobfile.JESYSMSG.fileNumber());
		if (jobOutputFile == null) {
			jobOutputFile = new BatchJobOutputFile("JES2",getJobid(), Jobfile.JESYSMSG.toString(), Jobfile.JESYSMSG.fileNumber(), getJobname(), null);
			jobOutputFiles.put(jobOutputFile.getId(), jobOutputFile);
		}
		jobOutputFile.addRecord(String.format(" GALASA04I ALLOC. FOR %1$s %2$s", getJobname(), getStepname()));
		jobOutputFile.addRecord(" GALASA05I JES2 ALLOCATED TO SYSOUT");
		jobOutputFile.addRecord(" GALASA05I JES2 ALLOCATED TO DATAIN");
	}

	protected void jobOutputEndJob() {
		for (BatchJobOutputFile jobOutputFile : jobOutputFiles.values()) {
			switch (jobOutputFile.getDdname()) {
				case "JESMSGLG":
					jobOutputFile.addRecord(String.format(" %1$s %2$s  GALASA06I %3$-8s - ENDED", getJesTime(), getJobid(), getJobname()));
					jobOutputFile.addRecord(String.format(" %1$s %2$s  GALASA07I %3$-8s ENDED - RC=%4$s", getJesTime(), getJobid(), getJobname(), getRetcode()));
					break;
				case "JESYSMSG":
					jobOutputFile.addRecord(String.format(" GALASA08I %1$-8s %2$-8s - STEP WAS EXECUTED - COND CODE %3$s", getJobname(), getStepname(), getRetcode()));
					break;
				default:
					break;
			}
		}
	}

	public String getOwner() {
		return this.owner;
	}

	public String getJobid() {
		return this.jobid;
	}
	
	public String getJobname() {
		return this.jobname;
	}

	public String getStepname() {
		return this.stepname;
	}

	public String getStatus() {
		return this.status;
	}

	public String getRetcode() {
		return this.retcode;
	}

	public String getJcl() {
		return this.jcl;
	}

	public void refreshJobStatus() {
		JsonObject statusOutput = new JsonObject();
		statusOutput.addProperty(PROP_OWNER, getOwner());
		statusOutput.addProperty(PROP_JOBID, getJobid());
		statusOutput.addProperty(PROP_JOBNAME, getJobname());
		statusOutput.addProperty(PROP_STATUS, getStatus());
		statusOutput.addProperty(PROP_RETCODE, getRetcode());
		setOutput(statusOutput.toString());
	}
	
	public String getOutput() {
		return this.output;
	}

	public void listFiles() {
		JsonArray fileArray = new JsonArray();
		for (BatchJobOutputFile jobOutputFile : this.jobOutputFiles.values()) {
			fileArray.add(jobOutputFile.getJsonObject());
		}
		setOutput(fileArray.toString());
	}

	private void setOwner(String owner) {
		this.owner = owner;
	}

	private void setJobid(String jobid) {
		this.jobid = jobid;
	}

	private void setJobname(String jobname) {
		this.jobname = jobname;
	}

	private void setStepname(String stepname) {
		this.stepname = stepname;		
	}

	protected void setStatus(String status) {
		this.status = status;
	}

	public void setRetcode(String retcode) {
		this.retcode = retcode;
	}

	private void setJcl(String jcl) {
		this.jcl = jcl;
	}

	private void setOutput(String output) {
		this.output = output;		
	}

	private void parseJclPhase0(String jcl) {
		setJcl(jcl);
		jclError = false;
		if (!getJcl().startsWith("/")) {
			jclError = true;
			jclErrorMessage = "Submit input data does not start with a slash";
			return;
		}
		if (!getJcl().substring(1, 2).equals("/")) {
			jclError = true;
			jclErrorMessage = "Job input was not recognized by system as a job";
			return;
		}
		String[] jclRecords = getJcl().split("\n");
		String label = jclRecords[0].substring(2, getJcl().indexOf(' '));
		if (label == null || label.isEmpty() || !isAlphanumericOrNational(label) || !jclRecords[0].trim().contains(" JOB"))  {
			jclError = true;
			jclErrorMessage = "Job input was not recognized by system as a job";
			return;
		}
		setJobname(label.length() > 8 ? label.substring(0,8) : label);
		setStepname("");
		for (String jclRecord : jclRecords) {
			if (jclRecord.startsWith("//") && jclRecord.contains(" EXEC ") && jclRecord.contains("PGM=")) {
				setStepname(jclRecord.split(" ")[0].substring(2));
				break;
			}
		}
	}
	
	protected void parseJclPhase1() {
		String[] jclRecords = getJcl().split("\n");
		// Job name length
		String label = jclRecords[0].substring(2, getJcl().indexOf(' '));
		if (label.length() > 8) {
			writeJclError(1, "GALASA09I INVALID LABEL");
		}
		
		for (int i = 1; i < jclRecords.length; i++) {
			if (jclRecords[i].startsWith("//")) {
				processJclRecord(jclRecords, i);
				if (this.jclError) {
					return;
				}				
			}
		}
		
		jobOutputPhase1();
		if (program != null && !program.equals("SIMBANK")) {
			writeJclError(programStmtNo, "GALASA16I EXEC PGM MUST BE \"SIMBANK\"");
		} else if (datain == null) {
			writeJclError(0, "GALASA17I DATAIN DD STATEMENT MISSING");
		}
	}

	private void processJclRecord(String[] jclRecords, int i) {
		if (jclRecords[i].contains("PGM=")) {
			// Multiple steps
			if (program != null) {
				writeJclError(i, "GALASA11I JCL CONTAINS MORE THAN ONE STEP");
				return;
			}
			program = StringUtils.substringAfter(jclRecords[i], "PGM=");
			programStmtNo = i;
			// Program name
			if (program == null || program.startsWith(" ")) {
				writeJclError(i, "GALASA12I FORMAT ERROR IN THE PGM FIELD");
				return;
			}
			program = StringUtils.stripEnd(program, ", ");
			if (program.length() > 8) {
				writeJclError(i, "GALASA13I EXCESSIVE PARAMETER LENGTH IN THE PGM FIELD");
				return;
			}
		}
		if (jclRecords[i].startsWith("//DATAIN ")) {
			processDatain(jclRecords, i);
		}
	}

	private void processDatain(String[] jclRecords, int i) {
		if (datain != null) {
			writeJclError(i, "GALASA14I MORE THAN ONE DATAIN DD STATEMENT");
			return;
		}
		datain = new ArrayList<>();
		for (int j = i+1; j < jclRecords.length && !jclRecords[j].startsWith("//"); j++) {
			datain.add(jclRecords[j]);
		}
	}

	private void writeJclError(int stmtNo, String message) {
		setStatus(STATUS_OUTPUT);
		setRetcode("JCL ERROR");
		jesmsglgJclError();
		jesysmsgWrite("  STMT NO. MESSAGE");
		if (stmtNo == 0) {
			jesysmsgWrite(String.format("%10s %s", " ", message)); 
		} else {
			jesysmsgWrite(String.format("%10d %s", stmtNo, message)); 
		}
		jclError = true;
		log.info("JOB " + getJobname() + " " + getJobid() +  " JCL ERROR");
	}

	private void jesmsglgJclError() {
		BatchJobOutputFile jobOutputFile = jobOutputFiles.get(Jobfile.JESMSGLG.fileNumber());
		jobOutputFile.addRecord(String.format(" %1$s %2$-8s  GALASA16I INVALID - JOB NOT RUN - JCL ERROR", getJesTime(), getJobid()));
	}

	private void jesysmsgWrite(String text) {
		BatchJobOutputFile jobOutputFile = jobOutputFiles.get(Jobfile.JESYSMSG.fileNumber());
		if (jobOutputFile == null) {
			jobOutputFile = new BatchJobOutputFile("JES2",getJobid(), Jobfile.JESYSMSG.toString(), Jobfile.JESYSMSG.fileNumber(), getJobname(), null);
			jobOutputFiles.put(jobOutputFile.getId(), jobOutputFile);
		}
		jobOutputFile.addRecord(text);
	}

	private boolean isAlphanumericOrNational (String label) {
	    String pattern= "^[A-Z0-9£$#@]*$";
	    return label.matches(pattern);
	}
	
	private String getJesTime() {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH.mm.ss"));
	}

	private String getJesDate() {
		return StringUtils.rightPad(getDay() + ",", 11) + LocalDateTime.now().format(DateTimeFormatter.ofPattern("d MMM y")).toUpperCase();
	}

	private String getDay() {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE")).toUpperCase();
	}
	
	private void setTimer(Timer timer) {
		this.timer = timer;
	}

	protected Timer getTimer() {
		return this.timer;
	}

	public String getFile(String token) {
		if (StringUtils.isNumeric(token)) {
			int id = Integer.parseInt(token);
			BatchJobOutputFile jobOutpuFile = this.jobOutputFiles.get(id); 
			if (jobOutpuFile != null) {
				return jobOutpuFile.getContent(); 
			}
		} else if (token.equals("JCL")) {
			return this.jcl;
		}
		return null;
	}

	public void purge() {
		JsonObject statusOutput = new JsonObject();
		statusOutput.addProperty(PROP_OWNER, getOwner());
		statusOutput.addProperty(PROP_JOBID, getJobid());
		statusOutput.addProperty(PROP_MESSAGE, "Request was successful.");
		statusOutput.addProperty(PROP_JOBNAME, getJobname());
		statusOutput.addProperty(PROP_STATUS, 0);
		setOutput(statusOutput.toString());
	}

	public boolean isSubmitted() {
		return this.submitted;
	}

	public boolean jclError() {
		return jclError;
	}

	protected void processSimbankAccounts() {
		log.info("Processing SIMBANK accounts...");
		
		// SYSOUT
		BatchJobOutputFile jobOutputFile = new BatchJobOutputFile(getStepname(), getJobid(), Jobfile.SYSOUT.toString(), Jobfile.SYSOUT.fileNumber(), getJobname(), null);
		jobOutputFile.addRecord(" ------------------------------ SIMBANK ------------------------------");
		jobOutputFile.addRecord(" Updating SIMBANK accounts");
		for (String record : datain) {
			jobOutputFile.addRecord(" " + record);
		}
		jobOutputFiles.put(jobOutputFile.getId(), jobOutputFile);
		setRetcode("0000");
		setStatus(STATUS_OUTPUT);
		jobOutputEndJob();
		refreshJobStatus();
	}
}
