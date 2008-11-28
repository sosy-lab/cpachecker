package cpaplugin.cpa.cpas.defuse;

import java.util.Collection;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.exceptions.CPAException;

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

	public boolean isBottomElement(AbstractElement element) {
		return false;
	}

	public boolean stop(AbstractElement element, AbstractElement reachedElement)
			throws CPAException {
		// TODO Erkan implement
   	 return false;
	}
}
