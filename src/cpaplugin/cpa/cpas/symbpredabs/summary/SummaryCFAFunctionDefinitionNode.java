package cpaplugin.cpa.cpas.symbpredabs.summary;

import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.CFANode;

public class SummaryCFAFunctionDefinitionNode extends CFAFunctionDefinitionNode
        implements SummaryCFANode {
    private CFANode inner;

    public SummaryCFAFunctionDefinitionNode(CFANode innerNode, int lineNumber,
            String functionName, String containingFileLocation) {
        super(lineNumber, functionName, containingFileLocation);
        inner = innerNode;
    }

    @Override
    public CFANode getInnerNode() {
        return inner;
    }
    
    public String toString() {
        return "S" + Integer.toString(inner.getNodeNumber());
    }
}
