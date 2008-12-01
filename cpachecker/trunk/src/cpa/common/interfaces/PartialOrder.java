package cpa.common.interfaces;

import cpa.common.interfaces.AbstractElement;
import exceptions.CPAException;

public interface PartialOrder
{
    public boolean satisfiesPartialOrder (AbstractElement element1, AbstractElement element2) throws CPAException;
}
