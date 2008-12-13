package cpa.common.interfaces;

import java.util.Collection;

import exceptions.CPAException;

public interface StopOperator
{
    public <AE extends AbstractElement> boolean stop (AE element, Collection<AE> reached) throws CPAException;
    public boolean stop (AbstractElement element, AbstractElement reachedElement) throws CPAException;
}
