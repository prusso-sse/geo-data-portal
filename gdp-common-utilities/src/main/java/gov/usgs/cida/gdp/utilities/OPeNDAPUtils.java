package gov.usgs.cida.gdp.utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ucar.ma2.Array;
import ucar.ma2.Range;
import ucar.nc2.Attribute;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.VariableDS;

public class OPeNDAPUtils {
	public enum OPeNDAPContentType {
		ASCII, BINARY, NONE;

		public static OPeNDAPContentType getTypeFromString(String string) {
			if (string.equals("ASCII")) {
				return ASCII;
			}

			if (string.equals("BINARY")) {
				return BINARY;
			}

			return NONE;
		}

		public static String getStringFromType(OPeNDAPContentType type) {
			switch (type) {
				case ASCII: {
					return "ASCII";
				}

				case BINARY: {
					return "BINARY";
				}

				default: {
					return "NONE";
				}
			}
		}
		
		public static String getContentTypeString(OPeNDAPContentType type) {
			switch (type) {
			case ASCII: {
				return ".ascii";
			}

			case BINARY: {
				return ".dods";
			}

			default: {
				return "";
			}
		}
		}
	}
	
	public static final int RANGE_STRIDE = 1;
	public static final String RANGE_DELIMETER = ":";
	public static final String URI_DELIMETER = ",";
	public static final String OPENDAP_PROTO = "dods:";
	public static final String REQUEST_PROTO = "http:";
	public static final List<String> REQUIRED_ATTRIBUTES = Arrays.asList("grid_mapping");
	
	/**
	 * With the given information, will return an OPeNDAP http request.
	 * 
	 * http://cida.usgs.gov/thredds/dodsC/new_gmo.ascii?longitude[400:1:406],latitude[20:1:22],time[2854:1:2941],pr[2854:1:2941][20:1:22][400:1:406],tas[2854:1:2941][20:1:22][400:1:406],tasmax[2854:1:2941][20:1:22][400:1:406],tasmin[2854:1:2941][20:1:22][400:1:406],wind[2854:1:2941][20:1:22][400:1:406]
	 * Range works like:
	 * 		[start, stride, last]
	 * 		where last is INCLUSIVE!
	 * 
	 * This method always assumes a range stride of 1.
	 * 
	 * @param datasetURI
	 * @param gridVariableList
	 * @param timeRange
	 * @param yRange
	 * @param xRange
	 * @return
	 */
	public static String generateOpenDapURL(String datasetURI, List<String> requestedVariableList, List<?> gridVariableList, Range timeRange, Range yRange, Range xRange) {
		return generateOpenDapURL(datasetURI, requestedVariableList, gridVariableList, timeRange, yRange, xRange, OPeNDAPContentType.NONE);
	}
	
	/**
	 * With the given information, will return an OPeNDAP http request.
	 * 
	 * http://cida.usgs.gov/thredds/dodsC/new_gmo.ascii?longitude[400:1:406],latitude[20:1:22],time[2854:1:2941],pr[2854:1:2941][20:1:22][400:1:406],tas[2854:1:2941][20:1:22][400:1:406],tasmax[2854:1:2941][20:1:22][400:1:406],tasmin[2854:1:2941][20:1:22][400:1:406],wind[2854:1:2941][20:1:22][400:1:406]
	 * Range works like:
	 * 		[start, stride, last]
	 * 		where last is INCLUSIVE!
	 * 
	 * This method always assumes a range stride of 1.
	 * 
	 * @param datasetURI
	 * @param gridVariableList
	 * @param timeName
	 * @param timeRange
	 * @param latitudeName
	 * @param yRange
	 * @param longitudeName
	 * @param xRange
	 * @param contentType
	 * @return
	 */
	public static String generateOpenDapURL(String datasetURI, List<String> requestedVariableList, List<?> gridVariableList, Range timeRange, Range yRange, 
			Range xRange, OPeNDAPContentType contentType) {
		StringBuffer result = new StringBuffer();
		
		/*
		 * Need Data Set URI (variable datasetURI)
		 * 		Example:	dods://cida.usgs.gov/thredds/dodsC/new_gmo
		 * 
		 * Need Set Variable List (variable datasetId)
		 * 		Example:	[pr, tas, tasmax, tasmin, wind]
		 * 
		 * Need Time Dimension
		 * 		Example:	 [0,22644]
		 * 
		 * Need Latitude Dimension
		 * 		Example:	[0,221]
		 * 
		 * Need Longitude Dimension
		 * 		Example:	[0,461]
		 * 
		 */
		
		/*
		 * First, make sure we format the URI correctly.  Get rid of the dods
		 * protocol descriptor and replace with http.
		 */
		String requestURL = datasetURI.replace(OPENDAP_PROTO, REQUEST_PROTO);
		result.append(requestURL);
		
		/*
		 * Determine the response type for the data
		 * 
		 * Update 070115
		 * 		Blodgett does not want a content type.  Is inforcing an OPeNDAP
		 * 		client URI.  Keeping this in here in case we ever want to offer
		 * 		a workable URL.
		 */
		result.append(OPeNDAPContentType.getContentTypeString(contentType));
		
		// Append parameter declarator
		result.append("?");
		
		/*
		 * Create longitude range
		 */
		String xRangeString = "[" + xRange.first() + RANGE_DELIMETER + RANGE_STRIDE + RANGE_DELIMETER + xRange.last() + "]";
		
		/*
		 * Create latitude range
		 */
		String yRangeString = "[" + yRange.first() + RANGE_DELIMETER + RANGE_STRIDE + RANGE_DELIMETER + yRange.last() + "]";
		
		/*
		 * Create time range
		 */
		String timeRangeString = "[" + timeRange.first() + RANGE_DELIMETER + RANGE_STRIDE + RANGE_DELIMETER + timeRange.last() + "]";
		
		/*
		 * Loop through the request variables and append them to the URL.
		 * 
		 * Variable ranges include all 3 dimension ranges:
		 * 		variable[z][y][x]
		 * 
		 * 
		 * 		1.	Loop through requestedVariableList
		 * 		2.	For each variable, look up VariableDS in gridVariableList
		 * 		3.	If VariableDS has associated variable, include that variable as well
		 * 		
		 */
		int size = gridVariableList.size();
		int lastIndex = size - 1;
		List<String> attributeVariablesUsed = new ArrayList<String>();
		for (int i = 0; i < gridVariableList.size(); i++) {
			boolean variableUsed = false;
			
			Object variable = gridVariableList.get(i);
			
			/*
			 * First check to see if this is a dimension.  If so, which axis
			 */
			if(variable instanceof CoordinateAxis1D) {
				String varName = ((VariableDS) variable).getShortName();
							
				switch(((CoordinateAxis1D) variable).getAxisType()) {
					case GeoX:
					case Lon: {
						/*
						 * This is the x axis which corresponds to the Longitude
						 */
						result.append(varName + timeRangeString + yRangeString + xRangeString);
						variableUsed = true;
						break;
					}
					case GeoY:
					case Lat: {
						/*
						 * This is the y axis which corresponds to the Latitude
						 */
						result.append(varName + timeRangeString + yRangeString + xRangeString);
						variableUsed = true;
						break;
					}
					case Time: {
						/*
						 * This is the time axis
						 */
						result.append(varName + timeRangeString + yRangeString + xRangeString);
						variableUsed = true;
						break;
					}
					default: {
						// do nothing
					}
				}
			} else if(variable instanceof VariableDS ) {
				String varName = ((VariableDS) variable).getShortName();
				
				/*
				 * Now lets see if this variable name exists in the requestedVariableList
				 */
				if(requestedVariableList.contains(varName)) {					
					/*
					 * First, we have a variable that has been requested in the original
					 * post.  Lets see if it has any dependent attributes associated
					 * with it.
					 */
					for(Attribute attribute : ((VariableDS) variable).getAttributes()) {
						if(REQUIRED_ATTRIBUTES.contains(attribute.getFullName())) {
							Array values = attribute.getValues();
							
							/*
							 * Lets loop through the attribute values.  We only care
							 * about strings to put on the URI.
							 */
							for(int j = 0; j < values.getSize(); j++) {
								Object value = values.getObject(j);
								
								if(value instanceof String) {
									String stringValue = (String)value;
									
									/*
									 * Now lets check to see if we've seen this attribute
									 * value.  If we have, continue.  If not, we need
									 * to add it to our URI.
									 */
									if(!attributeVariablesUsed.contains(stringValue)) {
										attributeVariablesUsed.add(stringValue);
										result.append(stringValue + URI_DELIMETER);
									}
								}
							}
						}
					}
					
					/*
					 * Finally, append the main variable
					 */
					result.append(varName + timeRangeString + yRangeString + xRangeString);
					
					variableUsed = true;
				}
			}
			
			/*
			 * Lets see if we need to append a delimeter
			 */
			if(variableUsed) {
				if(i < lastIndex) {
					result.append(URI_DELIMETER);
				}
			}
		}
		
		/*
		 * We have an issue where we might have appended a URI_DELIMETER to the URI when it
		 * was the last variable to be used but NOT the last variable in the list we iterate
		 * over.  This means we could have a hanging delimeter.  As far as I can tell, there
		 * really is no good way to deal with it programatically as we don't know if we will
		 * use all the variables in the variable list or not.  Lets just check here and
		 * remove it.
		 */
		String uri = result.toString();
		if(uri.indexOf(URI_DELIMETER, (uri.length() - 1)) != -1) {
			uri = uri.substring(0, uri.length() - 1);
		}
		
		return uri;
	}
}
