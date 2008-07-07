package cpaplugin.cpa.cpas.octagon;

import java.util.Collection;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.PreOrder;
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
		PreOrder preOrder = octDomain.getPreOrder ();
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
		OctElement octElem = (OctElement) element;
		return octElem.isEmpty();
	}

}
