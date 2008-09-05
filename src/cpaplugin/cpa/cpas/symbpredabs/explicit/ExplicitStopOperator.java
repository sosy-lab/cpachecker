package cpaplugin.cpa.cpas.symbpredabs.explicit;

import java.util.Collection;

import cpaplugin.common.LocationMappedReachedSet;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.exceptions.CPAException;
import cpaplugin.logging.CustomLogLevel;
import cpaplugin.logging.LazyLogger;


/**
 * Coverage check for explicit-state lazy abstraction
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ExplicitStopOperator implements StopOperator {

    private ExplicitAbstractDomain domain;
    
    public ExplicitStopOperator(ExplicitAbstractDomain d) {
        domain = d;
    }

    
    public AbstractDomain getAbstractDomain() {
        return domain;
    }

    
    public boolean isBottomElement(AbstractElement element) {
        return element == domain.getBottomElement();
    }

    
    public boolean stop(AbstractElement element,
            Collection<AbstractElement> reached) throws CPAException {
        if (reached instanceof LocationMappedReachedSet) {
            ExplicitAbstractElement e = (ExplicitAbstractElement)element;
            reached = ((LocationMappedReachedSet)reached).get(
                    e.getLocationNode());
            if (reached == null) return false;
        } 
        for (AbstractElement e : reached) {
            if (stop(element, e)) {
                return true;
            }
        }
        return false;
    }

    
    public boolean stop(AbstractElement element, AbstractElement reachedElement)
            throws CPAException {

        ExplicitAbstractElement e1 = (ExplicitAbstractElement)element;
        ExplicitAbstractElement e2 = (ExplicitAbstractElement)reachedElement;
        
        if (e1.getLocation().equals(e2.getLocation())) {
            LazyLogger.log(LazyLogger.DEBUG_4, 
                    "Checking Coverage of element: ", element);
            
            if (!e1.sameContext(e2)) {
                LazyLogger.log(CustomLogLevel.SpecificCPALevel,
                               "NO, not covered: context differs");
                return false;
            }

            ExplicitCPA cpa = domain.getCPA();
            ExplicitAbstractFormulaManager amgr =
                cpa.getAbstractFormulaManager();

            assert(e1.getAbstraction() != null);
            assert(e2.getAbstraction() != null);

            boolean ok = amgr.entails(e1.getAbstraction(), e2.getAbstraction());
            
            if (ok) {
                LazyLogger.log(CustomLogLevel.SpecificCPALevel,
                               "Element: ", element, " COVERED by: ", e2);
                cpa.setCoveredBy(e1, e2);
                e1.setCovered(true);
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
