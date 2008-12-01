package cpa.defuse;

import java.util.Collection;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.StopOperator;
import cpa.defuse.DefUseDomain;
import exceptions.CPAException;

public class DefUseStopJoin implements StopOperator
{
    private DefUseDomain defUseDomain;

    public DefUseStopJoin (DefUseDomain defUseDomain)
    {
        this.defUseDomain = defUseDomain;
    }

    public AbstractDomain getAbstractDomain ()
    {
        return defUseDomain;
    }

    public boolean stop (AbstractElement element, Collection<AbstractElement> reached) throws CPAException
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
