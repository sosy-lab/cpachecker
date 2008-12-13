package cpa.symbpredabs.summary;

import logging.LazyLogger;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;

/**
 * trivial merge operation for symbolic lazy abstraction with summaries
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SummaryMergeOperator implements MergeOperator {

    private final SummaryAbstractDomain domain;

    public SummaryMergeOperator(SummaryAbstractDomain d) {
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


    public AbstractElementWithLocation merge(AbstractElementWithLocation element1,
                                 AbstractElementWithLocation element2,
                                 Precision prec) {
        LazyLogger.log(LazyLogger.DEBUG_4,
                "Trying to merge elements: ", element1,
                " and: ", element2);

        return element2;
    }
}
