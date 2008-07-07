package cpaplugin.cpa.cpas.interprocedural;

import java.util.Collection;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.PreOrder;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.exceptions.CPAException;

public class InterProceduralStopSep implements StopOperator
{
    private InterProceduralDomain ipDomain;
    
    public InterProceduralStopSep (InterProceduralDomain interProDomain)
    {
        this.ipDomain = interProDomain;
    }
    
    public AbstractDomain getAbstractDomain ()
    {
        return this.ipDomain;
    }

    public boolean stop (AbstractElement element, Collection<AbstractElement> reached) throws CPAException
    {
        PreOrder preOrder = this.ipDomain.getPreOrder ();
        for (AbstractElement testElement : reached)
        {
            if (preOrder.satisfiesPreOrder (element, testElement))
                return true;
        }
        
        return false;
    }

	public boolean isBottomElement(AbstractElement element) {
		// TODO Auto-generated method stub
		return false;
	}
}
