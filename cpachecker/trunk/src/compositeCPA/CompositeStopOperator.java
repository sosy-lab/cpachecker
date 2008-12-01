package compositeCPA;

import java.util.Collection;
import java.util.List;

import cpa.common.CompositeDomain;
import cpa.common.CompositeElement;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.StopOperator;
import exceptions.CPAException;

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
		if(compositeDomain.isBottomElement(element)){
			return true;
		}

		CompositeElement comp1 = (CompositeElement) element;
		List<AbstractElement> comp1Elements = comp1.getElements ();

		if (comp1Elements.size () != stopOperators.size ())
			throw new CPAException ("Wrong number of stop operator");

		for(AbstractElement reachedElement:reached){
			if (stop (element, reachedElement)){
				return true;
			}
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
