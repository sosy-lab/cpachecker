package cpaplugin.cpa.domains.location;

import java.util.ArrayList;
import java.util.List;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.TransferRelation;

public class LocationTransferRelation implements TransferRelation
{
    private LocationDomain locationDomain;

    public LocationTransferRelation (LocationDomain locationDomain)
    {
        this.locationDomain = locationDomain;
    }

    public AbstractDomain getAbstractDomain ()
    {
        return locationDomain;
    }

    public AbstractElement getAbstractSuccessor (AbstractElement element, CFAEdge cfaEdge)
    {
        LocationElement inputElement = (LocationElement) element;
        CFANode node = inputElement.getLocationNode ();

        int numLeavingEdges = node.getNumLeavingEdges ();
        for (int edgeIdx = 0; edgeIdx < numLeavingEdges; edgeIdx++)
        {
            CFAEdge testEdge = node.getLeavingEdge (edgeIdx);
            if (testEdge == cfaEdge)
            {
                return new LocationElement (testEdge.getSuccessor ());
            }
        }

        return locationDomain.getBottomElement ();
    }

    public List<AbstractElement> getAllAbstractSuccessors (AbstractElement element)
    {
        LocationElement inputElement = (LocationElement) element;
        CFANode node = inputElement.getLocationNode ();

        List<AbstractElement> allSuccessors = new ArrayList<AbstractElement> ();
        int numLeavingEdges = node.getNumLeavingEdges ();

        for (int edgeIdx = 0; edgeIdx < numLeavingEdges; edgeIdx++)
        {
            CFAEdge tempEdge = node.getLeavingEdge (edgeIdx);
            allSuccessors.add (new LocationElement (tempEdge.getSuccessor ()));
        }

        return allSuccessors;
    }
}
