package org.n52.wps.server.database.domain;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.opengis.wps.x100.ExecuteResponseDocument;
import net.opengis.wps.x100.ExecuteResponseDocument.ExecuteResponse.ProcessOutputs;
import net.opengis.wps.x100.OutputDataType;
import net.opengis.wps.x100.ProcessStartedType;
import net.opengis.wps.x100.StatusType;

import org.joda.time.DateTime;

public class WpsResponse {
	
	private final String id;
	private final String wpsRequestId;
	private final String wpsAlgoIdentifer;
	private final WpsStatus status;
	private final Integer percentComplete;
	private final DateTime creationTime;
	private List<WpsOutput> outputs;
	private DateTime startTime;
	private DateTime endTime;

	public WpsResponse(String wpsRequestId, InputStream inputStream) {
		this(wpsRequestId, constructExecuteResponseFromStream(inputStream));
	}
	
	public WpsResponse(String inWpsRequestId, ExecuteResponseDocument executeResponseDoc) {
		id = UUID.randomUUID().toString();
		wpsRequestId = inWpsRequestId;
		wpsAlgoIdentifer = executeResponseDoc.getExecuteResponse().getProcess().getIdentifier().getStringValue();
		outputs = parseOutputs(id, executeResponseDoc.getExecuteResponse().getProcessOutputs());
		StatusType executeStatus = executeResponseDoc.getExecuteResponse().getStatus();
		status = WpsStatus.lookup(executeStatus);
		percentComplete = parsePercentComplete(executeStatus);
		creationTime = new DateTime(executeStatus.getCreationTime());
//		TODO put this in database
//		ProcessFailedType processFailed = executeResponseDoc.getExecuteResponse().getStatus().getProcessFailed();
//		if (processFailed != null) {
//			processFailed.getExceptionReport()
//		}
	}
	
	public WpsResponse(String inId, String inWpsRequestId, String inWpsAlgoIdentifer, WpsStatus inStatus, Integer inPercentComplete, DateTime inCreationTime) {
		id = inId;
		wpsRequestId = inWpsRequestId;
		wpsAlgoIdentifer = inWpsAlgoIdentifer;
		status = inStatus;
		percentComplete = inPercentComplete;
		creationTime = inCreationTime;
	}
	
	private static ExecuteResponseDocument constructExecuteResponseFromStream(InputStream inputStream) {
		try {
			return ExecuteResponseDocument.Factory.parse(inputStream);
		} catch (Exception e) {
			throw new RuntimeException("issue constructing ExecuteDocument from xml request", e);
		}
	}
	
	public String getId() {
		return id;
	}
	
	public String getWpsRequestId() {
		return wpsRequestId;
	}

	public String getWpsAlgoIdentifer() {
		return wpsAlgoIdentifer;
	}
	
	public List<WpsOutput> getOutputs() {
		return outputs;
	}
	
	public void setOutputs(List<WpsOutput> outputs) {
		this.outputs = outputs;
	}

	public WpsStatus getStatus() {
		return status;
	}

	public Integer getPercentComplete() {
		return percentComplete;
	}

	public DateTime getCreationTime() {
		return creationTime;
	}

	public DateTime getEndTime() {
		return endTime;
	}
	
	public DateTime getStartTime() {
		return startTime;
	}
	
	public void setStartTime(DateTime startTime) {
		this.startTime = startTime;
	}
	
	public void setEndTime(DateTime endTime) {
		this.endTime = endTime;
	}
	
	private List<WpsOutput> parseOutputs(String inWpsResponseId, ProcessOutputs processOutputs) {
		List<WpsOutput> ret = new ArrayList<>();
		if (processOutputs != null) {
			for (OutputDataType processOutput : processOutputs.getOutputArray()) {
				ret.add(new WpsOutput(inWpsResponseId, processOutput, processOutput.getReference() != null));
			}
		}
		return ret;
	}
	
	private Integer parsePercentComplete(StatusType type) {
		Integer percentage = 0;
		ProcessStartedType processPaused = type.getProcessPaused();
		ProcessStartedType processStarted = type.getProcessStarted();
		if (processPaused != null) {
			percentage = processPaused.getPercentCompleted();
		}
		if (processStarted != null) {
			percentage = processStarted.getPercentCompleted();
		}
		return percentage;
	}
}
