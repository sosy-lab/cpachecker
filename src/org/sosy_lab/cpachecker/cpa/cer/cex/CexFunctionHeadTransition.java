package org.sosy_lab.cpachecker.cpa.cer.cex;

import java.util.Optional;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;

public class CexFunctionHeadTransition extends CexRootTransition {
    private final String functionName;

    private CexFunctionHeadTransition(
            CexState pStartState,
            String pFunctionName,
            CexState pEndState) {
        super(pStartState, pEndState);
        functionName = pFunctionName;
    }

    public static CexFunctionHeadTransition
            create(CexState pstartState, String pFunctionName, CexState pEndState) {
        return new CexFunctionHeadTransition(pstartState, pFunctionName, pEndState);
    }

    public static CexFunctionHeadTransition
            create(CexState pStartState, FunctionEntryNode pFunctionNode, CexState pEndState) {
        String functionName = pFunctionNode.getFunction().getQualifiedName();
        return new CexFunctionHeadTransition(pStartState, functionName, pEndState);
    }

    public String getFunctionName() {
        return functionName;
    }

    @Override
    public Optional<CFANode> getRootFromCFA(CFA pCfa) {
        CFANode rootNode = pCfa.getFunctionHead(functionName);
        return Optional.ofNullable(rootNode);
    }

    @Override
    public String toString() {
        return functionName;
    }
}
