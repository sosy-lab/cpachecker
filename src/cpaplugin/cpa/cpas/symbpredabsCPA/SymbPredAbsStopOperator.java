package cpaplugin.cpa.cpas.symbpredabsCPA;

import java.util.Collection;

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
    
    public SymbPredAbsStopOperator(AbstractDomain d) {
        domain = (SymbPredAbsAbstractDomain) d;
    }

    
    public AbstractDomain getAbstractDomain() {
        return domain;
    }

    
    public boolean isBottomElement(AbstractElement element) {
        return element == domain.getBottomElement();
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

    	SymbPredAbsAbstractElement e1 = (SymbPredAbsAbstractElement)element;
    	SymbPredAbsAbstractElement e2 = (SymbPredAbsAbstractElement)reachedElement;
        
        if (e1.getLocation().equals(e2.getLocation())) {
            LazyLogger.log(LazyLogger.DEBUG_4, 
                    "Checking Coverage of element: ", element);
            
            if (!e1.sameContext(e2)) {
                LazyLogger.log(CustomLogLevel.SpecificCPALevel,
                               "NO, not covered: context differs");
                return false;
            }

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
        } else {
            return false;
        }
    }

}
