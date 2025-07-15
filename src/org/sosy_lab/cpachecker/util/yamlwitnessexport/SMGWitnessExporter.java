package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import java.util.List;
import java.util.stream.Collectors;

public class SMGWitnessExporter extends ARGToWitnessV2 {
    
    @Override
    protected void processARGNode(ARGState pState) {
        SMGState smgState = AbstractStates.extractStateByType(pState, SMGState.class);
        if (smgState != null) {
            List<ACSLInvariant> invariants = extractMemorySafetyInvariants(smgState);
            addInvariantsToWitness(pState, invariants);
        }
    }
    
    private List<ACSLInvariant> extractMemorySafetyInvariants(SMGState smgState) {
        return smgState.getInvariants().stream()
            .filter(i -> i.getProperty() == Property.MEMORY_SAFETY)
            .map(this::convertToACSL)
            .collect(Collectors.toList());
    }
    
    private ACSLInvariant convertToACSL(SMGInvariant invariant) {
        return invariant.accept(new ACSLConverter());
    }
}
