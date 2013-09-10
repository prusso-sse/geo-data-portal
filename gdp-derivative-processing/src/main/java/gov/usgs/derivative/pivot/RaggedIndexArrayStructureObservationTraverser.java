package gov.usgs.derivative.pivot;

import java.io.IOException;
import ucar.ma2.ArrayStructure;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.StructureMembers;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;

/**
 * COPIED FROM GLRI-AFINCH, rather than changing this, pull out to a common location
 * @author tkunicki
 */
public class RaggedIndexArrayStructureObservationTraverser implements ObservationTraverser {
    private final Variable observationVariable;

    public RaggedIndexArrayStructureObservationTraverser(Variable observationVariable) {
        this.observationVariable = observationVariable;
    }

    @Override
    public void traverse(ObservationVisitor visitor) throws IOException, InvalidRangeException {
        ArrayStructure array;
        final int oStep = 1 << 20;
        final int oTotal = observationVariable.getShape(0);
        
        visitor.start(oTotal);
        for (int oIndex = 0; oIndex < oTotal; oIndex += oStep) {
            int oCount = oIndex + oStep > oTotal ? oTotal - oIndex : oStep;
            array = (ArrayStructure) observationVariable.read(new int[]{oIndex}, new int[]{oCount});
            StructureMembers.Member mTime = array.findMember(DSGPivoter.TIME);
            StructureMembers.Member mIndex = array.findMember(DSGPivoter.INDEX);
            StructureMembers.Member mValue = array.findMember(DSGPivoter.MEAN);
            for (int aIndex = 0; aIndex < array.getSize(); aIndex++) {
                visitor.observation(array.getScalarInt(aIndex, mIndex), array.getScalarInt(aIndex, mTime), array.getScalarDouble(aIndex, mValue));
            }
        }
        visitor.finish();
    }
    
}
