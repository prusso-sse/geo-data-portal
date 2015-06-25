package gov.usgs.cida.gdp.wps.algorithm.heuristic;

import static org.junit.Assert.fail;
import gov.usgs.cida.gdp.wps.algorithm.heuristic.exception.AlgorithmHeuristicException;
import gov.usgs.cida.gdp.wps.algorithm.heuristic.exception.AlgorithmHeuristicExceptionID;

import java.net.URISyntaxException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ResultSizeAlgorithmHeuristicTest {
	
	@Before
    public void init() throws URISyntaxException {}
	
	@After
	public void destroy() throws Exception {}
		
	@Test
	public void testResultSizeAlgorithmHeuristicConstructor() {
		ResultSizeAlgorithmHeuristic resultSizeHeuristic = new ResultSizeAlgorithmHeuristic();
		
		assert(resultSizeHeuristic.getError().equals(""));
		
		try {
			resultSizeHeuristic.validated();
			fail("AlgorithmHeuristic.validated() did not throw expected exception.");
		} catch (AlgorithmHeuristicException e) {
			assert(e.getExceptionid().value() == AlgorithmHeuristicExceptionID.UNINITIALIZED_EXCEPTION.value());
		}
	}
	
}
