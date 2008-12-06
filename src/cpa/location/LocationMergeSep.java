package cpa.location;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.MergeOperator;
import exceptions.CPAException;

public class LocationMergeSep implements MergeOperator
{
    private final LocationDomain locationDomain;

    public LocationMergeSep (LocationDomain locationDomain)
    {
        this.locationDomain = locationDomain;
    }

    public AbstractDomain getAbstractDomain ()
    {
        return locationDomain;
    }

    public AbstractElement merge (AbstractElement element1, AbstractElement element2)
    {
        return element2;
    }

    public AbstractElementWithLocation merge(AbstractElementWithLocation pElement1,
                                             AbstractElementWithLocation pElement2) throws CPAException {
        return pElement2;
    }
}
