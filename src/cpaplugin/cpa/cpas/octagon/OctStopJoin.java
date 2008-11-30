package cpaplugin.cpa.cpas.octagon;

import java.util.Collection;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.exceptions.CPAException;

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
