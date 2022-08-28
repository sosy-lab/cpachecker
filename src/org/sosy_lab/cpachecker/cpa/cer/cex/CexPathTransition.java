package org.sosy_lab.cpachecker.cpa.cer.cex;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public abstract class CexPathTransition extends CexTransition {

    public CexPathTransition(CexNode pstartNode, CexNode pEndNode) {
        super(pstartNode, pEndNode);
    }

    public abstract CexNode evaluate(CexNode node, CFAEdge edge);
}
