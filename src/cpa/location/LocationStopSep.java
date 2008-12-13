package cpa.location;

import java.util.Collection;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.PartialOrder;
import cpa.common.interfaces.StopOperator;
import exceptions.CPAException;

public class LocationStopSep implements StopOperator
{
	private final LocationDomain locationDomain;

	public LocationStopSep (LocationDomain locationDomain)
	{
		this.locationDomain = locationDomain;
	}

	public AbstractDomain getAbstractDomain ()
	{
		return locationDomain;
	}

	public <AE extends AbstractElement> boolean stop (AE element, Collection<AE> reached) throws CPAException
	{
		PartialOrder partialOrder = locationDomain.getPartialOrder ();
		for (AbstractElement testElement : reached)
		{
			if (partialOrder.satisfiesPartialOrder (element, testElement))
				return true;
		}

		return false;
	}

	public boolean stop(AbstractElement element, AbstractElement reachedElement)
	throws CPAException {

		PartialOrder partialOrder = locationDomain.getPartialOrder ();
		if (partialOrder.satisfiesPartialOrder (element, reachedElement))
			return true;
		return false;
	}
}
