package gov.usgs.cida.gdp.wps.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;


/**
 *
 * @author tkunicki
 */
//@Ignore
public class WCSUtilFunctonalTest {
	private static String tmpDirectoryName = "./working";

    public ReferencedEnvelope testEnvelope = new ReferencedEnvelope(-90.05, -89.95, 44.95, 45.05, DefaultGeographicCRS.WGS84);
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        // Create temporary file directory
    	File tmpDir = new File(tmpDirectoryName);
    	if(!tmpDir.exists()) {
    		
    		try {
    			tmpDir.mkdir();
    		} catch (Exception e) {
    			org.junit.Assert.fail();
    		}
    	}
        
    	if(!tmpDir.exists()) {
    		org.junit.Assert.fail();
    	}
    }
    
    @AfterClass
    public static void cleanup() {
    	File tmpDir = new File(tmpDirectoryName);
    	if(tmpDir.exists()) {
    		try {
				Files.delete(tmpDir.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    }

    @Test
    @Ignore
    public void testArcServer_EROS_NED() throws URISyntaxException {
        // this endpoint may be dead
        File f = WCSUtil.generateTIFFFile(
                new URI("http://raster.nationalmap.gov/arcgis/services/LandCover/USGS_EROS_LandCover_NLCD/MapServer/WCSServer"),
                "1",
                testEnvelope,
                true, tmpDirectoryName);
        
        if(f.exists()) {
        	f.delete();
        } else {
        	org.junit.Assert.fail();
        }
    }

    @Test
    @Ignore
    public void testGeoServer_CIDA_NED() throws URISyntaxException, IOException {
    	/**
    	 * As of 6/30/14 we know this does not work due to server not existing
    	 */
        // this endpoint may be dead
        File f = WCSUtil.generateTIFFFile(
                new URI("http://igsarmewmaccave.gs.doi.net:8082/geoserver/wcs"),
                "sample:ned-sample",
                testEnvelope,
                true, tmpDirectoryName);
        
        if(f.exists()) {
        	f.delete();
        } else {
        	org.junit.Assert.fail();
        }
    }

    @Test
    @Ignore
    public void testGeoServer_CIDA_NEDMosaic() throws URISyntaxException, IOException {
    	/**
    	 * As of 6/30/14 we know this does not work due to server not existing
    	 */
        // this endpoint may be dead
        File f = WCSUtil.generateTIFFFile(
                new URI("http://igsarmewmaccave.gs.doi.net:8082/geoserver/wcs"),
                "sample:ned-mosaic",
                testEnvelope,
                true, tmpDirectoryName);
        
        if(f.exists()) {
        	f.delete();
        } else {
        	org.junit.Assert.fail();
        }
    }

    @Test
    @Ignore
    public void testArcServer_CIDA_NLCD2006() throws URISyntaxException {

        File f = WCSUtil.generateTIFFFile(
                new URI("http://raster.nationalmap.gov/arcgis/services/LandCover/USGS_EROS_LandCover_NLCD/MapServer/WCSServer"),
                "1",
                testEnvelope,
                true, tmpDirectoryName);
        
        if(f.exists()) {
        	f.delete();
        } else {
        	org.junit.Assert.fail();
        }
    }

    @Test
    @Ignore
    public void testArcServer_EROS_NLCD2001() throws URISyntaxException {
        // this endpoint may be dead
        File f = WCSUtil.generateTIFFFile(
                new URI("http://raster.nationalmap.gov/arcgis/services/LandCover/USGS_EROS_LandCover_NLCD/MapServer/WCSServer"),
                "2",
                testEnvelope,
                true, tmpDirectoryName);
        
        if(f.exists()) {
        	f.delete();
        } else {
        	org.junit.Assert.fail();
        }
    }
    
    @Test
    @Ignore
    public void testArcServer_ScienceBase_TIFF() throws URISyntaxException {
    	/**
    	 * As of 6/30/14 we know this does not work due to server not existing
    	 */
        // this endpoint may be dead
        File f = WCSUtil.generateTIFFFile(
                new URI("http://my-beta.usgs.gov/catalogMaps/mapping/ows/4f4e4799e4b07f02db48f9dd"),
                "ucrb_nlcd1992_all5states_utmzone12.tif",
                new ReferencedEnvelope(-109.51, -109.49, 38.49, 38.51, DefaultGeographicCRS.WGS84),
                true, tmpDirectoryName);
        
        if(f.exists()) {
        	f.delete();
        } else {
        	org.junit.Assert.fail();
        }
    }
}
