package org.sosy_lab.cpachecker.cpa.cer.cex;

import java.util.Optional;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;

public class CexFunctionHeadTransition extends CexRootTransition {
    private final String functionName;

    private CexFunctionHeadTransition(CexNode pstartNode, String pFunctionName, CexNode pEndNode) {
        super(pstartNode, pEndNode);
        functionName = pFunctionName;
    }

    public static CexFunctionHeadTransition
            create(CexNode pstartNode, String pFunctionName, CexNode pEndNode) {
        return new CexFunctionHeadTransition(pstartNode, pFunctionName, pEndNode);
    }

    public static CexFunctionHeadTransition
            create(CexNode pstartNode, FunctionEntryNode pFunctionNode, CexNode pEndNode) {
        String functionName = pFunctionNode.getFunction().getQualifiedName();
        return new CexFunctionHeadTransition(pstartNode, functionName, pEndNode);
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
