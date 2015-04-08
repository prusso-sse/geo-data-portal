package gov.usgs.cida.gdp.wps.parser;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.InputStream;

import org.junit.BeforeClass;
import org.junit.Test;
import org.n52.wps.server.database.domain.WpsRequest;

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
		
		JacksonXmlModule module = new JacksonXmlModule();
		// and then configure, for example:
		module.setDefaultUseWrapper(false);
		XmlMapper xmlMapper = new XmlMapper(module);
		xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		// and you can also configure AnnotationIntrospectors etc here:

		WpsRequest req = xmlMapper.readValue(reqXML, WpsRequest.class);
		assertThat("requestId is not present", req.getId(), is(notNullValue()));
		
		assertThat("algo Id is not present", req.getWpsAlgoIdentifer(), is(notNullValue()));
		assertThat("algo Id is not present", req.getWpsAlgoIdentifer(), equalTo("gov.usgs.cida.gdp.wps.algorithm.FeatureGridStatisticsAlgorithm"));
				
	}
	

}
