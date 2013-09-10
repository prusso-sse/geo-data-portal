package gov.usgs.derivative;

import gov.usgs.derivative.time.IntervalTimeStepDescriptor;
import gov.usgs.derivative.time.NetCDFDateUtil;
import gov.usgs.derivative.time.TimeStepDescriptor;
import java.util.List;
import org.joda.time.Interval;
import ucar.nc2.dt.GridDatatype;

/**
 *
 * @author tkunicki
 */
public class IntervalTimeStepAveragingVisitor extends AbstractTimeStepAveragingVisitor {

    private List<Interval> intervalList;

    public IntervalTimeStepAveragingVisitor(List<Interval> intervalList, String outputDir) {
        this.intervalList = intervalList;
        if (outputDir != null) {
            this.outputDir = outputDir;
        } else {
            this.outputDir = DerivativeUtil.DEFAULT_P30Y_PATH;
        }
    }
   
    @Override
    protected TimeStepDescriptor generateDerivativeTimeStepDescriptor(List<GridDatatype> gridDatatypeList) {
        return new IntervalTimeStepDescriptor(
            NetCDFDateUtil.toIntervalUTC(gridDatatypeList.get(0)),
            intervalList); 
    }
    
}
