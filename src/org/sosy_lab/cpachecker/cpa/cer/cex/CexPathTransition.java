package org.sosy_lab.cpachecker.cpa.cer.cex;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public abstract class CexPathTransition extends CexTransition {

    public CexPathTransition(CexState pStartState, CexState pEndState) {
        super(pStartState, pEndState);
    }

    public abstract CexState evaluate(CexState pState, CFAEdge edge);
}
