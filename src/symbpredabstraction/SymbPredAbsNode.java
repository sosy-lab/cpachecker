package symbpredabstraction;

import cpaplugin.cfa.objectmodel.CFANode;

/**
 * Actual implementation of SummaryCFANode
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */ 
public class SymbPredAbsNode extends CFANode implements SymbPredAbsCFANode {
    private CFANode inner;
    
    public SymbPredAbsNode(CFANode innerNode) {
        super(-1);
        inner = innerNode;
    }

    public CFANode getInnerNode() {
        return inner;
    }
    
    public String toString() {
        return "S" + Integer.toString(inner.getNodeNumber());
    }
    
    public String getFunctionName() {
        return inner.getFunctionName();
    }
    
}
