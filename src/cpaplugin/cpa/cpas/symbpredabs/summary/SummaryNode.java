package cpaplugin.cpa.cpas.symbpredabs.summary;

import cpaplugin.cfa.objectmodel.CFANode;

public class SummaryNode extends CFANode implements SummaryCFANode {
    private CFANode inner;
    
    public SummaryNode(CFANode innerNode) {
        super(-1);
        inner = innerNode;
    }

    public CFANode getInnerNode() {
        return inner;
    }
    
    public String toString() {
        return "S" + Integer.toString(inner.getNodeNumber());
    }
    
}
