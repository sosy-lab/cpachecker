package cpa.predicateabstraction;

import java.util.Collection;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.StopOperator;
import exceptions.CPAException;

public class PredicateAbstractionStopJoin implements StopOperator
{
	private final PredicateAbstractionDomain predicateAbstractionDomain;

	public PredicateAbstractionStopJoin (PredicateAbstractionDomain predAbsDomain)
	{
		this.predicateAbstractionDomain = predAbsDomain;
	}

	public AbstractDomain getAbstractDomain ()
	{
		return predicateAbstractionDomain;
	}

	public <AE extends AbstractElement> boolean stop (AE element, Collection<AE> reached, Precision prec) throws CPAException
	{
		// TODO Erkan implement
		return false;
	}

	public boolean stop(AbstractElement element, AbstractElement reachedElement)
			throws CPAException {
		// TODO Erkan implement
		return false;
	}
}
