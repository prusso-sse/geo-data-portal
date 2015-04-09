package org.n52.wps.server.database.domain;

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
	private final String wpsProcessId;
	private final List<WpsOutput> outputs;
	private final WpsStatus status;
	private final Integer percentComplete;
	private final DateTime creationTime;
	private final DateTime endTime;

	public WpsResponse(String inWpsRequestId, ExecuteResponseDocument executeResponseDoc) {
		id = UUID.randomUUID().toString();
		wpsRequestId = inWpsRequestId;
		wpsProcessId = executeResponseDoc.getExecuteResponse().getProcess().getIdentifier().getStringValue();
		outputs = parseOutputs(id, executeResponseDoc.getExecuteResponse().getProcessOutputs());
		StatusType executeStatus = executeResponseDoc.getExecuteResponse().getStatus();
		status = WpsStatus.lookup(executeStatus);
		percentComplete = parsePercentComplete(executeStatus);
		creationTime = new DateTime(executeStatus.getCreationTime());
		endTime = null;
	}
	
	public String getId() {
		return id;
	}
	
	public String getWpsRequestId() {
		return wpsRequestId;
	}

	public String getWpsProcessId() {
		return wpsProcessId;
	}
	
	public List<WpsOutput> getOutputs() {
		return outputs;
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
	
	private List<WpsOutput> parseOutputs(String inWpsResponseId, ProcessOutputs processOutputs) {
		List<WpsOutput> ret = new ArrayList<>();
		if (processOutputs != null) {
			for (OutputDataType processOutput : processOutputs.getOutputArray()) {
				ret.add(new WpsOutput(inWpsResponseId, processOutput));
			}
		}
		return ret;
	}
	
	private Integer parsePercentComplete(StatusType type) {
		Integer percentage = null;
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
