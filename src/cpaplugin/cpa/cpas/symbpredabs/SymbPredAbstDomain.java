package cpaplugin.cpa.cpas.symbpredabs;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.BottomElement;
import cpaplugin.cpa.common.interfaces.JoinOperator;
import cpaplugin.cpa.common.interfaces.PartialOrder;
import cpaplugin.cpa.common.interfaces.TopElement;


/**
 * TODO. This is currently broken
 */
public class SymbPredAbstDomain implements AbstractDomain {
    
    private SymbPredAbstCPA cpa;
    
    public SymbPredAbstDomain(SymbPredAbstCPA cpa) {
        this.cpa = cpa;
    }
    
    public SymbPredAbstCPA getCPA() { return cpa; }

    private class SymbPredAbstBottomElement implements BottomElement {}
    
    private class SymbPredAbstTopElement implements TopElement {}
    
    private class SymbPredAbstPreOrder implements PartialOrder {
        public boolean satisfiesPreOrder(AbstractElement element1, 
                                         AbstractElement element2) {
            if (element1.equals(element2)) {
                return true;
            }
            
            if (element1 == bottomElement || element2 == topElement) {
                return true;
            }
            
            SymbPredAbstElement e1 = (SymbPredAbstElement)element1;
            SymbPredAbstElement e2 = (SymbPredAbstElement)element2;
            if (!e1.getLocation().equals(e2.getLocation())) return false;
            // e1 is smaller or equal than e2 if they refer to the same location
            // and the data region of e1 is smaller than that of e2
            SymbolicFormulaManager mgr = cpa.getFormulaManager();
            if (mgr.entails(e1.getFormula(), e2.getFormula())) {
                return true;
            }
            
            return false;
        }
    }
    
    private class SymbPredAbstJoinOperator implements JoinOperator {
        public AbstractElement join(AbstractElement element1, 
                                    AbstractElement element2) {
            // Useless code, but helps to catch bugs by causing cast exceptions
            SymbPredAbstElement e1 = (SymbPredAbstElement)element1;
            SymbPredAbstElement e2 = (SymbPredAbstElement)element2;
            
            if (e1.equals(e2)) {
                return e1;
            }
            
            if (e1 == bottomElement) {
                return e2;
            }
            if (e2 == bottomElement) {
                return e1;
            }
            
            if (e1.getLocation().equals(e2.getLocation())) {
                // if the locations agree, the join operator simply takes the
                // disjunction of the concrete formulas (and discards the 
                // parent)
                SymbolicFormula f1 = e1.getFormula();
                SymbolicFormula f2 = e2.getFormula();
                SymbolicFormulaManager mgr = cpa.getFormulaManager();
                return new SymbPredAbstElement(e1.getLocation(), 
                        mgr.makeOr(f1, f2), null);
            }
                        
            return topElement;
        }
    }
    
    private final BottomElement bottomElement =
        new SymbPredAbstBottomElement();
    private final TopElement topElement = 
        new SymbPredAbstTopElement();
    private final PartialOrder partialOrder = new SymbPredAbstPartialOrder();
    private final JoinOperator joinOperator = 
        new SymbPredAbstJoinOperator();
    
    public BottomElement getBottomElement() {
        return bottomElement;
    }
    
    public boolean isBottomElement(AbstractElement element) {
        return element instanceof BottomElement;
    }

    public JoinOperator getJoinOperator() {
        return joinOperator;
    }

    public PartialOrder getPartialOrder() {
        return partialOrder;
    }

    public TopElement getTopElement() {
        return topElement;
    }

}
