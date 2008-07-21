package cpaplugin.compositeCPA;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cpaplugin.cpa.common.CompositeDomain;
import cpaplugin.cpa.common.CompositeElement;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.MergeOperator;
import cpaplugin.cpa.common.interfaces.PreOrder;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.exceptions.CPAException;

public class CompositeStopOperator implements StopOperator{

	private CompositeDomain compositeDomain;
	private List<StopOperator> stopOperators;

	public CompositeStopOperator (CompositeDomain compositeDomain, List<StopOperator> stopOperators)
	{
		this.compositeDomain = compositeDomain;
		this.stopOperators = stopOperators;
	}

	public AbstractDomain getAbstractDomain ()
	{
		return compositeDomain;
	}

	public boolean stop (AbstractElement element, Collection<AbstractElement> reached) throws CPAException
	{
		CompositeElement comp1 = (CompositeElement) element;
		List<AbstractElement> comp1Elements = comp1.getElements ();
		
		if (comp1Elements.size () != stopOperators.size ())
			throw new CPAException ("Wrong number of stop operator");

		if(isBottomElement(element)){
			return true;
		}
		
		for(AbstractElement reachedElement:reached){
			if (stop (element, reachedElement)){
				return true;
			}
		}
		return false;
	}

	public boolean isBottomElement(AbstractElement element) {

		CompositeElement compositeElement = (CompositeElement) element;

		List<AbstractElement> compositeElements = compositeElement.getElements ();

		for (int idx = 0; idx < compositeElements.size (); idx++)
		{
			StopOperator stopOp = stopOperators.get(idx);
			AbstractElement absElem = compositeElements.get(idx);
			if (stopOp.isBottomElement(absElem))
				return true;
		}

		return false;
	}

	public boolean stop(AbstractElement element, AbstractElement reachedElement)
	throws CPAException {
		CompositeElement compositeElement1 = (CompositeElement) element;
		CompositeElement compositeElement2 = (CompositeElement) reachedElement;

		List<AbstractElement> compositeElements1 = compositeElement1.getElements ();
		List<AbstractElement> compositeElements2 = compositeElement2.getElements ();

		for (int idx = 0; idx < compositeElements1.size (); idx++)
		{
			StopOperator stopOp = stopOperators.get(idx);
			AbstractElement absElem1 = compositeElements1.get(idx);
			AbstractElement absElem2 = compositeElements2.get(idx);
			if (!stopOp.stop(absElem1, absElem2))
				return false;
		}
		return true;
	}
}
