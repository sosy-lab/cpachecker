package org.sosy_lab.cpachecker.cpa.cer.cex;

public abstract class CexTransition {
    private final CexState startState;
    private final CexState endState;

    protected CexTransition(CexState pstartNode, CexState pEndNode) {
        startState = pstartNode;
        endState = pEndNode;
    }

    public CexState getStartState() {
        return startState;
    }

    public CexState getEndState() {
        return endState;
    }
}
