package cpaplugin.compositeCPA;

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
		if(isBottomElement(element)){
			return true;
		}
		
		PreOrder preOrder = compositeDomain.getPreOrder ();
		for (AbstractElement testElement : reached)
		{
			if (preOrder.satisfiesPreOrder (element, testElement)){
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
}
