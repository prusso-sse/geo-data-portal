package gov.usgs.cida.gdp.wps.parser;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.InputStream;
import java.util.UUID;

import net.opengis.wps.x100.ExecuteDocument;

import org.junit.BeforeClass;
import org.junit.Test;
import org.n52.wps.server.database.domain.WpsInput;
import org.n52.wps.server.database.domain.WpsRequest;
import org.n52.wps.server.database.domain.WpsRequestedOutput;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class DbRequestParsingTest {
	
	private static InputStream reqXML = null;
	
	@BeforeClass
	public static void initTest() {
		reqXML = DbRequestParsingTest.class.getClassLoader().getResourceAsStream("request.xml");
	}
	
	@Test
	public void testXML() {
		assertThat("The xml file should not be empty", reqXML, is(notNullValue()) );
	}
	
	@Test 
	public void testParse() throws Exception {
		
		String requestId = UUID.randomUUID().toString();//this will be created by the processor?
		WpsRequest req = new WpsRequest(requestId, reqXML);
		
		assertThat("requestId is not present", req.getId(), is(notNullValue()));
		assertThat("requestId is not present", req.getId(), is(equalTo(requestId)));
		
		assertThat("algo Id is not present", req.getWpsAlgoIdentifer(), is(notNullValue()));
		assertThat("algo Id is not present", req.getWpsAlgoIdentifer(), equalTo("gov.usgs.cida.gdp.wps.algorithm.FeatureGridStatisticsAlgorithm"));
		
		assertThat("inputs shouldn't be empty", req.getWpsInputs(), is(not(empty())));
		assertThat("inputs should have 11", req.getWpsInputs().size(), is(11));
		WpsInput wpsInput = req.getWpsInputs().get(0);
		assertThat("inputs should have an inputId", wpsInput.getInputId(), is(notNullValue()));
		assertThat("inputs should have a value", wpsInput.getValue(), is(notNullValue()));
		assertThat("wps req id should match passed in value", wpsInput.getWpsRequestId(), is(requestId));
		assertThat("inputs should have an id", wpsInput.getId(), is(notNullValue()));
		
		assertThat("outputs shouldn't be empty", req.getWpsRequestedOutputs(), is(not(empty())));
		assertThat("outputs should have 1", req.getWpsRequestedOutputs().size(), is(1));
		WpsRequestedOutput wpsRequestedOutput = req.getWpsRequestedOutputs().get(0);
		assertThat("wps req id should match passed in value", wpsRequestedOutput.getWpsRequestId(), is(requestId));
		assertThat("output should have values", wpsRequestedOutput.getValue(), is(notNullValue()));
		assertThat("output should have values", wpsRequestedOutput.getValue(), equalTo("OUTPUT"));
	}
	

}
