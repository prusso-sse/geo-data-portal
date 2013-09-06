package gov.usgs.derivative.pivot;

import gov.usgs.derivative.run.DerivativeOptions;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.joda.time.DateTime;
import org.joda.time.Days;
import ucar.ma2.Array;
import ucar.ma2.ArrayStructure;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.StructureMembers;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;

public class DSGPivoter {

    public static final String STATION = "station";
    public static final String TIME = "time";
    public static final String MEAN = "mean";
    public static final String RECORD = "record";
    public static final String INDEX = "index";
    
    private final NetcdfFile ncInput;
    private final Variable oVariable;
    private final DerivativeOptions options;
    
    public DSGPivoter(NetcdfFile netCDFFile, DerivativeOptions options) {
        this.ncInput = netCDFFile;
        this.oVariable = netCDFFile.findVariable(RECORD);
        this.options = options;
    }
    
    public void pivot() throws IOException, InvalidRangeException {
        long start = System.currentTimeMillis();
        
        BufferedWriter writer = null;
        try {
        
            DSGPivoter.ReadObserationsVisitor visitor = new DSGPivoter.ReadObserationsVisitor();
            new RaggedIndexArrayStructureObservationTraverser(oVariable).traverse(visitor);
            Map<Integer, List<Double>> observationMap = visitor.getObservationMap();
            
            System.out.println(
                    "Station Count: " + visitor.stationCount + 
                    " : TimeCount " + visitor.stationTimeCount +
                    " : RecordCount " + visitor.recordCount
                    );
            System.out.println((System.currentTimeMillis() - start) + "ms");
            
            generatePivotFile(observationMap, visitor.stationCount, visitor.stationTimeCount, visitor.recordCount);
        } finally {
            if (writer != null) try { writer.close(); } catch (Exception e) {}
        }
    }
    
    
    protected void generatePivotFile(Map<Integer, List<Double>> observationMap, int stationCount, int timeCount, int recordCount) throws IOException, InvalidRangeException {
        NetcdfFileWriter ncWriter = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, options.outputFile);
        
        Dimension nStationDim = ncWriter.addDimension(null, STATION, stationCount);
        // 59 is the largest station size for derivatives, parameterize?
        Dimension nStationIdLenDim = ncWriter.addDimension(null, "station_id_len", 59);
        Dimension nTimeDim = ncWriter.addDimension(null, TIME, timeCount);
        
        ArrayStructure array = (ArrayStructure) oVariable.read(new int[]{0}, new int[]{0});
        StructureMembers.Member mIndex = array.findMember(INDEX);
        StructureMembers.Member mTime = array.findMember(TIME);
        StructureMembers.Member mMean = array.findMember(MEAN);
        
        Variable nStationIdVar = ncWriter.addVariable(null, "station_id", DataType.CHAR, Arrays.asList(nStationDim, nStationIdLenDim));
        nStationIdVar.addAttribute(new Attribute(CF.STANDARD_NAME, CF.STATION_ID));
        nStationIdVar.addAttribute(new Attribute(CF.CF_ROLE, CF.TIMESERIES_ID));

        Variable nTimeVar = ncWriter.addVariable(null, TIME, DataType.INT, Arrays.asList(nTimeDim));
        nTimeVar.addAttribute(new Attribute(CF.STANDARD_NAME, TIME));
        nTimeVar.addAttribute(new Attribute(CDM.UNITS, mTime.getUnitsString()));
        nTimeVar.addAttribute(new Attribute(CF.CALENDAR, "gregorian"));
        
        Variable nLatVar = ncWriter.addVariable(null, "lat", DataType.FLOAT, Arrays.asList(nStationDim));
        nLatVar.addAttribute(new Attribute(CF.STANDARD_NAME, "latitude"));
        nLatVar.addAttribute(new Attribute(CDM.UNITS, CDM.LAT_UNITS));
        
        Variable nLonVar = ncWriter.addVariable(null, "lon", DataType.FLOAT, Arrays.asList(nStationDim));
        nLonVar.addAttribute(new Attribute(CF.STANDARD_NAME, "longitude"));
        nLonVar.addAttribute(new Attribute(CDM.UNITS, CDM.LON_UNITS));
        
        // TODO get variables from input file and put them here
        Variable nMean = ncWriter.addVariable(null, "mean", DataType.DOUBLE, Arrays.asList(nStationDim, nTimeDim));
        nMean.addAttribute(new Attribute(CDM.UNITS, mMean.getUnitsString()));
        nMean.addAttribute(new Attribute(CF.COORDINATES, "time lat lon"));
        nMean.addAttribute(new Attribute(CDM.FILL_VALUE, Float.valueOf(-1f)));
        
        ncWriter.addGroupAttribute(null, new Attribute(CDM.CONVENTIONS, "CF-1.6"));
        ncWriter.addGroupAttribute(null, new Attribute(CF.FEATURE_TYPE, "timeSeries"));
        
        ncWriter.setFill(true);
        
        ncWriter.create();
        
        ncWriter.write(nStationIdVar, ncInput.findVariable("station_id").read());
        ncWriter.write(nLonVar, ncInput.findVariable("lat").read());
        ncWriter.write(nLatVar, ncInput.findVariable("lon").read());
        
        // TODO time and base date need to change
        Array nTimeArray = Array.factory(DataType.INT, new int[] { timeCount } );
        DateTime baseDateTime = DateTime.parse("1960-01-01T00:00:00.000Z");
        DateTime currentDateTime = baseDateTime;
        for (int tIndex = 0; tIndex < timeCount; ++tIndex) {
            nTimeArray.setInt(tIndex, Days.daysBetween(baseDateTime, currentDateTime).getDays());
            currentDateTime = currentDateTime.plusMonths(1);
        }
        ncWriter.write(nTimeVar, nTimeArray);
        
        for (Map.Entry<Integer, List<Double>> entry : observationMap.entrySet()) {
            int stationIndex = entry.getKey();
            List<Double> values = entry.getValue();
            int timeMissing = timeCount - values.size();     
            
            Array valueArray = Array.factory(DataType.DOUBLE, new int[] { 1, timeCount - timeMissing} );
            int valueArrayIndex = 0;
            for (double value : values) {
                valueArray.setDouble(valueArrayIndex++, value);
            }
            
            ncWriter.write(nMean, new int[] { stationIndex, timeMissing }, valueArray);

        }
        
        ncWriter.close();
    }
    
    public static class ReadObserationsVisitor extends AbstractObservationVisitor {
        
        private int stationCount;
        private int stationTimeCount;
        
        private int recordCount = 0;
        private int exampleStationIndex;
        
        private Map<Integer, List<Double>> observationMap = new TreeMap<Integer, List<Double>>();
        
        @Override public void observation(int stationIndex, int timeIndex, double value) {
            if (observationMap.containsKey(stationIndex)) {
                observationMap.get(stationIndex).add(value);
            } else {
                List<Double> list = new ArrayList<Double>();
                list.add(value);
                observationMap.put(stationIndex, list);
            }
            exampleStationIndex = stationIndex;
            recordCount++;
        }
        @Override public void finish() {
            stationCount = observationMap.size();
            stationTimeCount = observationMap.get(exampleStationIndex).size();
        }
        
        Map<Integer, List<Double>> getObservationMap() {
            return observationMap;
        } 
    }
}
