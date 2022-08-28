package org.sosy_lab.cpachecker.cpa.cer.cex;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cpa.cer.CERUtils;

public class CexFunctionReturnTransition extends CexPathTransition {

    private final String statement;
    private final String functionName;

    private CexFunctionReturnTransition(
            CexNode pstartNode,
            String pStatement,
            String pFunctionName,
            CexNode pEndNode) {
        super(pstartNode, pEndNode);
        statement = pStatement;
        functionName = pFunctionName;
    }

    public static CexFunctionReturnTransition
            create(CexNode pstartNode, String pStatement, String pFunctionName, CexNode pEndNode) {
        return new CexFunctionReturnTransition(pstartNode, pStatement, pFunctionName, pEndNode);
    }

    public static CexFunctionReturnTransition
            create(CexNode pstartNode, FunctionReturnEdge pEdge, CexNode pEndNode) {
        String statement = CERUtils.CFAEdgeToString(pEdge);
        String functionName = pEdge.getSuccessor().getFunction().getQualifiedName();
        return new CexFunctionReturnTransition(pstartNode, statement, functionName, pEndNode);
    }

    @Override
    public CexNode evaluate(CexNode pNode, CFAEdge pEdge) {
        if (!pNode.equals(startNode) || !(pEdge instanceof FunctionReturnEdge)) {
            return pNode;
        }

        FunctionReturnEdge edge = (FunctionReturnEdge) pEdge;
        String nodeFName = pEdge.getSuccessor().getFunction().getQualifiedName();
        if (CERUtils.CFAEdgeToString(edge).equals(statement) && nodeFName.equals(functionName)) {
            return endNode;
        }

        return pNode;
    }

    public String getStatement() {
        return statement;
    }

    public String getFunctionName() {
        return functionName;
    }
}
