package cpaplugin.cpa.cpas.octagon;

import java.util.Collection;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.BottomElement;
import cpaplugin.cpa.common.interfaces.PartialOrder;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.exceptions.CPAException;

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
		PartialOrder preOrder = octDomain.getPreOrder ();
		for (AbstractElement testElement : reached)
		{
			if (preOrder.satisfiesPreOrder (element, testElement))
			{
				return true;
			}
		}

		return false;
	}

	//TODO test this
	public boolean isBottomElement(AbstractElement element) {
		if(element instanceof BottomElement){
			return true;
		}
		OctElement octElem = (OctElement) element;
		return octElem.isEmpty();
	}

	public boolean stop(AbstractElement element, AbstractElement reachedElement)
	throws CPAException {
		PartialOrder preOrder = octDomain.getPreOrder ();
		if (preOrder.satisfiesPreOrder (element, reachedElement))
			return true;
		return false;
	}

}
