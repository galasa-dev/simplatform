/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.simplatform.management.facility;

import java.util.LinkedList;
import java.util.List;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public class BatchJobOutputFile {
	
	private int id;
	private String ddname;
	private List<String> fileContentList  = new LinkedList<>();
	private JsonObject jsonObject = new JsonObject();

	public BatchJobOutputFile(String stepname, String jobid, String ddname, int id, String jobname, String procstep) {
		this.id = id;
		this.ddname = ddname;
		
		if (!stepname.equals("")) {
			this.jsonObject.addProperty("stepname", stepname);
		}
		this.jsonObject.addProperty("jobid", jobid);
		this.jsonObject.addProperty("ddname", ddname);
		this.jsonObject.addProperty("id", id);
		this.jsonObject.addProperty("jobname", jobname);
		if (procstep == null) {
			this.jsonObject.add("procstep", JsonNull.INSTANCE);
		} else {
			this.jsonObject.addProperty("procstep", procstep);
		}
	}

	public int getId() {
		return this.id;
	}
	
	public JsonObject getJsonObject() {
		return this.jsonObject;
	}

	public String getContent() {
		return String.join("\n", this.fileContentList);
	}

	public String getDdname() {
		return this.ddname;
	}

	public void addRecord(String record) {
		this.fileContentList.add(record);
	}
}
