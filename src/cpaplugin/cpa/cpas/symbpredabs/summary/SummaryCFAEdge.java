package cpaplugin.cpa.cpas.symbpredabs.summary;

import cpaplugin.cfa.objectmodel.AbstractCFAEdge;
import cpaplugin.cfa.objectmodel.CFAEdgeType;

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
    
    public String toString() {
        return "(" + getPredecessor().toString() + 
            "->" + getSuccessor().toString() + ")";
    }
}
