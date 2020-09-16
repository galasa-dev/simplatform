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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dev.galasa.simplatform.application.Bank;
import dev.galasa.simplatform.exceptions.AccountNotFoundException;
import dev.galasa.simplatform.exceptions.DuplicateAccountException;

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
	
	private static final String ACCOUNT_OPEN = "ACCOUNT_OPEN";
	private static final String ACCOUNT_REPORT = "ACCOUNT_REPORT";
	
	private static final String REPORT_HEAD = "1------------------------------ SIMBANK ------------------------------";
	private static final String REPORT_ASA_0 = "0";
	
	private Map<Integer, BatchJobOutputFile> jobOutputFiles = new HashMap<>();
	private String control;
	private List<String> datain;
	private boolean controldDdFound = false;
	private boolean datainDdFound = false;
	private boolean jclerrorHeadWritten;
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
		jobOutputFile = getJobOutputfile(Jobfile.JESMSGLG);		
		jobOutputFile.addRecord("1            S Y M P L A T F O R M  J O B  L O G  --  S Y S T E M  S Y M P  --  N O D E  G A L A S A");
		jobOutputFile.addRecord("0");
		jobOutputFile.addRecord(String.format(" %1$s %2$s ---- %3$s ----", getJesTime(), getJobid(), getJesDate()));
		jobOutputFile.addRecord(String.format(" %1$s %2$s  SYMP0001I USERID %3$s IS ASSIGNED TO THIS JOB.", getJesTime(), getJobid(), StringUtils.rightPad(getOwner(), 8)));
	
		// JESJCL
		jobOutputFile = getJobOutputfile(Jobfile.JESJCL);
		String[] jclRecords = getJcl().split("\n");		
		jobOutputFile.addRecord("         1 " + StringUtils.rightPad(jclRecords[0], 72) + getJobid());
		for (int i = 1; i < jclRecords.length; i++) {
			if (jclRecords[i].startsWith("//")) {
				jobOutputFile.addRecord(StringUtils.leftPad(Integer.toString(i+1), 10) + " " + jclRecords[i]);
			}
		}
	}

	private void jobOutputPhase1() {
		BatchJobOutputFile jobOutputFile;
		
		// JESMSGLG
		jobOutputFile = getJobOutputfile(Jobfile.JESMSGLG);
		jobOutputFile.addRecord(String.format(" %1$s %2$s  SYMP0002I %3$-8s STARTED - INIT 21   - CLASS A        - SYS GLSA", getJesTime(), getJobid(), getJobname()));
		jobOutputFile.addRecord(String.format(" %1$s %2$s  SYMP0003I %3$-8s - STARTED", getJesTime(), getJobid(), getJobname()));
		
		// JESYSMSG
		jobOutputFile = getJobOutputfile(Jobfile.JESYSMSG);
		jobOutputFile.addRecord(String.format(" SYMP0004I ALLOC. FOR %1$s %2$s", getJobname(), getStepname()));
		jobOutputFile.addRecord(" SYMP0005I JES2 ALLOCATED TO SYSOUT");
		if (controldDdFound) {
			jobOutputFile.addRecord(" SYMP0005I JES2 ALLOCATED TO CONTROL");
		}
		if (datainDdFound) {
			jobOutputFile.addRecord(" SYMP0005I JES2 ALLOCATED TO DATAIN");
		}
	}

	protected void jobOutputEndJob() {
		for (BatchJobOutputFile jobOutputFile : jobOutputFiles.values()) {
			switch (jobOutputFile.getDdname()) {
				case "JESMSGLG":
					jobOutputFile.addRecord(String.format(" %1$s %2$s  SYMP0006I %3$-8s - ENDED", getJesTime(), getJobid(), getJobname()));
					jobOutputFile.addRecord(String.format(" %1$s %2$s  SYMP0007I %3$-8s ENDED - RC=%4$s", getJesTime(), getJobid(), getJobname(), getRetcode()));
					break;
				case "JESYSMSG":
					jobOutputFile.addRecord(String.format(" SYMP0008I %1$-8s %2$-8s - STEP WAS EXECUTED - COND CODE %3$s", getJobname(), getStepname(), getRetcode()));
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
			writeJclError(1, "SYMP0009I INVALID LABEL");
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
			writeJclError(programStmtNo, "SYMP0016I EXEC PGM MUST BE \"SIMBANK\"");
		}
		if (!controldDdFound) {
			writeJclError(0, "SYMP0017I CONTROL DD STATEMENT MISSING");
		}
		if (!datainDdFound) {
			writeJclError(0, "SYMP0017I DATAIN  DD STATEMENT MISSING");
		}
	}

	private void processJclRecord(String[] jclRecords, int i) {
		if (jclRecords[i].contains("PGM=")) {
			// Multiple steps
			if (program != null) {
				writeJclError(i, "SYMP0011I JCL CONTAINS MORE THAN ONE STEP");
				return;
			}
			program = StringUtils.substringAfter(jclRecords[i], "PGM=");
			programStmtNo = i;
			// Program name
			if (program == null || program.startsWith(" ")) {
				writeJclError(i, "SYMP0012I FORMAT ERROR IN THE PGM FIELD");
				return;
			}
			program = StringUtils.stripEnd(program, ", ");
			if (program.length() > 8) {
				writeJclError(i, "SYMP0013I EXCESSIVE PARAMETER LENGTH IN THE PGM FIELD");
				return;
			}
		}
		if (jclRecords[i].startsWith("//CONTROL ")) {
			controldDdFound = true;
			processControl(jclRecords, i);
		}
		if (jclRecords[i].startsWith("//DATAIN ")) {
			datainDdFound = true;
			processDatain(jclRecords, i);
		}
	}

	private void processControl(String[] jclRecords, int i) {
		if (control != null) {
			writeJclError(i, "SYMP0014I MORE THAN ONE CONTROL DD STATEMENT");
			return;
		}
		if (i <= jclRecords.length +1 && !jclRecords[i+1].startsWith("//")) {
			control = jclRecords[i+1];
		}
	}

	private void processDatain(String[] jclRecords, int i) {
		if (datain != null) {
			writeJclError(i, "SYMP0014I MORE THAN ONE DATAIN DD STATEMENT");
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
		if (!jclerrorHeadWritten) {
			jesmsglgJclError();
			jesysmsgWrite("  STMT NO. MESSAGE");
		}
		jclerrorHeadWritten = true;
		if (stmtNo == 0) {
			jesysmsgWrite(String.format("%10s %s", " ", message)); 
		} else {
			jesysmsgWrite(String.format("%10d %s", stmtNo, message)); 
		}
		jclError = true;
		log.info("JOB " + getJobname() + " " + getJobid() +  " JCL ERROR");
	}

	private void jesmsglgJclError() {
		BatchJobOutputFile jobOutputFile = getJobOutputfile(Jobfile.JESMSGLG);
		jobOutputFile.addRecord(String.format(" %1$s %2$-8s  SYMP0016I INVALID - JOB NOT RUN - JCL ERROR", getJesTime(), getJobid()));
	}

	private void jesysmsgWrite(String text) {
		BatchJobOutputFile jobOutputFile = getJobOutputfile(Jobfile.JESYSMSG);
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

    public void cancel() {
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
		
		if (validControl()) {
			Map<String, String> invalidRecords = new LinkedHashMap<>();
			Map<String, String> processedAccounts = new LinkedHashMap<>();
			String recordCounter;
			int i = 0;
			for (String record : datain) {
				recordCounter = StringUtils.leftPad(Integer.toString(++i), 6, "0");
				if (validRecord(record, recordCounter, invalidRecords)) {
					processAccount(recordCounter, record, processedAccounts, invalidRecords);
				}
			}
			writeSysoutreport(processedAccounts, invalidRecords);
		}
		setStatus(STATUS_OUTPUT);
		jobOutputEndJob();
		refreshJobStatus();
	}

	private boolean validControl() {
		if (control != null && (control.equals(ACCOUNT_OPEN) || control.equals(ACCOUNT_REPORT))) {
			return true;
		}

		BatchJobOutputFile jobOutputFile = getJobOutputfile(Jobfile.SYSOUT);
		jobOutputFile.addRecord(REPORT_HEAD);
		jobOutputFile.addRecord(REPORT_ASA_0);
		if (control == null) {
			jobOutputFile.addRecord(" ERROR - Control keyword not supplied");
		} else {
			jobOutputFile.addRecord(" ERROR - Control keyword not recognised: " + control);
		}
		setRetcode("CC 0020");
		
		return false;
	}

	private BatchJobOutputFile getJobOutputfile(Jobfile jobfile) {
		BatchJobOutputFile jobOutputFile = jobOutputFiles.get(jobfile.fileNumber());
		if (jobOutputFile == null) {
			jobOutputFile = new BatchJobOutputFile(jobfile.toString().startsWith("JES") ? "JES2" : getStepname(), getJobid(), jobfile.toString(), jobfile.fileNumber(), getJobname(), null);
			jobOutputFiles.put(jobfile.number, jobOutputFile);
		}
		return jobOutputFile;
	}

	private boolean validRecord(String record, String recordCounter, Map<String, String> invalidRecords) {
		if (record.length() > 80) {
			invalidRecords.put(recordCounter, String.format("%1$-79s/ INVALID INPUT - Input record > 80 characters", record.substring(0, 79)));
			return false;
		} else if (record.startsWith(" ")) {
			invalidRecords.put(recordCounter, String.format("%1$-80s INVALID INPUT - Input record must start in first column", record));
			return false;
		}
		return validFields(record, recordCounter, invalidRecords);
	}

	private boolean validFields(String record, String recordCounter, Map<String, String> invalidRecords) {
		Double maxBalance = 1.0E8;
		boolean valid = true;
		String[] fields = record.trim().split(",");
		if (control.equals(ACCOUNT_REPORT) && fields.length != 2) {
			invalidRecords.put(recordCounter, String.format("%1$-80s INVALID INPUT - Input record format for ACCOUNT_REPORT is AccountNumber,SortCode", record));
			valid = false;
		} else if (control.equals(ACCOUNT_OPEN) && fields.length != 3) {
			invalidRecords.put(recordCounter, String.format("%1$-80s INVALID INPUT - Input record format for ACCOUNT_OPEN is AccountNumber,SortCode,Balance", record));
			valid = false;
		} else {
			String accountNumber = fields[0];
			String sortCode = fields[1];
			Double balance = null;
			if (control.equals(ACCOUNT_OPEN)) {
				balance = toDouble(fields[2]);
			}
			StringBuilder errorMessage = new StringBuilder();
			errorMessage.append(validAccountNumber(accountNumber));
			errorMessage.append(validSortCode(sortCode));
			errorMessage.append(validBalance(balance));
			
			if (errorMessage.length() != 0) {
				invalidRecords.put(recordCounter, String.format("%1$-80s INVALID INPUT - %2$s", record, errorMessage));
				valid = false;
			} else if (control.equals(ACCOUNT_OPEN) && balance >= maxBalance) {
				invalidRecords.put(recordCounter, String.format("%1$-80s INVALID INPUT - Balance exceeds %2$,.2f", record, maxBalance-0.01));
				valid = false;
			}
		}
		return valid;
	}

	private String validAccountNumber(String accountNumber) {
		StringBuilder errorMessage = new StringBuilder();
		if (!StringUtils.isNumeric(accountNumber)) {
			errorMessage.append("Account number must be numeric. ");
		} else if (accountNumber.length() != 9) {
			errorMessage.append("Account number must be 9 numeric digits. ");
		}
		return errorMessage.toString();
	}

	private String validSortCode(String sortCode) {
		String errorMessage = "";
		if (!sortCode.matches("[0-9][0-9]-[0-9][0-9]-[0-9][0-9]")) {
			errorMessage = "SortCode must of the format 99-99-99. ";
		}
		return errorMessage;
	}

	private String validBalance(Double balance) {
		String errorMessage = "";
		if (control.equals(ACCOUNT_OPEN) && balance == null) {
			errorMessage = "Balance must be a Double.";
		}
		return errorMessage;
	}

	private void processAccount(String recordCounter, String record, Map<String, String> processdAccounts, Map<String, String> invalidRecords) {
		String[] fields = record.trim().split(",");
		String accountNumber = fields[0];
		String sortCode = fields[1];
		if (control.equals(ACCOUNT_OPEN)) {
			Double balance = Double.parseDouble(fields[2]);
			Bank bank = new Bank();
			try {
				boolean successful = bank.openAccount(accountNumber, sortCode, balance);
				if (!successful) {
					invalidRecords.put(recordCounter, String.format("%1$-80s DATABASE ERROR - %2$s", record, bank.getDatabaseException()));
				} else if (!bank.accountExists(accountNumber)) {
					invalidRecords.put(recordCounter, String.format("%1$-80s UNKNOWN ERROR - Account not opened", record));
				}
			} catch (DuplicateAccountException e) {
				invalidRecords.put(recordCounter, String.format("%1$-80s INVALID INPUT - Account exists", record));
				return;
			}
			processdAccounts.put(recordCounter, String.format("%1$-14s   %2$-9s   %3$,15.2f - Account opened", accountNumber, sortCode, balance));
		} else if (control.equals(ACCOUNT_REPORT)) {
			Bank bank = new Bank();
			double balance;
			try {
				balance = bank.getBalance(accountNumber);
			} catch (AccountNotFoundException e) {
				invalidRecords.put(recordCounter, String.format("%1$-80s INVALID INPUT - Account does not exist", record));
				return;
			}
			processdAccounts.put(recordCounter, String.format("%1$-14s   %2$-9s   %3$,15.2f", accountNumber, sortCode, balance));
		}
	}

	private void writeSysoutreport(Map<String, String> processedAccounts, Map<String, String> invalidRecords) {
		BatchJobOutputFile jobOutputFile = getJobOutputfile(Jobfile.SYSOUT);
		jobOutputFile.addRecord(REPORT_HEAD);
		jobOutputFile.addRecord(REPORT_ASA_0);
		jobOutputFile.addRecord(" Record Number   Account Number   Sort-code           Balance");
		jobOutputFile.addRecord(" =============   ==============   =========   ===============");
		
		for (Entry<String, String> entry : processedAccounts.entrySet()) {
			jobOutputFile.addRecord(String.format(" %1$-13s   %2$s", entry.getKey(), entry.getValue()));
		}

		if (processedAccounts.size() == 0) {
			jobOutputFile.addRecord("0ERROR: No valid input records supplied");
		}
		jobOutputFile.addRecord(String.format("0  Records read       %6d", processedAccounts.size() + invalidRecords.size()));
		jobOutputFile.addRecord(String.format("   Records rejected   %6d", invalidRecords.size()));
		jobOutputFile.addRecord(String.format("   Records processed  %6d", processedAccounts.size()));
		if (invalidRecords.size() == 0) {
			setRetcode("CC 0000");
		} else {
			jobOutputFile.addRecord(REPORT_HEAD);
			jobOutputFile.addRecord(REPORT_ASA_0);
			jobOutputFile.addRecord("0                            Error Report");
			jobOutputFile.addRecord(" Record");
			jobOutputFile.addRecord(String.format(" Number %1$-80s Message", "Record"));
			jobOutputFile.addRecord(String.format(" %1s %2s %3s", StringUtils.repeat('=', 6), StringUtils.repeat('=', 80), StringUtils.repeat('=', 150)));
			for (Entry<String, String> entry : invalidRecords.entrySet()) {
				jobOutputFile.addRecord(String.format(" %1s %2s", entry.getKey(), entry.getValue()));				
			}
			if (processedAccounts.size() == 0) {
				setRetcode("CC 0020");
			} else {
				setRetcode("CC 0004");
			}
		}
	}

	public void writeStackTraceToOutput(Exception e) {
		log.log(Level.SEVERE, "Exception in Symbank batch processing", e);
		setStatus(STATUS_OUTPUT);
		BatchJobOutputFile jobOutputFile = getJobOutputfile(Jobfile.JESMSGLG);
		jobOutputFile.addRecord(ExceptionUtils.getStackTrace(e));
	}
	
	private Double toDouble(String s) {
		try {
			return Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
