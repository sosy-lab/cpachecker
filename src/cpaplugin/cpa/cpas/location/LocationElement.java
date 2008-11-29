package cpaplugin.cpa.cpas.location;

import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cpa.common.interfaces.AbstractElementWithLocation;

public class LocationElement implements AbstractElementWithLocation
{
    private final CFANode locationNode;
    
    public LocationElement (CFANode locationNode)
    {
        this.locationNode = locationNode;
    }
    
    public CFANode getLocationNode ()
    {
        return locationNode;
    }
    
    @Override
    public boolean equals (Object other)
    {
        if (!(other instanceof LocationElement))
            return false;
        
        return locationNode.getNodeNumber () == ((LocationElement)other).locationNode.getNodeNumber ();
    }
    
    @Override
    public String toString ()
    {
        return Integer.toString (locationNode.getNodeNumber ());
    }
    
    @Override
    public int hashCode() {
    	return locationNode.getNodeNumber();
    }
}
