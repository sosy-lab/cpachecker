package cpa.symbpredabs.explicit;

import java.util.Collection;
import java.util.Set;

import logging.CustomLogLevel;
import logging.LazyLogger;

import common.LocationMappedReachedSet;
import common.Pair;
import common.LocationMappedReachedSet.LocationMappedProjection;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.StopOperator;
import exceptions.CPAException;


/**
 * Coverage check for explicit-state lazy abstraction
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ExplicitStopOperator implements StopOperator {

    private final ExplicitAbstractDomain domain;

    public ExplicitStopOperator(ExplicitAbstractDomain d) {
        domain = d;
    }


    public AbstractDomain getAbstractDomain() {
        return domain;
    }


    public <AE extends AbstractElement> boolean stop(AE element,
                                                     Collection<AE> reached, Precision prec) throws CPAException {
      if (reached instanceof LocationMappedProjection) {
        ExplicitAbstractElement e = (ExplicitAbstractElement)element;
        Set<AbstractElementWithLocation> effReached =
          ((LocationMappedProjection)reached).get(
              e.getLocationNode());
        if (effReached == null) return false;
        for (AbstractElementWithLocation e2: effReached) {
          if (stop(element, e2)) {
            return true;
          }
        }
      } else {
        for (AbstractElement e : reached) {
          if (stop(element, e)) {
            return true;
          }
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
