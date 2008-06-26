package cpaplugin.cpa.common.interfaces;

import cpaplugin.common.CPAException;

public interface JoinOperator
{
    public AbstractElement join (AbstractElement element1, AbstractElement element2) throws CPAException;
}
