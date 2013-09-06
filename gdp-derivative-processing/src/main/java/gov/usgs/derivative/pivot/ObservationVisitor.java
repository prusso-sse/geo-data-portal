package gov.usgs.derivative.pivot;

/**
 *
 * @author tkunicki
 */
public interface ObservationVisitor {

    public void start(long observationCount);
    
    public void observation(int stationIndex, int timeIndex, double value);
    
    public void finish();
    
}
