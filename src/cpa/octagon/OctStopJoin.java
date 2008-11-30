package cpa.octagon;

import java.util.Collection;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.StopOperator;
import exceptions.CPAException;

public class OctStopJoin implements StopOperator{
	
private OctDomain octDomain;
    
    public OctStopJoin (OctDomain octDomain)
    {
        this.octDomain = octDomain;
    }
    
    public AbstractDomain getAbstractDomain ()
    {
        return octDomain;
    }

    public boolean stop (AbstractElement element, Collection<AbstractElement> reached) throws CPAException
    {
        // TODO implement
        return false;
    }

	public boolean stop(AbstractElement element, AbstractElement reachedElement)
			throws CPAException {
        // TODO implement
        return false;
    }

}
