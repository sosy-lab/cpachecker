package cpa.explicit;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.MergeOperator;
import exceptions.CPAException;

public class ExplicitAnalysisMergeSep implements MergeOperator
{
    private ExplicitAnalysisDomain explicitAnalysisDomain;

    public ExplicitAnalysisMergeSep (ExplicitAnalysisDomain explicitAnalysisDomain)
    {
        this.explicitAnalysisDomain = explicitAnalysisDomain;
    }

    public AbstractDomain getAbstractDomain ()
    {
        return explicitAnalysisDomain;
    }

    public AbstractElement merge (AbstractElement element1, AbstractElement element2)
    {
        return element2;
    }

    public AbstractElementWithLocation merge(AbstractElementWithLocation pElement1,
                                             AbstractElementWithLocation pElement2) throws CPAException {
      throw new CPAException ("Cannot return element with location information");
    }
}
