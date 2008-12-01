package cpa.symbpredabs.summary;

import cfa.objectmodel.AbstractCFAEdge;
import cfa.objectmodel.CFAEdgeType;

/**
 * An edge that connects two SummaryCFANodes
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SummaryCFAEdge extends AbstractCFAEdge {

    public SummaryCFAEdge() {
        super("");
    }

    public CFAEdgeType getEdgeType() {
        return CFAEdgeType.BlankEdge;
    }

    @Override
    public String toString() {
        return "(" + getPredecessor().toString() +
            "->" + getSuccessor().toString() + ")";
    }
}
