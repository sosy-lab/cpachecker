package cpa.symbpredabs.summary;


/**
 * InnerCFANodes are CFANodes used in the subgraphs attached to summary
 * locations.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public interface InnerCFANode {
    public SummaryCFANode getSummaryNode();
    public void setSummaryNode(SummaryCFANode s);
}
