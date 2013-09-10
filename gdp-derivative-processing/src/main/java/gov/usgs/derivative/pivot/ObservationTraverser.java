package gov.usgs.derivative.pivot;

import java.io.IOException;
import ucar.ma2.InvalidRangeException;

/**
 *
 * @author tkunicki
 */
public interface ObservationTraverser {

    void traverse(ObservationVisitor visitor) throws IOException, InvalidRangeException;
    
}
