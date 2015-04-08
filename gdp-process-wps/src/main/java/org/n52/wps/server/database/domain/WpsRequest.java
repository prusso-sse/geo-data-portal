package org.n52.wps.server.database.domain;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import net.opengis.wps.x100.DataInputsType;
import net.opengis.wps.x100.DocumentOutputDefinitionType;
import net.opengis.wps.x100.ExecuteDocument;
import net.opengis.wps.x100.ExecuteDocument.Execute;
import net.opengis.wps.x100.InputType;
import net.opengis.wps.x100.ResponseDocumentType;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.collect.ImmutableList;

public class WpsRequest {
	
	private final String id;
	
	private final String wpsAlgoIdentifer;

	private final List<WpsInput> wpsInputs;

	private final List<WpsRequestedOutput> wpsRequestedOutputs;

	private final ExecuteDocument executeDoc;
	
	public WpsRequest(String requeistId, InputStream stream) {
		this(requeistId, constructExecuteFromStream(stream));
	}
	

	public WpsRequest(String requtestId, ExecuteDocument inExecuteDoc) {
		id = requtestId;
		executeDoc = inExecuteDoc;
		Execute execute = inExecuteDoc.getExecute();
		wpsAlgoIdentifer = execute.getIdentifier().getStringValue();
		wpsInputs = constructInputs(execute.getDataInputs());
		wpsRequestedOutputs = constructOutputs(execute.getResponseForm().getResponseDocument());
	}

	private List<WpsRequestedOutput> constructOutputs(ResponseDocumentType responseDocumentType) {
		ArrayList<WpsRequestedOutput> ret = new ArrayList<>();
		if (responseDocumentType != null) {
			for (DocumentOutputDefinitionType outputDefinitionType : responseDocumentType.getOutputArray()) {
				if (!outputDefinitionType.getIdentifier().isNil()) {
					ret.add(new WpsRequestedOutput(id, outputDefinitionType));
				}
			}
		}
		return ImmutableList.copyOf(ret.toArray(new WpsRequestedOutput[0]));
	}
	
	private static ExecuteDocument constructExecuteFromStream(InputStream stream) {
		JacksonXmlModule module = new JacksonXmlModule();
		// and then configure, for example:
		module.setDefaultUseWrapper(false);
		ExecuteDocument executeDoc;
		try {
			executeDoc = ExecuteDocument.Factory.parse(stream);
		} catch (Exception e) {
			throw new RuntimeException("issue constructing ExecuteDocument from xml request", e);
		}
		XmlMapper xmlMapper = new XmlMapper(module);
		xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return executeDoc;
	}

	private List<WpsInput> constructInputs(DataInputsType dataInputs) {
		ArrayList<WpsInput> ret = new ArrayList<WpsInput>();
		if (dataInputs != null) {
			for (InputType inputType : dataInputs.getInputArray()) {
				if (inputType.getData() != null && inputType.getData().getLiteralData() != null) {
					ret.add(new WpsInput(id, inputType));
				}
			}
		}
		return ImmutableList.copyOf(ret.toArray(new WpsInput[0]));
	}

	public String getId() {
		return id;
	}
	
	public String getWpsAlgoIdentifer() {
		return wpsAlgoIdentifer;
	}
	
	public List<WpsInput> getWpsInputs() {
		return wpsInputs;
	}
	
	public List<WpsRequestedOutput> getWpsRequestedOutputs() {
		return wpsRequestedOutputs;
	}
	
	public ExecuteDocument getExecuteDoc() {
		return executeDoc;
	}

}
