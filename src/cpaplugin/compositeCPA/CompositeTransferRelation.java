package cpaplugin.compositeCPA;

import java.util.ArrayList;
import java.util.List;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cpa.common.CompositeDomain;
import cpaplugin.cpa.common.CompositeElement;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.cpa.cpas.location.LocationElement;
import cpaplugin.cpa.cpas.location.LocationTransferRelation;
import cpaplugin.exceptions.CPAException;

public class CompositeTransferRelation implements TransferRelation{

	private CompositeDomain compositeDomain;
	private List<TransferRelation> transferRelations;

	private LocationTransferRelation locationTransferRelation;

	public CompositeTransferRelation (CompositeDomain compositeDomain, List<TransferRelation> transferRelations)
	{
		this.compositeDomain = compositeDomain;
		this.transferRelations = transferRelations;

		TransferRelation first = transferRelations.get (0);
		if (first instanceof LocationTransferRelation)
		{
			locationTransferRelation = (LocationTransferRelation) first;
		}
	}

	public AbstractDomain getAbstractDomain ()
	{
		return compositeDomain;
	}

	public AbstractElement getAbstractSuccessor (AbstractElement element, CFAEdge cfaEdge)
	{
		CompositeElement compositeElement = (CompositeElement) element;
		List<AbstractElement> inputElements = compositeElement.getElements ();
		List<AbstractElement> resultingElements = new ArrayList<AbstractElement> ();

		for (int idx = 0; idx < transferRelations.size (); idx++)
		{
			TransferRelation transfer = transferRelations.get (idx);
			AbstractElement subElement = inputElements.get (idx);

			AbstractElement successor = transfer.getAbstractSuccessor (subElement, cfaEdge);
			resultingElements.add (successor);
		}

		return new CompositeElement (resultingElements);
	}

	public List<AbstractElement> getAllAbstractSuccessors (AbstractElement element) throws CPAException
	{
		if (locationTransferRelation == null)
			throw new CPAException ("First abstract domain must be a location domain to call getAllAbstractSuccessors()");

		CompositeElement compositeElement = (CompositeElement) element;
		List<AbstractElement> abstractElements = compositeElement.getElements (); 
		LocationElement locationElement = (LocationElement) abstractElements.get (0);

		List<AbstractElement> results = new ArrayList<AbstractElement> ();

		CFANode node = locationElement.getLocationNode ();
		for (int edgeIdx = 0; edgeIdx < node.getNumLeavingEdges (); edgeIdx++)
		{
			CFAEdge edge = node.getLeavingEdge (edgeIdx);
			results.add (getAbstractSuccessor (element, edge));
		}

		return results;
	}
}
