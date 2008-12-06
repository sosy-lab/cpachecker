package cpa.itpabs;

import logging.LazyLogger;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.MergeOperator;

/**
 * Trivial merge operator for interpolation-based lazy abstraction
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ItpMergeOperator implements MergeOperator {

    private final ItpAbstractDomain domain;

    public ItpMergeOperator(ItpAbstractDomain d) {
        domain = d;
    }


    public AbstractDomain getAbstractDomain() {
        return domain;
    }


    public AbstractElement merge(AbstractElement element1,
                                 AbstractElement element2) {
        LazyLogger.log(LazyLogger.DEBUG_4,
                "Trying to merge elements: ", element1,
                " and: ", element2);

        return element2;
    }

    public AbstractElementWithLocation merge(AbstractElementWithLocation element1,
                                 AbstractElementWithLocation element2) {
        LazyLogger.log(LazyLogger.DEBUG_4,
                "Trying to merge elements: ", element1,
                " and: ", element2);

        return element2;
    }

}
