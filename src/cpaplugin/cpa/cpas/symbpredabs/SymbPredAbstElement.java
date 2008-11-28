package cpaplugin.cpa.cpas.symbpredabs;

import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.AbstractElementWithLocation;
import cpaplugin.logging.CPACheckerLogger;
import cpaplugin.logging.LazyLogger;

/**
 * TODO. This is currently broken
 */
public class SymbPredAbstElement 
        implements AbstractElementWithLocation {
    // unique identifier for each domain element
    private static int nextAvailableElemId = 1;

    private CFANode location;
    private int elemId;
    // symbolic representation of (an over-approximation of) the data region 
    // at this location
    private SymbolicFormula concrFormula;
    // symbolic representation of the abstract data region at this location
    private AbstractFormula absFormula;
    // parent element, used for "path" reconstruction in counterexample analysis
    private SymbPredAbstElement parent;
    // the SSA map for variables referenced in the concrete formula
    private SSAMap ssaMap;
    // Shortcut! Our merge operator is "symmetrical", in the sense
    // that it is meant to replace both elements, not only the second one. This
    // is already possible in the CPA algorithm, since after a merge the first
    // element is checked for coverage by the result of the merge. But in order
    // to test coverage quickly, we "cheat" here and set the element to be 
    // explicitly covered by using the following field
    private SymbPredAbstElement coveredBy;

    // access to members
    public CFANode getLocation() { return location; }
    public int getId() { return elemId; }
    public SymbolicFormula getConcreteFormula() { return concrFormula; }
    public AbstractFormula getAbstractFormula() { return absFormula; }
    public SymbPredAbstElement getParent() { return parent; }
    public SSAMap getSSAMap() { return ssaMap; }
    
    // default (not public) access, since this is hackish...
    SymbPredAbstElement getCoveredBy() { return coveredBy; }
    void setCoveredBy(SymbPredAbstElement e) { coveredBy = e; }
    
    // returns the AND of concrete and abstract formula
    public SymbolicFormula getFormula() {
        if (absFormula == null) {
            return concrFormula;
        } else {
            SymbolicFormulaManager mgr = 
                SymbPredAbstCPA.getInstance().getFormulaManager();
            AbstractFormulaManager amgr =
                SymbPredAbstCPA.getInstance().getAbstractFormulaManager();
            SymbolicFormula f = amgr.toConcrete(mgr, absFormula);
            return mgr.makeAnd(concrFormula, mgr.instantiate(f, ssaMap));
        }
    }
    
    public void setConcreteFormula(SymbolicFormula cf) { concrFormula = cf; }
    public void setAbstractFormula(AbstractFormula af) { absFormula = af; }
    public void setParent(SymbPredAbstElement p) { parent = p; }
    
    public boolean equals(Object other) {
        if (!(other instanceof SymbPredAbstElement)) return false;        
        return getId() == ((SymbPredAbstElement)other).getId();
    }
    
    public int hashCode() {
        return getId();
    }
    
    public String toString() {
        
        if (CPACheckerLogger.getLevel() > LazyLogger.DEBUG_1.intValue()) {
            return "E<" + Integer.toString(location.getNodeNumber()) + ">(" +
                Integer.toString(elemId) + ", P" + 
                (parent == null ? "0" : Integer.toString(parent.getId())) + ")";
        } else {
            return "E<" + Integer.toString(location.getNodeNumber()) + ">(" +
               Integer.toString(elemId) + ", P" + 
               (parent == null ? "0" : Integer.toString(parent.getId())) + ")" + 
               "[" + concrFormula.toString() + "]{" + 
               (absFormula == null ? "" : absFormula.toString()) + "}";
        }
    }
    
    public SymbPredAbstElement(CFANode locationNode, SymbolicFormula cf,
                               AbstractFormula af, 
                               SymbPredAbstElement p, SSAMap ssa) {
        location = locationNode;
        elemId = nextAvailableElemId++;
        parent = p;
        concrFormula = cf;
        absFormula = af;
        ssaMap = ssa;
        coveredBy = null;
    }

    public SymbPredAbstElement(CFANode locationNode, SymbolicFormula cf,
            AbstractFormula af, SymbPredAbstElement p) {
        this(locationNode, cf, af, p, new SSAMap());
    }
    
    public SymbPredAbstElement(CFANode locationNode, SymbolicFormula cf,
            AbstractFormula af) {
        this(locationNode, cf, af, null, new SSAMap());
    }
    
    public CFANode getLocationNode() {
        return location;
    }

}
