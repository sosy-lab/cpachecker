package cpa.symbpredabs.explicit;

import logging.LazyLogger;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import exceptions.CPAException;

/**
 * Trivial merge operator for Explicit-state lazy abstraction.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ExplicitMergeOperator implements MergeOperator {

    private final ExplicitAbstractDomain domain;

    public ExplicitMergeOperator(ExplicitAbstractDomain d) {
        domain = d;
    }


    public AbstractDomain getAbstractDomain() {
        return domain;
    }

    public AbstractElement merge(AbstractElement element1,
                                 AbstractElement element2,
                                 Precision prec) {
        LazyLogger.log(LazyLogger.DEBUG_4,
                "Trying to merge elements: ", element1,
                " and: ", element2);

        return element2;
    }

    public AbstractElementWithLocation merge(AbstractElementWithLocation pElement1,
                                             AbstractElementWithLocation pElement2,
                                             Precision prec) throws CPAException {
      LazyLogger.log(LazyLogger.DEBUG_4,
          "Trying to merge elements: ", pElement1,
          " and: ", pElement2);

      return pElement2;
    }
}
