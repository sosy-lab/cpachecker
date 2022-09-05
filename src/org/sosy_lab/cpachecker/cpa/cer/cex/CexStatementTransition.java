package org.sosy_lab.cpachecker.cpa.cer.cex;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.cer.CERUtils;

public class CexStatementTransition extends CexPathTransition {
    private final String statement;

    private CexStatementTransition(CexState pstartNode, String pStatement, CexState pEndNode) {
        super(pstartNode, pEndNode);
        statement = pStatement;
    }

    public static CexStatementTransition
            create(CexState pstartNode, String pStatement, CexState pEndNode) {
        return new CexStatementTransition(pstartNode, pStatement, pEndNode);
    }

    public String getStatement() {
        return statement;
    }

    @Override
    public CexState evaluate(CexState state, CFAEdge pEdge) {
        if (!state.equals(getStartState())) {
            return state;
        }
        if (statement.equals(CERUtils.CFAEdgeToString(pEdge))) {
            return getEndState();
        }
        return state;
    }

    @Override
    public String toString() {
        return statement;
    }
}
