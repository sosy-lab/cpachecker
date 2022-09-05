package org.sosy_lab.cpachecker.cpa.cer.cex;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cpa.cer.CERUtils;

public class CexFunctionReturnTransition extends CexPathTransition {

    private final String statement;
    private final String functionName;

    private CexFunctionReturnTransition(
            CexState pstartState,
            String pStatement,
            String pFunctionName,
            CexState pEndState) {
        super(pstartState, pEndState);
        statement = pStatement;
        functionName = pFunctionName;
    }

    public static CexFunctionReturnTransition create(
            CexState pStartState,
            String pStatement,
            String pFunctionName,
            CexState pEndState) {
        return new CexFunctionReturnTransition(pStartState, pStatement, pFunctionName, pEndState);
    }

    public static CexFunctionReturnTransition
            create(CexState pStartState, FunctionReturnEdge pEdge, CexState pEndState) {
        String statement = CERUtils.CFAEdgeToString(pEdge);
        String functionName = pEdge.getSuccessor().getFunction().getQualifiedName();
        return new CexFunctionReturnTransition(pStartState, statement, functionName, pEndState);
    }

    @Override
    public CexState evaluate(CexState pState, CFAEdge pEdge) {
        if (!pState.equals(getStartState()) || !(pEdge instanceof FunctionReturnEdge)) {
            return pState;
        }

        FunctionReturnEdge edge = (FunctionReturnEdge) pEdge;
        String nodeFName = pEdge.getSuccessor().getFunction().getQualifiedName();
        if (CERUtils.CFAEdgeToString(edge).equals(statement) && nodeFName.equals(functionName)) {
            return getEndState();
        }

        return pState;
    }

    public String getStatement() {
        return statement;
    }

    public String getFunctionName() {
        return functionName;
    }

    @Override
    public String toString() {
        return getStatement() + "@" + getFunctionName();
    }
}
