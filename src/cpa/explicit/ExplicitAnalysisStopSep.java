package cpa.explicit;

import java.util.Collection;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.PartialOrder;
import cpa.common.interfaces.StopOperator;
import exceptions.CPAException;

public class ExplicitAnalysisStopSep implements StopOperator {

	private ExplicitAnalysisDomain explicitAnalysisDomain;

	public ExplicitAnalysisStopSep (ExplicitAnalysisDomain explicitAnalysisDomain)
	{
		this.explicitAnalysisDomain = explicitAnalysisDomain;
	}

	public AbstractDomain getAbstractDomain ()
	{
		return explicitAnalysisDomain;
	}

	public <AE extends AbstractElement> boolean stop (AE element, Collection<AE> reached)
	throws CPAException {
		PartialOrder partialOrder = explicitAnalysisDomain.getPartialOrder();
		for (AbstractElement testElement : reached)
		{
			if (partialOrder.satisfiesPartialOrder (element, testElement))
				return true;
		}
		return false;
	}

	public boolean stop(AbstractElement element, AbstractElement reachedElement)
	throws CPAException {
		PartialOrder partialOrder = explicitAnalysisDomain.getPartialOrder ();
		if (partialOrder.satisfiesPartialOrder (element, reachedElement))
			return true;
		return false;
	}
}
