package cpa.location;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.MergeOperator;

public class LocationMergeSep implements MergeOperator
{
    private LocationDomain locationDomain;

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
}
