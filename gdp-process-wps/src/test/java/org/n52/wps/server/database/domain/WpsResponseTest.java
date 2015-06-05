package org.n52.wps.server.database.domain;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import net.opengis.wps.x100.ExecuteResponseDocument;
import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.XmlException;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.*;

/**
 *
 * @author jiwalker
 */
public class WpsResponseTest {
	
	private static InputStream acceptedXML = null;
	private static InputStream succeededXML = null;
	
	@BeforeClass
	public static void initTest() {
		acceptedXML = WpsResponseTest.class.getClassLoader().getResourceAsStream("accepted.xml");
		succeededXML = WpsResponseTest.class.getClassLoader().getResourceAsStream("succeeded.xml");
	}
	
	@AfterClass
	public static void cleanupTest() {
		IOUtils.closeQuietly(acceptedXML);
		IOUtils.closeQuietly(succeededXML);
	}
	
	@Test
	public void testAccepted() throws XmlException, IOException {
		String id = UUID.randomUUID().toString();
		WpsResponse wpsResponse = new WpsResponse(id, ExecuteResponseDocument.Factory.parse(acceptedXML));
		
		assertThat("id is not null", wpsResponse.getId(), is(not(nullValue())));
		assertThat("request id is the same as passed into constructor", wpsResponse.getWpsRequestId(), is(equalTo(id)));
		assertThat("creation time is same as in document", wpsResponse.getCreationTime().compareTo(new DateTime("2015-04-08T16:16:34.177-05:00")), is(equalTo(0)));
		assertThat("process id is the algorithm identifier", wpsResponse.getWpsAlgoIdentifer(), is(equalTo("gov.usgs.cida.gdp.wps.algorithm.FeatureGridStatisticsAlgorithm")));
		assertThat("process status is accepted", wpsResponse.getStatus(), is(equalTo(WpsStatus.ACCEPTED)));
		
		assertThat("process outputs are empty", wpsResponse.getOutputs(), is(empty()));
		assertThat("process endtime is null", wpsResponse.getEndTime(), is(nullValue()));
		assertThat("process percent complete is null", wpsResponse.getPercentComplete(), is(nullValue()));
	}
	
	@Test
	public void testSucceeded() throws XmlException, IOException {
		String id = UUID.randomUUID().toString();
		WpsResponse wpsResponse = new WpsResponse(id, ExecuteResponseDocument.Factory.parse(succeededXML));
		
		assertThat("id is not null", wpsResponse.getId(), is(not(nullValue())));
		assertThat("request id is the same as passed into constructor", wpsResponse.getWpsRequestId(), is(equalTo(id)));
		assertThat("creation time is same as in document", wpsResponse.getCreationTime().compareTo(new DateTime("2014-09-03T15:09:09.334-05:00")), is(equalTo(0)));
		assertThat("creation time is same as in document", wpsResponse.getStartTime(), is(nullValue()));
		assertThat("process id is the algorithm identifier", wpsResponse.getWpsAlgoIdentifer(), is(equalTo("gov.usgs.cida.gdp.wps.algorithm.FeatureWeightedGridStatisticsAlgorithm")));
		assertThat("process status is accepted", wpsResponse.getStatus(), is(equalTo(WpsStatus.SUCCEEDED)));
		
		assertThat("process outputs are empty", wpsResponse.getOutputs().size(), is(1));
		assertThat("process endtime is null", wpsResponse.getEndTime(), is(nullValue()));
		assertThat("process percent complete is null", wpsResponse.getPercentComplete(), is(nullValue()));
	}
}
