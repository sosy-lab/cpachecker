package cpa.common.interfaces;

import exceptions.CPAException;

public interface MergeOperator
{
    public AbstractElement merge (AbstractElement element1, AbstractElement element2) throws CPAException;
    public AbstractElementWithLocation merge (AbstractElementWithLocation element1, AbstractElementWithLocation element2) throws CPAException;
}
