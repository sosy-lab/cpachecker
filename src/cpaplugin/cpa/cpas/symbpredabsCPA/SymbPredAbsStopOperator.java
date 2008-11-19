package cpaplugin.cpa.cpas.symbpredabsCPA;

import java.util.Collection;

import symbpredabstraction.MathsatSymbPredAbsFormulaManager;
import symbpredabstraction.SymbPredAbsAbstractFormulaManager;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.exceptions.CPAException;
import cpaplugin.logging.CustomLogLevel;
import cpaplugin.logging.LazyLogger;

/**
 * coverage check for symbolic lazy abstraction with summaries
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SymbPredAbsStopOperator implements StopOperator {

    private SymbPredAbsAbstractDomain domain;
    private SymbPredAbsCPA cpa;
    
    public SymbPredAbsStopOperator(AbstractDomain d) {
        domain = (SymbPredAbsAbstractDomain) d;
        cpa = domain.getCPA();
    }

    
    public AbstractDomain getAbstractDomain() {
        return domain;
    }

    
    public boolean isBottomElement(AbstractElement element) {
    	SymbPredAbsAbstractElement symbPredAbsElem = (SymbPredAbsAbstractElement) element;

//		if(predAbsElem == (domain.getBottomElement())){
//			System.out.println("==========================");
//			return true;
//		}
    	// TODO if the element is the bottom element
    	if (domain.getCPA().getBDDMathsatSymbPredAbsAbstractManager().isFalse(symbPredAbsElem.getAbstraction())){
    		return true;
    	}

		return false;
    }

    
    public boolean stop(AbstractElement element,
            Collection<AbstractElement> reached) throws CPAException {
    	
        for (AbstractElement e : reached) {
            if (stop(element, e)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean stop(AbstractElement element, AbstractElement reachedElement)
            throws CPAException {
    	
    	// TODO move this into preorder

    	SymbPredAbsAbstractElement e1 = (SymbPredAbsAbstractElement)element;
    	SymbPredAbsAbstractElement e2 = (SymbPredAbsAbstractElement)reachedElement;
        
    	// TODO
//    	if(e1.getLocation().equals(e2.getLocation())){
    	// TODO check
    	//	boolean b = cpa.isAbstractionLocation(e1.getLocation());
    	boolean b = e1.isAbstractionNode();
    		// if not an abstraction location
    		if(!b){
    			if(e1.getParents().equals(e2.getParents())){
    				MathsatSymbPredAbsFormulaManager mgr = cpa.getMathsatSymbPredAbsFormulaManager();
    				boolean ok = mgr.entails(e1.getPathFormula().getSymbolicFormula(), e1.getPathFormula().getSymbolicFormula());
    				if (ok) {
    	                cpa.setCoveredBy(e1, e2);
    	            } else {
    	                LazyLogger.log(CustomLogLevel.SpecificCPALevel,
    	                               "NO, not covered");
    	            }
    	            return ok;
    			}
    			else{
    				return false;
    			}
    		}
    		// if abstraction location
    		else{
                LazyLogger.log(LazyLogger.DEBUG_4, 
                        "Checking Coverage of element: ", element);
                
//                if (!e1.sameContext(e2)) {
//                    LazyLogger.log(CustomLogLevel.SpecificCPALevel,
//                                   "NO, not covered: context differs");
//                    return false;
//                }

                SymbPredAbsCPA cpa = domain.getCPA();
                SymbPredAbsAbstractFormulaManager amgr = cpa.getAbstractFormulaManager();

                assert(e1.getAbstraction() != null);
                assert(e2.getAbstraction() != null);

                boolean ok = amgr.entails(e1.getAbstraction(), e2.getAbstraction());

                if (ok) {
                    LazyLogger.log(CustomLogLevel.SpecificCPALevel,
                                   "Element: ", element, " COVERED by: ", e2);
                    cpa.setCoveredBy(e1, e2);
                } else {
                    LazyLogger.log(CustomLogLevel.SpecificCPALevel,
                                   "NO, not covered");
                }

                return ok;
    		}
    	//}
    	// TODO if locations are different
//    	else{
//    		return false;
//    	}
    }
}
