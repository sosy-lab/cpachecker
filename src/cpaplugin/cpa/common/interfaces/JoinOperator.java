package cpaplugin.cpa.common.interfaces;

import cpaplugin.exceptions.CPAException;

public interface JoinOperator
{
    public AbstractElement join (AbstractElement element1, AbstractElement element2) throws CPAException;
}
