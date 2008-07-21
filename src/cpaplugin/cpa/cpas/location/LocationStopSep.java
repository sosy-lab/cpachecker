package cpaplugin.cpa.cpas.location;

import java.util.Collection;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.PreOrder;
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
		PreOrder preOrder = locationDomain.getPreOrder ();
		for (AbstractElement testElement : reached)
		{
			if (preOrder.satisfiesPreOrder (element, testElement))
				return true;
		}

		return false;
	}

	public boolean isBottomElement(AbstractElement element) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean stop(AbstractElement element, AbstractElement reachedElement)
	throws CPAException {

		PreOrder preOrder = locationDomain.getPreOrder ();
		if (preOrder.satisfiesPreOrder (element, reachedElement))
			return true;
		return false;
	}
}
