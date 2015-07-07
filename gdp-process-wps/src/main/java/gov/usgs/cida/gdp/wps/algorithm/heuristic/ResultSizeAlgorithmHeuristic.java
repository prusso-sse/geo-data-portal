package gov.usgs.cida.gdp.wps.algorithm.heuristic;

import gov.usgs.cida.gdp.coreprocessing.analysis.grid.GridUtility;
import gov.usgs.cida.gdp.utilities.OPeNDAPUtils;
import gov.usgs.cida.gdp.wps.algorithm.GDPAlgorithmUtil;
import gov.usgs.cida.gdp.wps.algorithm.heuristic.exception.AlgorithmHeuristicException;
import gov.usgs.cida.gdp.wps.algorithm.heuristic.exception.AlgorithmHeuristicExceptionID;

import java.util.Date;
import java.util.List;

import org.geotools.feature.FeatureCollection;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;

/**
 * This Heuristic provides a means in determining the resulting size of a NetCDF
 * request.  Based on the dimensions, variables and filters requested, a NetCDF
 * result can be Gigabytes in size.  This Heuristic will compute an estimate of
 * the resulting NetCDF result size and return true or false if it violates the
 * global response maximum size.
 * @author prusso
 *
 */
public class ResultSizeAlgorithmHeuristic implements AlgorithmHeuristic {
	static private org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ResultSizeAlgorithmHeuristic.class);
	
	public static final long MAXIMUM_DATA_SET_SIZE = 524288000;		// 500MB
	private long maximumSizeConfigured = MAXIMUM_DATA_SET_SIZE;
	
	private GridDataset gridDataset;
    private List<String> gridVariableList;
    private FeatureCollection<?, ?> featureCollection;
    private Date dateTimeStart;
    private Date dateTimeEnd;
    private boolean requireFullCoverage;
    private String result = "";
    private long resultingSize = 0;
    
    public ResultSizeAlgorithmHeuristic() {
    	this.gridDataset = null;
    	this.gridVariableList = null;
    	this.featureCollection = null;
    	this.dateTimeStart = null;
    	this.dateTimeEnd = null;
    	this.requireFullCoverage = false;
    }
    
    public ResultSizeAlgorithmHeuristic(List<String> gridVariableList, FeatureCollection<?, ?> featureCollection,
    		Date dateTimeStart, Date dateTimeEnd, boolean requireFullCoverage) {
    	this.gridDataset = null;
    	this.gridVariableList = gridVariableList;
    	this.featureCollection = featureCollection;
    	this.dateTimeStart = dateTimeStart;
    	this.dateTimeEnd = dateTimeEnd;
    	this.requireFullCoverage = requireFullCoverage;
    }
    
    public ResultSizeAlgorithmHeuristic(GridDataset gridDataset, List<String> gridVariableList, FeatureCollection<?, ?> featureCollection,
    		Date dateTimeStart, Date dateTimeEnd, boolean requireFullCoverage) {
    	this.gridDataset = gridDataset;
    	this.gridVariableList = gridVariableList;
    	this.featureCollection = featureCollection;
    	this.dateTimeStart = dateTimeStart;
    	this.dateTimeEnd = dateTimeEnd;
    	this.requireFullCoverage = requireFullCoverage;
    }
    
	@Override
	public boolean validated() throws AlgorithmHeuristicException {
		isInitialized();
		
		String timeName = "";
		Range timeRange = null;
		
		String longName = "";
		Range xLngRange = null;		// Assuming that Range[0] (which is explicitly stated as the X coordinate) is the longitude
		
		String latName = "";
		Range yLatRange = null;		// Assuming that Range[1] (which is explicitly stated as the Y coordiante) is the latitude
		
		resultingSize = 0;
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
				this.result = "General Exception thrown";
				String msg = this.result + ":\n" + e.getMessage();
				throw new AlgorithmHeuristicException(AlgorithmHeuristicExceptionID.GENERAL_EXCEPTION, "ResultSizeAlgorithmHeuristic", "validated", msg);
			}
			
            GridCoordSystem gridCoordSystem;
			try {
				gridCoordSystem = gridDataType.getCoordinateSystem();
			} catch (Exception e) {
				this.result = "General Exception thrown";
				String msg = this.result + ":\n" + e.getMessage();
				throw new AlgorithmHeuristicException(AlgorithmHeuristicExceptionID.GENERAL_EXCEPTION, "ResultSizeAlgorithmHeuristic", "validated", msg);
			}

            // generate sub-set
            Range tRange = GDPAlgorithmUtil.generateTimeRange(gridDataType, dateTimeStart, dateTimeEnd);
            
            /*
             * Save the time range or inspect that it hasnt changed
             */
            if(!inspectRange(tRange, timeRange)) {
            	try {
            		timeRange = new Range(tRange.first(), tRange.last());
            		timeName = gridDataType.getCoordinateSystem().getTimeAxis1D().getShortName();
                } catch (Exception e) {
                	this.result = "General Exception thrown";
    				String msg = this.result + ":\n" + e.getMessage();
    				throw new AlgorithmHeuristicException(AlgorithmHeuristicExceptionID.GENERAL_EXCEPTION, "ResultSizeAlgorithmHeuristic", "validated", msg);
                }
            }
            
            Range[] xyRanges;
			try {
				xyRanges = GridUtility.getXYRangesFromBoundingBox(featureCollection.getBounds(), gridCoordSystem, requireFullCoverage);
			} catch (InvalidRangeException e) {
				this.result = "InvalidRangeException thrown";
				String msg = this.result + ":\n" + e.getMessage();
				throw new AlgorithmHeuristicException(AlgorithmHeuristicExceptionID.GDP_GRID_UTILITY_EXCEPTION, "ResultSizeAlgorithmHeuristic", "validated", msg);
			} catch (TransformException e) {
				this.result = "TransformException thrown";
				String msg = this.result + ":\n" + e.getMessage();
				throw new AlgorithmHeuristicException(AlgorithmHeuristicExceptionID.GDP_GRID_UTILITY_EXCEPTION, "ResultSizeAlgorithmHeuristic", "validated", msg);
			} catch (FactoryException e) {
				this.result = "FactoryException thrown";
				String msg = this.result + ":\n" + e.getMessage();
				throw new AlgorithmHeuristicException(AlgorithmHeuristicExceptionID.GDP_GRID_UTILITY_EXCEPTION, "ResultSizeAlgorithmHeuristic", "validated", msg);
			}
			
			/*
             * Save the longitude range or inspect that it hasnt changed
             */
			if(!inspectRange(xyRanges[0], xLngRange)) {
            	try {
            		xLngRange = new Range(xyRanges[0].first(), xyRanges[0].last());
            		longName = gridCoordSystem.getXHorizAxis().getShortName();
                } catch (Exception e) {
                	this.result = "General Exception thrown";
    				String msg = this.result + ":\n" + e.getMessage();
    				throw new AlgorithmHeuristicException(AlgorithmHeuristicExceptionID.GENERAL_EXCEPTION, "ResultSizeAlgorithmHeuristic", "validated", msg);
                }
            }
			
			/*
             * Save the latitude range or inspect that it hasnt changed
             */
			if(!inspectRange(xyRanges[1], yLatRange)) {
            	try {
            		yLatRange = new Range(xyRanges[1].first(), xyRanges[1].last());
            		latName = gridCoordSystem.getYHorizAxis().getShortName();
                } catch (Exception e) {
                	this.result = "General Exception thrown";
    				String msg = this.result + ":\n" + e.getMessage();
    				throw new AlgorithmHeuristicException(AlgorithmHeuristicExceptionID.GENERAL_EXCEPTION, "ResultSizeAlgorithmHeuristic", "validated", msg);
                }
            }
			
            
			try {
				gridDataType = gridDataType.makeSubset(null, null, tRange, null, xyRanges[1], xyRanges[0]);
			} catch (InvalidRangeException e) {
				this.result = "InvalidRangeException thrown";
				String msg = this.result + ":\n" + e.getMessage();
				throw new AlgorithmHeuristicException(AlgorithmHeuristicExceptionID.GENERAL_EXCEPTION, "ResultSizeAlgorithmHeuristic", "validated", msg);
			}
			

            Variable gridV = (Variable) gridDataType.getVariable();
            
            /*
             * OPeNDAP Dimension Size Heuristic
             * 
             * DIM1.count * DIM2.count * DIMn.count * VARIABLE.bytesize
             */
            List<Dimension> dimensions = gridV.getDimensions(); // looks like [time = 366;, latitude = 4;, longitude = 5;]
            long totalVectors = 1;
            for(Dimension dim : dimensions) {
            	totalVectors *= dim.getLength();
            }
            DataType dataType = gridV.getDataType();
            String datatype = gridV.getDataType().name();
            long totalVectorSize = totalVectors * dataType.getSize();
            resultingSize += totalVectorSize;
            LOGGER.debug("\n\nOPeNDAP HEURISTIC INFO:\n" +
            			 "\tDimensions:         \t" + dimensions + "\n" +
            			 "\tDimension Size:     \t" + totalVectors + "\n" +
            			 "\tDatatype:           \t" + datatype + "\n" +
            			 "\tTotal Variable Size:\t" + totalVectorSize + "\n\n");
            /*
             * ************************************************************
             */
        }
        
        LOGGER.debug("\nTOTAL SIZE FOR ALGORITHM DATASET WITH " + gridVariableList.size() + " VARIABLES:\n" +
        			 "\tTotal Data Size:    \t" + resultingSize);
        
        
        if(resultingSize >= maximumSizeConfigured) {
        	/*
        	 * Retrieve the OPeNDAP URL for this request
        	 */
        	String openDapURL = OPeNDAPUtils.generateOpenDapURL(gridDataset.getLocationURI(), gridVariableList, gridDataset.getNetcdfFile().getVariables(), timeRange, yLatRange, xLngRange);
        	
        	this.result = "Estimated Data Size [" + resultingSize + " bytes] is greater than allowed maximum [" + 
        				  maximumSizeConfigured + " bytes].  The following URI can be used with the nccopy tool " +
        				  "to create a local copy of the data in the NetCDF4 format. See the Geo Data Portal " +
        				  "documentation for more information: " + openDapURL;
        	return false;
        }
        
		return true;
	}

	@Override
	public String getError() {
		return this.result;
	}
	
	public long getResultingSize() {
		return resultingSize;
	}
	
	public long getMaximumSizeConfigured() {
		return maximumSizeConfigured;
	}

	public void setMaximumSizeConfigured(long maximumSizeConfigured) {
		this.maximumSizeConfigured = maximumSizeConfigured;
	}

	public void setGridDataset(GridDataset gridDataSet) {
		this.gridDataset = gridDataSet;
	}
	
	public void setGridVariableList(List<String> gridVariableList) {
		this.gridVariableList = gridVariableList;
	}

	public void setFeatureCollection(FeatureCollection<?, ?> featureCollection) {
		this.featureCollection = featureCollection;
	}

	public void setDateTimeStart(Date dateTimeStart) {
		this.dateTimeStart = dateTimeStart;
	}

	public void setDateTimeEnd(Date dateTimeEnd) {
		this.dateTimeEnd = dateTimeEnd;
	}

	public void setRequireFullCoverage(boolean requireFullCoverage) {
		this.requireFullCoverage = requireFullCoverage;
	}

	private boolean isInitialized() throws AlgorithmHeuristicException {
		if (this.gridDataset == null) {
			throw new AlgorithmHeuristicException(AlgorithmHeuristicExceptionID.UNINITIALIZED_EXCEPTION, "ResultSizeAlgorithmHeuristic", "validated", "GridDataSet has not been initialized!");
		}
		if (this.gridVariableList == null) {
			throw new AlgorithmHeuristicException(AlgorithmHeuristicExceptionID.UNINITIALIZED_EXCEPTION, "ResultSizeAlgorithmHeuristic", "validated", "GridVariableList has not been initialized!");
		}
		if (this.featureCollection == null) {
			throw new AlgorithmHeuristicException(AlgorithmHeuristicExceptionID.UNINITIALIZED_EXCEPTION, "ResultSizeAlgorithmHeuristic", "validated", "FeatureCollection has not been initialized!");
		}
		if (this.dateTimeStart == null) {
			throw new AlgorithmHeuristicException(AlgorithmHeuristicExceptionID.UNINITIALIZED_EXCEPTION, "ResultSizeAlgorithmHeuristic", "validated", "DateTimeStart has not been initialized!");
		}			
		if (this.dateTimeEnd == null) {
			throw new AlgorithmHeuristicException(AlgorithmHeuristicExceptionID.UNINITIALIZED_EXCEPTION, "ResultSizeAlgorithmHeuristic", "validated", "DateTimeEnd has not been initialized!");
		}
		
		return true;
	}
	
	/**
	 * This method will compare 2 ranges, an original range and a clone range that is to be
	 * representative of the original.
	 * <br/><br/>
	 * If the cloned range is null this will return false.
	 * <br/><br/>
	 * If the cloned range is the same as the original, it will return true.
	 * <br/><br/>
	 * If the cloned range is not null, it will check to make sure that the clone
	 * does indeed have the same value as the original.  If it does not, it will throw
	 * an AlgorithmHeuristicException.
	 * @param original
	 * @param clone
	 * @throws AlgorithmHeuristicException
	 */
	private boolean inspectRange(Range original, Range clone) throws AlgorithmHeuristicException {
		if(clone == null) {
        	return false;
        } else {
        	/*
        	 * We need to see if the range has changed.  If it has (which it shouldnt) we have an unrecoverable error.
        	 */
        	if((original.first() != clone.first()) || (original.last() != clone.last())) {
        		String msg = "General Exception thrown" + ":\nA dimensional range has changed between variables.";
				throw new AlgorithmHeuristicException(AlgorithmHeuristicExceptionID.GENERAL_EXCEPTION, "ResultSizeAlgorithmHeuristic", "updateRange", msg);
        	}
        }
		
		return true;
	}

}
