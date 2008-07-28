package cpaplugin.cpa.cpas.symbpredabs.summary;

import cpaplugin.cfa.objectmodel.CFAErrorNode;

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
