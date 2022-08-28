package org.sosy_lab.cpachecker.cpa.cer;

import java.util.Collection;
import java.util.Iterator;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.cer.cexInfos.CounterexampleInformation;
import org.sosy_lab.cpachecker.cpa.cer.cexInfos.FeasibilityCheckerInformation;
import org.sosy_lab.cpachecker.cpa.cer.cexInfos.PrecisionInformation;

public class CERState implements AbstractState {

    private final CFANode node;

    private FeasibilityCheckerInformation checkerInfo;
    private PrecisionInformation lastPrecisionInfos;

    public CERState(CFANode pNode, Collection<CounterexampleInformation> cexInfos) {
        node = pNode;

        checkerInfo = null;
        lastPrecisionInfos = null;

        if (cexInfos != null) {
            Iterator<CounterexampleInformation> iter = cexInfos.iterator();
            while (iter.hasNext()) {
                CounterexampleInformation info = iter.next();
                if (info instanceof PrecisionInformation) {
                    lastPrecisionInfos = (PrecisionInformation) info;
                } else if (info instanceof FeasibilityCheckerInformation) {
                    checkerInfo = (FeasibilityCheckerInformation) info;
                }
            }
        }
    }

    public CFANode getNode() {
        return node;
    }

    public PrecisionInformation getPrecisionInfo() {
        return lastPrecisionInfos;
    }

    public FeasibilityCheckerInformation getCheckerInfo() {
        return checkerInfo;
    }
}
