package symbpredabstraction;

import cpaplugin.cfa.objectmodel.AbstractCFAEdge;
import cpaplugin.cfa.objectmodel.CFAEdgeType;

/**
 * An edge that connects two SummaryCFANodes
 * 
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SymbPredAbsCFAEdge extends AbstractCFAEdge {

    public SymbPredAbsCFAEdge() {
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
