package cpa.symbpredabs.summary;

import cfa.objectmodel.CFANode;

/**
 * Actual implementation of SummaryCFANode
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SummaryNode extends CFANode implements SummaryCFANode {
    private CFANode inner;

    public SummaryNode(CFANode innerNode) {
        super(-1);
        inner = innerNode;
    }

    public CFANode getInnerNode() {
        return inner;
    }

    @Override
    public String toString() {
        return "S" + Integer.toString(inner.getNodeNumber());
    }

    @Override
    public String getFunctionName() {
        return inner.getFunctionName();
    }

}
