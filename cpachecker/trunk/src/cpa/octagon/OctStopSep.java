package cpa.octagon;

import java.util.Collection;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.PartialOrder;
import cpa.common.interfaces.StopOperator;
import exceptions.CPAException;

public class OctStopSep implements StopOperator{

	private OctDomain octDomain;

	public OctStopSep (OctDomain octDomain)
	{
		this.octDomain = octDomain;
	}

	public AbstractDomain getAbstractDomain ()
	{
		return octDomain;
	}

	public boolean stop (AbstractElement element, Collection<AbstractElement> reached) throws CPAException
	{
		PartialOrder partialOrder = octDomain.getPartialOrder ();
		for (AbstractElement testElement : reached)
		{
			if (partialOrder.satisfiesPartialOrder (element, testElement))
			{
				return true;
			}
		}

		return false;
	}

	public boolean stop(AbstractElement element, AbstractElement reachedElement)
	throws CPAException {
		PartialOrder partialOrder = octDomain.getPartialOrder ();
		if (partialOrder.satisfiesPartialOrder (element, reachedElement))
			return true;
		return false;
	}

}
