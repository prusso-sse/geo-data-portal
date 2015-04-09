package org.n52.wps.server.database.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.opengis.wps.x100.ExecuteResponseDocument;
import net.opengis.wps.x100.ExecuteResponseDocument.ExecuteResponse.ProcessOutputs;
import net.opengis.wps.x100.OutputDataType;

public class WpsResponse {
	
	private final String id;
	private final String wpsRequestId;
	private final String wpsProcessId;
	private final List<WpsOutput> outputs;

	public WpsResponse(String inWpsRequestId, ExecuteResponseDocument executeResponseDoc) {
		id = UUID.randomUUID().toString();
		wpsRequestId = inWpsRequestId;
		wpsProcessId = executeResponseDoc.getExecuteResponse().getProcess().getIdentifier().getStringValue();
		outputs = parseOutputs(id, executeResponseDoc.getExecuteResponse().getProcessOutputs());
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
	
	private List<WpsOutput> parseOutputs(String inWpsResponseId, ProcessOutputs processOutputs) {
		List<WpsOutput> ret = new ArrayList<>();
		if (processOutputs != null) {
			for (OutputDataType processOutput : processOutputs.getOutputArray()) {
				ret.add(new WpsOutput(inWpsResponseId, processOutput));
			}
		}
		return ret;
	}
}
