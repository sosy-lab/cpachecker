package cpaplugin.cpa.common.interfaces;

import cpaplugin.exceptions.CPAException;

public interface PartialOrder
{
    public boolean satisfiesPartialOrder (AbstractElement element1, AbstractElement element2) throws CPAException;
}
