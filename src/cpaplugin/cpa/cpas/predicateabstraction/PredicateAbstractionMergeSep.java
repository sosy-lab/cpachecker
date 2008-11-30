package cpaplugin.cpa.cpas.predicateabstraction;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.MergeOperator;

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
