package cpa.defuse;

import java.util.Collection;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.StopOperator;
import exceptions.CPAException;

public class DefUseStopJoin implements StopOperator
{
    private final DefUseDomain defUseDomain;

    public DefUseStopJoin (DefUseDomain defUseDomain)
    {
        this.defUseDomain = defUseDomain;
    }

    public AbstractDomain getAbstractDomain ()
    {
        return defUseDomain;
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
