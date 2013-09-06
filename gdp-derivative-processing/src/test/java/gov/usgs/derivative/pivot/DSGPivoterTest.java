package gov.usgs.derivative.pivot;

import gov.usgs.derivative.run.DerivativeOptions;
import org.junit.Test;
import static org.junit.Assert.*;
import ucar.nc2.NetcdfFile;

/**
 *
 * @author jiwalker
 */
public class DSGPivoterTest {

    /**
     * Test of pivot method, of class DSGPivoter.
     */
    @Test
    public void testPivot() throws Exception {
        NetcdfFile nc = null;
        DerivativeOptions options = new DerivativeOptions();
        options.datasetLocation = "/home/jiwalker/mnt/striped/derivatives/Spatial/CONUS_States.dbf/ccsm_a1b-cooling_degree_days,65.0,dsg.nc";
        options.outputFile = "/home/jiwalker/mnt/striped/derivatives/Spatial-pivot/test.nc";
        try {
            nc = NetcdfFile.open(options.datasetLocation);

            new DSGPivoter(nc, options).pivot();
            
        } finally {
            if (nc != null) {
                nc.close();
            }
        }
        
    }
}