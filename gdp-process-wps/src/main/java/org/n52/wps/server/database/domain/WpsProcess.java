package org.n52.wps.server.database.domain;

import java.util.UUID;

import org.joda.time.DateTime;

public class WpsProcess {
	
	private final String id;
	private String status;
	private Double percentComplete;
	private final String wpsRequestId;
	private final DateTime startTime;
	private DateTime endTime;
	
	public WpsProcess() {
		id = UUID.randomUUID().toString();
		wpsRequestId = null;
		startTime = null;
	}
	
	
	public DateTime getEndTime() {
		return endTime;
	}
	
	public String getId() {
		return id;
	}
	public Double getPercentComplete() {
		return percentComplete;
	} 
	
	public DateTime getStartTime() {
		return startTime;
	}
	
	public String getStatus() {
		return status;
	}
	
	public String getWpsRequestId() {
		return wpsRequestId;
	}

}
