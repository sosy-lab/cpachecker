package cpa.explicit;

import java.util.Collection;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.StopOperator;
import exceptions.CPAException;

public class ExplicitAnalysisStopJoin implements StopOperator {
	
    private ExplicitAnalysisDomain explicitAnalysisDomain;

    public ExplicitAnalysisStopJoin (ExplicitAnalysisDomain explicitAnalysisDomain)
    {
        this.explicitAnalysisDomain = explicitAnalysisDomain;
    }

    public AbstractDomain getAbstractDomain ()
    {
        return explicitAnalysisDomain;
    }

    public boolean stop (AbstractElement element, Collection<AbstractElement> reached) 
    		throws CPAException {
    	// TODO
    	return false;
    }

	public boolean stop(AbstractElement element, AbstractElement reachedElement)
			throws CPAException {
		// TODO
		return false;
	}
}
