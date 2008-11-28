package cpaplugin.cpa.cpas.location;

import java.util.ArrayList;
import java.util.List;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cfa.objectmodel.c.CallToReturnEdge;
import cpaplugin.cpa.common.CPATransferException;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.TransferRelation;

/**
 * @author holzera
 *
 */
public class InverseLocationTransferRelation implements TransferRelation
{
    private LocationDomain locationDomain;

    public InverseLocationTransferRelation (LocationDomain locationDomain)
    {
        this.locationDomain = locationDomain;
    }

    public AbstractDomain getAbstractDomain ()
    {
        return locationDomain;
    }

    public AbstractElement getAbstractSuccessor (AbstractElement element, CFAEdge cfaEdge) throws CPATransferException
    {
        LocationElement inputElement = (LocationElement) element;
        CFANode node = inputElement.getLocationNode();
        
        int numEnteringEdges = node.getNumEnteringEdges();
        
        for (int edgeIdx = 0; edgeIdx < numEnteringEdges; edgeIdx++) {
        	CFAEdge testEdge = node.getEnteringEdge(edgeIdx);
        	
        	if (testEdge == cfaEdge) {
        		return new LocationElement(testEdge.getPredecessor());
        	}
        }
        
        if (node.getEnteringSummaryEdge() != null) {
        	CallToReturnEdge summaryEdge = node.getEnteringSummaryEdge();
        	return new LocationElement(summaryEdge.getPredecessor());
        }
        
        return locationDomain.getBottomElement();
    }

    public List<AbstractElement> getAllAbstractSuccessors (AbstractElement element) throws CPATransferException
    {
    	LocationElement inputElement = (LocationElement) element;
        CFANode node = inputElement.getLocationNode();
        
        List<AbstractElement> allSuccessors = new ArrayList<AbstractElement> ();
        int numEnteringEdges = node.getNumEnteringEdges ();

        for (int edgeIdx = 0; edgeIdx < numEnteringEdges; edgeIdx++)
        {
            CFAEdge tempEdge = node.getEnteringEdge(edgeIdx);
            allSuccessors.add (new LocationElement(tempEdge.getPredecessor()));
        }

        return allSuccessors;
    }
}
