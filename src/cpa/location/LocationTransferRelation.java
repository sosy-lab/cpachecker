package cpa.location;

import java.util.ArrayList;
import java.util.List;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.CallToReturnEdge;

import exceptions.CPATransferException;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;

public class LocationTransferRelation implements TransferRelation
{
    private final LocationDomain locationDomain;

    public LocationTransferRelation (LocationDomain locationDomain)
    {
        this.locationDomain = locationDomain;
    }

    public AbstractDomain getAbstractDomain ()
    {
        return locationDomain;
    }

    public AbstractElement getAbstractSuccessor (AbstractElement element, CFAEdge cfaEdge, Precision prec) throws CPATransferException
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

        if (node.getLeavingSummaryEdge() != null){
        	CallToReturnEdge summaryEdge = node.getLeavingSummaryEdge();
        	return new LocationElement (summaryEdge.getSuccessor());
        }

        return locationDomain.getBottomElement ();
    }

    public List<AbstractElementWithLocation> getAllAbstractSuccessors (AbstractElementWithLocation element, Precision prec) throws CPATransferException
    {
        CFANode node = element.getLocationNode ();

        List<AbstractElementWithLocation> allSuccessors = new ArrayList<AbstractElementWithLocation> ();
        int numLeavingEdges = node.getNumLeavingEdges ();

        for (int edgeIdx = 0; edgeIdx < numLeavingEdges; edgeIdx++)
        {
            CFAEdge tempEdge = node.getLeavingEdge (edgeIdx);
            allSuccessors.add (new LocationElement (tempEdge.getSuccessor ()));
        }

        return allSuccessors;
    }
}
