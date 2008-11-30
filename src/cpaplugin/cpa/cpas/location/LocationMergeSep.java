package cpaplugin.cpa.cpas.location;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.MergeOperator;

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
