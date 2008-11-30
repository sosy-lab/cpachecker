package cpaplugin.cpa.cpas.predicateabstraction;

import java.util.Collection;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.exceptions.CPAException;

public class PredicateAbstractionStopJoin implements StopOperator
{
	private PredicateAbstractionDomain predicateAbstractionDomain;

	public PredicateAbstractionStopJoin (PredicateAbstractionDomain predAbsDomain)
	{
		this.predicateAbstractionDomain = predAbsDomain;
	}

	public AbstractDomain getAbstractDomain ()
	{
		return predicateAbstractionDomain;
	}

	public boolean stop (AbstractElement element, Collection<AbstractElement> reached) throws CPAException
	{
		// TODO Erkan implement
		return false;
	}

	public boolean isBottomElement(AbstractElement element) {

		PredicateAbstractionElement predAbsElem = (PredicateAbstractionElement) element;
		
		if(predAbsElem.equals(predicateAbstractionDomain.getBottomElement())){
			return true;
		}
		
		return false;
	}

	public boolean stop(AbstractElement element, AbstractElement reachedElement)
			throws CPAException {
		// TODO Erkan implement
		return false;
	}
}
