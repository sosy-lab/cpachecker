package cpa.defuse;

import java.util.Collection;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.PartialOrder;
import cpa.common.interfaces.StopOperator;
import exceptions.CPAException;

public class DefUseStopSep implements StopOperator
{
	private final DefUseDomain defUseDomain;

	public DefUseStopSep (DefUseDomain defUseDomain)
	{
		this.defUseDomain = defUseDomain;
	}

	public AbstractDomain getAbstractDomain ()
	{
		return defUseDomain;
	}

	public <AE extends AbstractElement> boolean stop (AE element, Collection<AE> reached) throws CPAException
	{
		PartialOrder partialOrder = defUseDomain.getPartialOrder ();
		for (AbstractElement testElement : reached)
		{
			if (partialOrder.satisfiesPartialOrder (element, testElement))
				return true;
		}

		return false;
	}

	public boolean stop(AbstractElement element, AbstractElement reachedElement)
	throws CPAException {
		// TODO check
		PartialOrder partialOrder = defUseDomain.getPartialOrder ();
		if (partialOrder.satisfiesPartialOrder (element, reachedElement))
			return true;
		return false;
	}
}
