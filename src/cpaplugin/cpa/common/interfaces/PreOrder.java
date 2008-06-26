package cpaplugin.cpa.common.interfaces;

import cpaplugin.common.CPAException;

public interface PreOrder
{
    public boolean satisfiesPreOrder (AbstractElement element1, AbstractElement element2) throws CPAException;
}
