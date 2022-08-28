package org.sosy_lab.cpachecker.cpa.cer.cex;

public abstract class CexTransition {
    protected final CexNode startNode;
    protected final CexNode endNode;

    protected CexTransition(CexNode pstartNode, CexNode pEndNode) {
        startNode = pstartNode;
        endNode = pEndNode;
    }

    public CexNode getStartNode() {
        return startNode;
    }

    public CexNode getEndNode() {
        return endNode;
    }
}
