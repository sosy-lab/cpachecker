package cpa.common.interfaces;

import cpa.common.interfaces.AbstractElement;
import exceptions.CPAException;

public interface JoinOperator
{
    public AbstractElement join (AbstractElement element1, AbstractElement element2) throws CPAException;
}
