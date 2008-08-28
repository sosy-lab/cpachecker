package cpaplugin.cpa.cpas.symbpredabs.summary;

import cpaplugin.cfa.objectmodel.CFANode;

/**
 * A node that summarizes a loop-free subpart of the input program
 * 
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public interface SummaryCFANode {
    public CFANode getInnerNode();
}
