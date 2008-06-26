package cpaplugin.cpa.common.interfaces;

import java.util.Collection;

import cpaplugin.common.CPAException;

public interface StopOperator
{
    public AbstractDomain getAbstractDomain ();
    public boolean stop (AbstractElement element, Collection<AbstractElement> reached) throws CPAException;
}
