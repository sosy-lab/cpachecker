package symbpredabstraction;

/**
 * InnerCFANodes are CFANodes used in the subgraphs attached to summary
 * locations.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public interface SymbPredAbsInnerCFANode {
    public SymbPredAbsCFANode getSummaryNode();
    public void setSummaryNode(SymbPredAbsCFANode s);
}
