package cpaplugin.cpa.cpas.predicateabstraction;

import java.util.Collection;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.PartialOrder;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.exceptions.CPAException;
import cpaplugin.logging.CPACheckerLogger;
import cpaplugin.logging.CustomLogLevel;

public class PredicateAbstractionStopSep implements StopOperator
{
	private PredicateAbstractionDomain predicateAbstractionDomain;

	public PredicateAbstractionStopSep (PredicateAbstractionDomain predAbsDomain)
	{
		this.predicateAbstractionDomain = predAbsDomain;
	}

	public AbstractDomain getAbstractDomain ()
	{
		return predicateAbstractionDomain;
	}

	public boolean stop (AbstractElement element, Collection<AbstractElement> reached) throws CPAException
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
