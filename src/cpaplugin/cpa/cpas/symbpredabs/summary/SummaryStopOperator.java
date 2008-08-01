package cpaplugin.cpa.cpas.symbpredabs.summary;

import java.util.Collection;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.cpa.cpas.symbpredabs.logging.LazyLogger;
import cpaplugin.exceptions.CPAException;
import cpaplugin.logging.CustomLogLevel;

public class SummaryStopOperator implements StopOperator {

    private SummaryAbstractDomain domain;
    
    public SummaryStopOperator(SummaryAbstractDomain d) {
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
        for (AbstractElement e : reached) {
            if (stop(element, e)) {
                return true;
            }
        }
        return false;
    }

    
    public boolean stop(AbstractElement element, AbstractElement reachedElement)
            throws CPAException {

        SummaryAbstractElement e1 = (SummaryAbstractElement)element;
        SummaryAbstractElement e2 = (SummaryAbstractElement)reachedElement;
        
        if (e1.getLocation().equals(e2.getLocation())) {
            LazyLogger.log(LazyLogger.DEBUG_4, 
                    "Checking Coverage of element: ", element);

            SummaryCPA cpa = domain.getCPA();
            SummaryAbstractFormulaManager amgr = cpa.getAbstractFormulaManager();

            assert(e1.getAbstraction() != null);
            assert(e2.getAbstraction() != null);

            boolean ok = amgr.entails(e1.getAbstraction(), e2.getAbstraction());

            if (ok) {
                LazyLogger.log(CustomLogLevel.SpecificCPALevel,
                               "Element: ", element, " COVERED by: ", e2);
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
