package cpaplugin.cpa.cpas.itpabs.symbolic;

import java.util.Collection;
import java.util.Map;

import cpaplugin.cfa.objectmodel.CFAErrorNode;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cpa.cpas.itpabs.ItpAbstractElement;
import cpaplugin.cpa.cpas.symbpredabs.Pair;
import cpaplugin.cpa.cpas.symbpredabs.SSAMap;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormula;
import cpaplugin.cpa.cpas.symbpredabs.summary.SummaryCFANode;

/**
 * AbstractElement for the symbolic version (with summary locations) of the
 * interpolation-based lazy abstraction analysis
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */ 
public class ItpSymbolicAbstractElement extends ItpAbstractElement {
    // for each "leaf" node in the inner CFA of this summary, we keep the 
    // symbolic representation of all the paths leading to the leaf
    private Map<CFANode, Pair<SymbolicFormula, SSAMap>> pathFormulas;

    public ItpSymbolicAbstractElement(CFANode loc) {
        super(loc);
    }

    public String toString() {
        return "SE<" + Integer.toString(
                ((SummaryCFANode)getLocation()).getInnerNode().getNodeNumber())
                + ">(" + Integer.toString(getId()) + ",P=" + 
                (getParent() != null ? getParent().getId() : "NIL") + ")"; 
    }

    public boolean isErrorLocation() {
        return (((SummaryCFANode)getLocation()).getInnerNode() instanceof 
                CFAErrorNode);
    }
    
    public Pair<SymbolicFormula, SSAMap> getPathFormula(CFANode leaf) { 
        return pathFormulas.get(leaf); 
    }

    public void setPathFormulas(Map<CFANode, Pair<SymbolicFormula, SSAMap>> pf){
        pathFormulas = pf;
    }

    public Collection<CFANode> getLeaves() {
        assert(pathFormulas != null);        
        return pathFormulas.keySet();
    }
    
}
