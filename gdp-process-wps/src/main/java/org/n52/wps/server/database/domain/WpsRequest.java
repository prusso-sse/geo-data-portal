package org.n52.wps.server.database.domain;

import java.util.UUID;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement
public class WpsRequest {
	
	private final String id;
	@JacksonXmlProperty(namespace="ows", localName="Identifier")
	private String wpsAlgoIdentifer;
	
	public WpsRequest() {
		id = UUID.randomUUID().toString();
	}

	public Object getId() {
		return id;
	}
	
	public void setWpsAlgoIdentifer(String wpsAlgoIdentifer) {
		this.wpsAlgoIdentifer = wpsAlgoIdentifer;
	}
	
	public String getWpsAlgoIdentifer() {
		return wpsAlgoIdentifer;
	}

}
