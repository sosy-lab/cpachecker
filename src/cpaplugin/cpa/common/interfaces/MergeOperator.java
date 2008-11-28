package cpaplugin.cpa.common.interfaces;

import cpaplugin.exceptions.CPAException;

public interface MergeOperator
{
    public AbstractElement merge (AbstractElement element1, AbstractElement element2) throws CPAException;
}
