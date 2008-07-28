package cpaplugin.cpa.cpas.location;

import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.AbstractElementWithLocation;

public class LocationElement implements AbstractElement, AbstractElementWithLocation
{
    private CFANode locationNode;
    
    public LocationElement (CFANode locationNode)
    {
        this.locationNode = locationNode;
    }
    
    public CFANode getLocationNode ()
    {
        return locationNode;
    }
    
    public boolean equals (Object other)
    {
        if (!(other instanceof LocationElement))
            return false;
        
        return locationNode.getNodeNumber () == ((LocationElement)other).locationNode.getNodeNumber ();
    }
    
    public String toString ()
    {
        return Integer.toString (locationNode.getNodeNumber ());
    }
}
