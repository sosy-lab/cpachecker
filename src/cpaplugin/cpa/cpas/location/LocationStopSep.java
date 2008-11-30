package cpaplugin.cpa.cpas.location;

import java.util.Collection;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.PartialOrder;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.exceptions.CPAException;

public class LocationStopSep implements StopOperator
{
	private LocationDomain locationDomain;

	public LocationStopSep (LocationDomain locationDomain)
	{
		this.locationDomain = locationDomain;
	}

	public AbstractDomain getAbstractDomain ()
	{
		return locationDomain;
	}

	public boolean stop (AbstractElement element, Collection<AbstractElement> reached) throws CPAException
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
