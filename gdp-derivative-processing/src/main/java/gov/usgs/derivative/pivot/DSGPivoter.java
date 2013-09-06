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
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;

public class DSGPivoter {

    private final NetcdfFile ncInput;
    private final Variable oVariable;
    private final DerivativeOptions options;
    
    public DSGPivoter(NetcdfFile netCDFFile, DerivativeOptions options) {
        this.ncInput = netCDFFile;
        this.oVariable = netCDFFile.findVariable("record");
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
                    " : TimeCountMin " + visitor.stationTimeCountMin +
                    " : TimeCountMax " + visitor.stationTimeCountMax +
                    " : RecordCount " + visitor.recordCount
                    );
            System.out.println((System.currentTimeMillis() - start) + "ms");
            
            generatePivotFile(observationMap, visitor.stationCount, visitor.stationTimeCountMax, visitor.recordCount);
        } finally {
            if (writer != null) try { writer.close(); } catch (Exception e) {}
        }
    }
    
    
    protected void generatePivotFile(Map<Integer, List<Double>> observationMap, int stationCount, int timeCount, int recordCount) throws IOException, InvalidRangeException {
        NetcdfFileWriter ncWriter = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, options.outputFile);
        
        Dimension nStationDim = ncWriter.addDimension(null, "station", stationCount);
        // 59 is the largest station size for derivatives, parameterize?
        Dimension nStationIdLenDim = ncWriter.addDimension(null, "station_id_len", 59);
        Dimension nTimeDim = ncWriter.addDimension(null, "time", timeCount);
        
        Variable nStationIdVar = ncWriter.addVariable(null, "station_id", DataType.CHAR, Arrays.asList(nStationDim, nStationIdLenDim));
        nStationIdVar.addAttribute(new Attribute(CF.STANDARD_NAME, CF.STATION_ID));
        nStationIdVar.addAttribute(new Attribute(CF.CF_ROLE, CF.TIMESERIES_ID));

        Variable nTimeVar = ncWriter.addVariable(null, "time", DataType.INT, Arrays.asList(nTimeDim));
        nTimeVar.addAttribute(new Attribute(CF.STANDARD_NAME, "time"));
        nTimeVar.addAttribute(new Attribute(CDM.UNITS, "days since 1960-01-01T00:00:00.000Z"));
        nTimeVar.addAttribute(new Attribute(CF.CALENDAR, "gregorian"));
        
        Variable nLatVar = ncWriter.addVariable(null, "lat", DataType.FLOAT, Arrays.asList(nStationDim));
        nLatVar.addAttribute(new Attribute(CF.STANDARD_NAME, "latitude"));
        nLatVar.addAttribute(new Attribute(CDM.UNITS, CDM.LAT_UNITS));
        
        Variable nLonVar = ncWriter.addVariable(null, "lon", DataType.FLOAT, Arrays.asList(nStationDim));
        nLonVar.addAttribute(new Attribute(CF.STANDARD_NAME, "longitude"));
        nLonVar.addAttribute(new Attribute(CDM.UNITS, CDM.LON_UNITS));
        
        // TODO get variables from input file and put them here
        Variable nMean = ncWriter.addVariable(null, "mean", DataType.DOUBLE, Arrays.asList(nStationDim, nTimeDim));
        nMean.addAttribute(oVariable.findAttribute(CDM.UNITS));
        nMean.addAttribute(oVariable.findAttribute(CF.STANDARD_NAME));
        
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
        
        private int stationIndexLast;
        private int stationCount;
        
        private int stationTimeCountMin = Integer.MAX_VALUE;
        private int stationTimeCountMax = Integer.MIN_VALUE;
        private ArrayList<Double> stationTimeSeries = null;
        
        private int recordCount;
        
        private Map<Integer, List<Double>> observationMap = new TreeMap<Integer, List<Double>>();

        ObservationVisitor delegate = new DSGPivoter.ReadObserationsVisitor.PrimingVisitor();
        
        @Override public void observation(int stationIndex, int timeIndex, double value) {
            delegate.observation(stationIndex, timeIndex, value);
        }
        @Override public void finish() {
            delegate.finish();
        }
        
        public class PrimingVisitor extends AbstractObservationVisitor {
            @Override public void observation(int stationIndex, int timeIndex, double value) {
                initStationData(stationIndex);
                recordCount++;
                delegate = new DSGPivoter.ReadObserationsVisitor.CountingVisitor();
            }
        }
        public class CountingVisitor extends AbstractObservationVisitor {
            @Override public void observation(int stationIndex, int timeIndex, double value) {
                if (stationIndexLast != stationIndex) {
                    processStationData();
                    initStationData(stationIndex);
                }
                stationTimeSeries.add(value);
                recordCount++;
            }
            @Override public void finish() {
                processStationData();
            }
        }    
        private void processStationData() {
            stationTimeSeries.trimToSize();
            observationMap.put(stationIndexLast, stationTimeSeries);
            int stationTimeCount = stationTimeSeries.size();
            if (stationTimeCount < stationTimeCountMin) {
                stationTimeCountMin = stationTimeCount;
            }
            if (stationTimeCount > stationTimeCountMax) {
                stationTimeCountMax = stationTimeCount;
            }
        }
        private void initStationData(int stationIndex) {
            stationIndexLast = stationIndex;
            stationCount++;
            stationTimeSeries = new ArrayList<Double>();
        }
        
        Map<Integer, List<Double>> getObservationMap() {
            return observationMap;
        } 
    }
}
