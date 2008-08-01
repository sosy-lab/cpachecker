package cpaplugin.cpa.cpas.symbpredabs.summary;

import java.util.Collection;
import java.util.Map;

import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.AbstractElementWithLocation;
import cpaplugin.cpa.cpas.symbpredabs.AbstractFormula;
import cpaplugin.cpa.cpas.symbpredabs.Pair;
import cpaplugin.cpa.cpas.symbpredabs.SSAMap;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormula;

public class SummaryAbstractElement 
        implements AbstractElement, AbstractElementWithLocation {
    
    private int elemId;
    private SummaryCFANode summaryLocation;
    // for each "leaf" node in the inner CFA of this summary, we keep the 
    // symbolic representation of all the paths leading to the leaf
    private Map<CFANode, Pair<SymbolicFormula, SSAMap>> pathFormulas;
    private AbstractFormula abstraction;
    private SummaryAbstractElement parent;
    
    private static int nextAvailableId = 1;
    
    public int getId() { return elemId; }
    public SummaryCFANode getLocation() { return summaryLocation; }
    public Pair<SymbolicFormula, SSAMap> getPathFormula(CFANode leaf) { 
        return pathFormulas.get(leaf); 
    }
    public AbstractFormula getAbstraction() { return abstraction; }
    
    public void setAbstraction(AbstractFormula a) { abstraction = a; }
    public void setPathFormulas(Map<CFANode, Pair<SymbolicFormula, SSAMap>> pf){
        pathFormulas = pf;
    }
    
    public SummaryAbstractElement getParent() { return parent; }
    public void setParent(SummaryAbstractElement p) { parent = p; }

    private SummaryAbstractElement(SummaryCFANode loc, AbstractFormula a, 
            Map<CFANode, Pair<SymbolicFormula, SSAMap>> pf,
            SummaryAbstractElement p) {
        elemId = nextAvailableId++;
        summaryLocation = loc;
        abstraction = a;
        pathFormulas = pf;
        parent = p;
    }
    
    public SummaryAbstractElement(SummaryCFANode loc) {
        this(loc, null, null, null);
    }
    
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof SummaryAbstractElement)) {
            return false;
        } else {
            return elemId == ((SummaryAbstractElement)o).elemId;
        }
    }
    
    public String toString() {
        return "SE<" + Integer.toString(
                summaryLocation.getInnerNode().getNodeNumber()) + ">(" +
                Integer.toString(getId()) + ")"; 
    }

    public CFANode getLocationNode() {
        return (CFANode)summaryLocation;
    }
    
    public Collection<CFANode> getLeaves() {
        assert(pathFormulas != null);
        
        return pathFormulas.keySet();
    }
}
