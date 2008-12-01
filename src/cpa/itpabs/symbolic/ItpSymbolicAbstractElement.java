package cpa.itpabs.symbolic;

import java.util.Collection;
import java.util.Map;

import cfa.objectmodel.CFAErrorNode;
import cfa.objectmodel.CFANode;

import cpa.itpabs.ItpAbstractElement;
import cpa.symbpredabs.Pair;
import cpa.symbpredabs.SSAMap;
import cpa.symbpredabs.SymbolicFormula;
import cpa.symbpredabs.summary.SummaryCFANode;

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

    @Override
    public String toString() {
        return "SE<" + Integer.toString(
                ((SummaryCFANode)getLocation()).getInnerNode().getNodeNumber())
                + ">(" + Integer.toString(getId()) + ",P=" + 
                (getParent() != null ? getParent().getId() : "NIL") + ")"; 
    }

    @Override
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

    @Override
    public Collection<CFANode> getLeaves() {
        assert(pathFormulas != null);        
        return pathFormulas.keySet();
    }
    
}
