package cpaplugin.cpa.cpas.symbpredabs.summary;

import cpaplugin.cfa.objectmodel.CFANode;


/**
 * Actual implementation of InnerCFANode
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class InnerNode extends CFANode implements InnerCFANode {

    private SummaryCFANode summary;
    
    public InnerNode(int lineNumber) {
        super(lineNumber);
        summary = null;
    }
    
    public SummaryCFANode getSummaryNode() { return summary; }

    public void setSummaryNode(SummaryCFANode s) {
        summary = s;
    }

}
