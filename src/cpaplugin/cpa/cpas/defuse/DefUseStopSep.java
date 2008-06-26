package cpaplugin.cpa.cpas.defuse;

import java.util.Collection;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.PreOrder;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.exceptions.CPAException;

public class DefUseStopSep implements StopOperator
{
    private DefUseDomain defUseDomain;
    
    public DefUseStopSep (DefUseDomain defUseDomain)
    {
        this.defUseDomain = defUseDomain;
    }
    
    public AbstractDomain getAbstractDomain ()
    {
        return defUseDomain;
    }

    public boolean stop (AbstractElement element, Collection<AbstractElement> reached) throws CPAException
    {
        PreOrder preOrder = defUseDomain.getPreOrder ();
        for (AbstractElement testElement : reached)
        {
            if (preOrder.satisfiesPreOrder (element, testElement))
                return true;
        }
        
        return false;
    }
}
