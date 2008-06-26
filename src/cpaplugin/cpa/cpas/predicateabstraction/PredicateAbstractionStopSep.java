package cpaplugin.cpa.cpas.predicateabstraction;

import java.util.Collection;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.PreOrder;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.exceptions.CPAException;

public class PredicateAbstractionStopSep implements StopOperator
{
    private PredicateAbstractionDomain predicateAbstractionDomain;
    
    public PredicateAbstractionStopSep (PredicateAbstractionDomain predAbsDomain)
    {
        this.predicateAbstractionDomain = predAbsDomain;
    }
    
    public AbstractDomain getAbstractDomain ()
    {
        return predicateAbstractionDomain;
    }

    public boolean stop (AbstractElement element, Collection<AbstractElement> reached) throws CPAException
    {
        PreOrder preOrder = predicateAbstractionDomain.getPreOrder ();
        for (AbstractElement testElement : reached)
        {
            if (preOrder.satisfiesPreOrder (element, testElement))
                return true;
        }
        
        return false;
    }
}
