package cpa.symbpredabs.explicit;

import logging.LazyLogger;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.MergeOperator;

/**
 * Trivial merge operator for Explicit-state lazy abstraction.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ExplicitMergeOperator implements MergeOperator {

    private ExplicitAbstractDomain domain;

    public ExplicitMergeOperator(ExplicitAbstractDomain d) {
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

}
