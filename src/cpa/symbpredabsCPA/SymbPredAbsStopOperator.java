package cpa.symbpredabsCPA;

import java.util.Collection;

import logging.CustomLogLevel;
import logging.LazyLogger;
import symbpredabstraction.MathsatSymbPredAbsFormulaManager;
import symbpredabstraction.SymbPredAbsAbstractFormulaManager;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.StopOperator;
import exceptions.CPAException;

/**
 * coverage check for symbolic lazy abstraction with summaries
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SymbPredAbsStopOperator implements StopOperator {

    private final SymbPredAbsAbstractDomain domain;
    private final SymbPredAbsCPA cpa;

    public SymbPredAbsStopOperator(AbstractDomain d) {
        domain = (SymbPredAbsAbstractDomain) d;
        cpa = domain.getCPA();
    }


    public AbstractDomain getAbstractDomain() {
        return domain;
    }


    public <AE extends AbstractElement> boolean stop(AE element,
            Collection<AE> reached) throws CPAException {

        for (AbstractElement e : reached) {
            if (stop(element, e)) {
                return true;
            }
        }
        return false;
    }

    public boolean stop(AbstractElement element, AbstractElement reachedElement)
            throws CPAException {

    	// TODO move this into partialorder

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

                if(!e1.getParents().equals(e2.getParents())){
                  return false;
                }

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
