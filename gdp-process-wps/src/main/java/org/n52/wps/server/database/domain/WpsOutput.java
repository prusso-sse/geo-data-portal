package org.n52.wps.server.database.domain;

import java.util.UUID;

import net.opengis.wps.x100.OutputDataType;

public class WpsOutput {
	
	private final String id;
	private final String wpsResponseId;
	private final String mimeType;
	private final long responseLength;
	private final String location;
	
	

	public WpsOutput(String inWpsResponseId, OutputDataType processOutput) {
		id = UUID.randomUUID().toString();
		wpsResponseId = inWpsResponseId;
		responseLength = 0;
		mimeType = null;
		location = null;
	}

}
