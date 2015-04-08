package org.n52.wps.server.database.domain;

import java.util.UUID;

import net.opengis.wps.x100.DocumentOutputDefinitionType;

public class WpsRequestedOutput {
	
	private final String id;
	private final String wpsRequestId;
	private String value;

	public WpsRequestedOutput(String inWpsRequestId, DocumentOutputDefinitionType outputDefinitionType) {
		id = UUID.randomUUID().toString();
		wpsRequestId = inWpsRequestId;
		value = outputDefinitionType.getIdentifier().getStringValue();
	}
	
	public String getId() {
		return id;
	}
	
	public String getWpsRequestId() {
		return wpsRequestId;
	}
	
	public String getValue() {
		return value;
	}


}
