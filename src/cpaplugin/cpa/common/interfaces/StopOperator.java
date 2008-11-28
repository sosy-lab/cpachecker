package cpaplugin.cpa.common.interfaces;

import java.util.Collection;

import cpaplugin.exceptions.CPAException;

public interface StopOperator
{
    public AbstractDomain getAbstractDomain ();
    public boolean stop (AbstractElement element, Collection<AbstractElement> reached) throws CPAException;
    public boolean stop (AbstractElement element, AbstractElement reachedElement) throws CPAException;
    public boolean isBottomElement (AbstractElement element);
}
