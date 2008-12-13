package cpa.symbpredabs.summary;

import java.util.Collection;

import logging.CustomLogLevel;
import logging.LazyLogger;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.StopOperator;
import exceptions.CPAException;

/**
 * coverage check for symbolic lazy abstraction with summaries
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SummaryStopOperator implements StopOperator {

    private final SummaryAbstractDomain domain;

    public SummaryStopOperator(SummaryAbstractDomain d) {
        domain = d;
    }


    public AbstractDomain getAbstractDomain() {
        return domain;
    }


    public <AE extends AbstractElement> boolean stop(AE element,
            Collection<AE> reached, Precision prec) throws CPAException {
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

            if (!e1.sameContext(e2)) {
                LazyLogger.log(CustomLogLevel.SpecificCPALevel,
                               "NO, not covered: context differs");
                return false;
            }

            SummaryCPA cpa = domain.getCPA();
            SummaryAbstractFormulaManager amgr = cpa.getAbstractFormulaManager();

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
