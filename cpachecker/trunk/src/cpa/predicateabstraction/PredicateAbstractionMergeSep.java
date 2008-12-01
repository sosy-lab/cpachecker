package cpa.predicateabstraction;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.MergeOperator;

public class PredicateAbstractionMergeSep implements MergeOperator{

private PredicateAbstractionDomain predicateAbstractionDomain;

    public PredicateAbstractionMergeSep (PredicateAbstractionDomain predAbsDomain)
    {
        this.predicateAbstractionDomain = predAbsDomain;
    }

    public AbstractDomain getAbstractDomain ()
    {
        return predicateAbstractionDomain;
    }

    public AbstractElement merge (AbstractElement element1, AbstractElement element2)
    {
        return element2;
    }
}
