package symbpredabstraction;

import cpaplugin.cfa.objectmodel.CFANode;


/**
 * Actual implementation of InnerCFANode
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SymbPredAbsInnerNode extends CFANode implements SymbPredAbsInnerCFANode {

    private SymbPredAbsCFANode summary;
    
    public SymbPredAbsInnerNode(int lineNumber) {
        super(lineNumber);
        summary = null;
    }
    
    public SymbPredAbsCFANode getSummaryNode() { return summary; }

    public void setSummaryNode(SymbPredAbsCFANode s) {
        summary = s;
    }

}
