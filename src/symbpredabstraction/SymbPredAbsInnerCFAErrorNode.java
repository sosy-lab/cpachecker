package symbpredabstraction;

import cpaplugin.cfa.objectmodel.CFAErrorNode;

/**
 * InnerCFANode for Error locations
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SymbPredAbsInnerCFAErrorNode extends CFAErrorNode implements SymbPredAbsInnerCFANode {

    private SymbPredAbsCFANode summary;
    
    public SymbPredAbsInnerCFAErrorNode(int lineNumber) {
        super(lineNumber);
        summary = null;
    }
    
    public SymbPredAbsCFANode getSummaryNode() { return summary; }

    public void setSummaryNode(SymbPredAbsCFANode s) {
        summary = s;
    }

}
