package cpa.predicateabstraction;

import java.util.Collection;

import logging.CPACheckerLogger;
import logging.CustomLogLevel;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.PartialOrder;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.StopOperator;
import exceptions.CPAException;

public class PredicateAbstractionStopSep implements StopOperator
{
	private final PredicateAbstractionDomain predicateAbstractionDomain;

	public PredicateAbstractionStopSep (PredicateAbstractionDomain predAbsDomain)
	{
		this.predicateAbstractionDomain = predAbsDomain;
	}

	public AbstractDomain getAbstractDomain ()
	{
		return predicateAbstractionDomain;
	}

	public <AE extends AbstractElement> boolean stop (AE element, Collection<AE> reached, Precision prec) throws CPAException
	{
		PartialOrder partialOrder = predicateAbstractionDomain.getPartialOrder ();
		for (AbstractElement testElement : reached)
		{
			CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel, " Partial order check: element:  " + element
					+ " reached " + reached + " --> "+ partialOrder.satisfiesPartialOrder (element, testElement));
			if (partialOrder.satisfiesPartialOrder (element, testElement))
				return true;
		}

		return false;
	}

	public boolean stop(AbstractElement element, AbstractElement reachedElement)
	throws CPAException {
		PartialOrder partialOrder = predicateAbstractionDomain.getPartialOrder ();
		CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel, " Partial order check: element:  " + element
				+ " reached element" + reachedElement + " --> "+ partialOrder.satisfiesPartialOrder (element, reachedElement));
		if (partialOrder.satisfiesPartialOrder (element, reachedElement))
			return true;
		return false;
	}
}
