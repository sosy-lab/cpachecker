package cpaplugin.cpa.domains.location;

import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cpa.common.interfaces.AbstractElement;

public class LocationElement implements AbstractElement
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
