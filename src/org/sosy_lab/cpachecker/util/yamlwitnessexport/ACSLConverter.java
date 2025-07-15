package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import org.sosy_lab.cpachecker.cpa.smg.SMGInvariant;

public class ACSLConverter {
    
    public String convertToACSL(SMGInvariant invariant) {
        switch (invariant.getType()) {
            case POINTER_VALIDITY:
                return "\\valid(" + invariant.getPointer() + ")";
            case ALLOCATION_STATUS:
                return "\\allocated(" + invariant.getPointer() + ")";
            case BUFFER_BOUNDS:
                return "\\valid(" + invariant.getPointer() + "+(0.." + 
                       (invariant.getSize() - 1) + "))";
            case TEMPORAL_SAFETY:
                return "\\at(" + invariant.getExpression() + ", " + 
                       invariant.getTimepoint() + ")";
            default:
                return invariant.toString();
        }
    }
}
