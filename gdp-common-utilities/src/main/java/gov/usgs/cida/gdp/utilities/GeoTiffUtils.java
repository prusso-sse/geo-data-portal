package gov.usgs.cida.gdp.utilities;

import gov.usgs.cida.gdp.utilities.exception.GeoTiffUtilException;
import gov.usgs.cida.gdp.utilities.exception.GeoTiffUtilExceptionID;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;

import ucar.ma2.Array;
import ucar.ma2.Range;
import ucar.ma2.Range.Iterator;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.geotiff.GeotiffWriter;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;

public class GeoTiffUtils {
    static private org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(GeoTiffUtils.class);
    
    public static final String GEOTIFF_DIRECTORY_PREFIX = "GeoTiff_";
    public static final String FILE_DATE_FORMAT = "yyyy-MM-dd-HH-mm-ss";
    public static final String FILE_DELIMETER = "-";
    public static final String FILE_TIFF_EXTENSION = ".tiff";
    public static final String FILE_ZIP_EXTENSION = ".zip";
    
    public static File generateGeoTiffZipFromGrid(GridDataset gridDataset, List<String> gridVariableList, Date dateTimeStart, Date dateTimeEnd, String destination) throws GeoTiffUtilException {
        Path destinationDirectory = Paths.get(destination);
        
        CalendarDateFormatter dateFormatter = new CalendarDateFormatter(FILE_DATE_FORMAT);
        
        /*
         * Get the dataset ID and format it accordingly
         *      QUOTE (JIRA GDP-947)
         *          dataId should be what comes after dodsC in the OPeNDAP URI with '/'s replaced with '-'s
         */        
        String datasetURI = gridDataset.getLocationURI();        
        String dataId = datasetURI.replace(OPeNDAPUtils.OPENDAP_PROTO, "");
        dataId = dataId.replace("/", "-");
        
        /*
         * Create a temp directory for this specific request as we could get name collisions
         */
        Path workingDirectory;
        try {
            workingDirectory = Files.createTempDirectory(destinationDirectory, GEOTIFF_DIRECTORY_PREFIX);
        } catch (IOException e) {
            throw new GeoTiffUtilException(GeoTiffUtilExceptionID.GENERAL_EXCEPTION,
                    "GeoTiffUtils", "generateGeoTiffZipFromGrid", "Unable to create GeoTiff working " +
                    "directory at [" + destinationDirectory + "]  Exception: " + e.getMessage());
        }
        
        File result = null;
        String resultingZipFileName = "";
        try {
            for (String gridVariable : gridVariableList) {
                GridDatatype gridDataType;
                try {
                    /*
                     * A null pointer exception can be thrown here due to errors in the
                     * request such as incorrect dimensions (GridVariableList) associated
                     * with a dataset that does not contain them.
                     */
                    gridDataType = gridDataset.findGridDatatype(gridVariable);
                } catch (Exception e) {
                    throw new GeoTiffUtilException(GeoTiffUtilExceptionID.GENERAL_EXCEPTION,
                            "GeoTiffUtils", "generateGeoTiffZipFromGrid", "Unable to generate Grid Data Type " +
                            "for dataset [" + gridDataset.getLocationURI() + "] and variable [" + gridVariable +
                            "].  Exception: " + e.getMessage());
                }
                
                Range timeRange = TimeRangeUtil.generateTimeRange(gridDataType, dateTimeStart, dateTimeEnd);
                
                Iterator iter = timeRange.getIterator();
                
                while (iter.hasNext()) {
                    int tRange = iter.next();
                                    
                    /*
                     * Create the filename with the dataId and time of this iteration
                     *      QUOTE (JIRA GDP-947)
                     *          Each GeoTIFF file should be named with the time stamp converted to a string. 
                     *          As long as its an unambiguous string that someone can parse later, the format
                     *          isn't very important. It would be nice if it was a format that sorts nicely 
                     *          like dataId-YYYY-MM-DD-hh-mm-ss.tiff
                     */
                    CalendarDate date = TimeRangeUtil.getTimeFromRangeIndex(gridDataType, tRange);
                    String dateString = dateFormatter.toString(date);
                    String datasetName = dataId + FILE_DELIMETER + dateString;
                    String absoluteFilePath = workingDirectory + File.separator + datasetName + FILE_TIFF_EXTENSION;
            
                    GeoTiffUtils.createGeoTiffForGrid(gridDataset, gridDataType, tRange, absoluteFilePath);
            
                    LOGGER.debug("testGeoTiffWriter(): Successfully wrote tiff file at: " + absoluteFilePath);
                }
            }
            
            /*
             * Now zip the contents of this geotiff directory
             */
            resultingZipFileName = workingDirectory.toString() + FILE_ZIP_EXTENSION;
            try {
                FileHelper.zipDirectory(workingDirectory.toString(), resultingZipFileName);
                LOGGER.debug("GeoTiffUtils.generateGeoTiffZipFromGrid(): Directory [" + workingDirectory.toString() + "] successfully zipped.");
            } catch (Exception e) {
                throw new GeoTiffUtilException(GeoTiffUtilExceptionID.ZIP_EXCEPTION,
                        "GeoTiffUtils", "generateGeoTiffZipFromGrid", "Unable to zip GeoTiff generated files for directory [" +
                        workingDirectory.toString() + "].  Exception: " + e.getMessage());
            }
            
            result = new File(resultingZipFileName);
        } finally {
            /*
             * Now delete the temporary directory
             */
            try {
                FileUtils.deleteDirectory(workingDirectory.toFile());
            } catch (IOException e) {
                LOGGER.error("Unable to delete GeoTiff directory [" + workingDirectory.toString() + "].");
            }
        }
        
        /*
         * Final check to make sure that the zip file actually exists and then return the
         * File handle to the caller.
         */
        if((result != null) && (result.exists())) {
            return result;
        } else {
            throw new GeoTiffUtilException(GeoTiffUtilExceptionID.GENERAL_EXCEPTION,
                    "GeoTiffUtils", "generateGeoTiffZipFromGrid", "Unable to find resulting GeoTiff zipped file " +
                    resultingZipFileName + "].");
        }
    }
    
    public static void createGeoTiffForGrid(GridDataset gridDataset, GridDatatype grid, int timeIndex, String filename) throws GeoTiffUtilException {
        try {
            Array data = grid.readDataSlice(timeIndex, 0, -1, -1);
            GeotiffWriter writer = new GeotiffWriter(filename);
            writer.writeGrid(gridDataset, grid, data, false);
            writer.close();
        } catch (Exception e) {
            throw new GeoTiffUtilException(GeoTiffUtilExceptionID.GEOTIFFWRITER_EXCEPTION,
                    "GeoTiffUtils", "generateGeoTiffZipFromGrid", "Unable to generate Tiff image from grid [" +
                    gridDataset.getLocationURI() + "].  Exception: " + e.getMessage());
        }
    }
}
