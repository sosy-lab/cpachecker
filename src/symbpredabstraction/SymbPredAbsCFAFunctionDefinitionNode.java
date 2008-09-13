package symbpredabstraction;

import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.CFANode;

/**
 * A summary node corresponding to a function definition
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SymbPredAbsCFAFunctionDefinitionNode extends CFAFunctionDefinitionNode
        implements SymbPredAbsCFANode {
    private CFANode inner;

    public SymbPredAbsCFAFunctionDefinitionNode(CFANode innerNode, int lineNumber,
            String functionName, String containingFileLocation) {
        super(lineNumber, functionName, containingFileLocation);
        inner = innerNode;
    }

    public CFANode getInnerNode() {
        return inner;
    }
    
    public String toString() {
        return "S" + Integer.toString(inner.getNodeNumber());
    }
}
