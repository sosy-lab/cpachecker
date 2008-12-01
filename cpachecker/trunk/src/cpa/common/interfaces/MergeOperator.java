package cpa.common.interfaces;

import cpa.common.interfaces.AbstractElement;
import exceptions.CPAException;

public interface MergeOperator
{
    public AbstractElement merge (AbstractElement element1, AbstractElement element2) throws CPAException;
}
