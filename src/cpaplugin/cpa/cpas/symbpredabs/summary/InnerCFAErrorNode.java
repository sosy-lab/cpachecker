package cpaplugin.cpa.cpas.symbpredabs.summary;

import cpaplugin.cfa.objectmodel.CFAErrorNode;

/**
 * InnerCFANode for Error locations
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class InnerCFAErrorNode extends CFAErrorNode implements InnerCFANode {

    private SummaryCFANode summary;
    
    public InnerCFAErrorNode(int lineNumber) {
        super(lineNumber);
        summary = null;
    }
    
    public SummaryCFANode getSummaryNode() { return summary; }

    public void setSummaryNode(SummaryCFANode s) {
        summary = s;
    }

}
