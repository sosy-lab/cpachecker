package cpaplugin.compositeCPA;

import java.util.ArrayList;
import java.util.List;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.CFAEdgeType;
import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cfa.objectmodel.c.CallToReturnEdge;
import cpaplugin.cmdline.CPAMain;
import cpaplugin.cpa.common.CPATransferException;
import cpaplugin.cpa.common.CallElement;
import cpaplugin.cpa.common.CallStack;
import cpaplugin.cpa.common.CompositeDomain;
import cpaplugin.cpa.common.CompositeElement;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.AbstractElementWithLocation;
import cpaplugin.cpa.common.interfaces.TransferRelation;
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

	public AbstractElement getAbstractSuccessor (AbstractElement element, CFAEdge cfaEdge) throws CPATransferException
	{
		CompositeElement compositeElement = (CompositeElement) element;
		List<AbstractElement> inputElements = compositeElement.getElements ();
		List<AbstractElement> resultingElements = new ArrayList<AbstractElement> ();

		CallStack updatedCallStack = compositeElement.getCallStack();

		// TODO add some check here for unbounded recursive calls
		if(cfaEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge)
		{
			String functionName = cfaEdge.getSuccessor().getFunctionName();
			CFANode callNode = cfaEdge.getPredecessor();
			CallElement ce = new CallElement(functionName, callNode, compositeElement);
			CallStack cs = compositeElement.getCallStack();
			updatedCallStack = cs.clone();
			updatedCallStack.push(ce);
		}

		// handling the return from a function		
		else if(cfaEdge.getEdgeType() == CFAEdgeType.ReturnEdge)
		{
			CallElement topCallElement = compositeElement.getCallStack().peek();
			assert(cfaEdge.getPredecessor().getFunctionName().
					equals(topCallElement.getFunctionName()));
			CallElement returnElement = compositeElement.getCallStack().getSecondTopElement();

			if(! topCallElement.isConsistent(cfaEdge.getSuccessor()) ||
					! returnElement.isConsistent(cfaEdge.getSuccessor().getFunctionName()) ){
				return compositeDomain.getBottomElement();
			}

			// TODO we are saving the abstract state on summary edge, that works for
			// now but this is a terrible design practice. Add another method
			// getAbstractSuccessorOnReturn(subElement, prevElement, cfaEdge)
			// and implement it for all CPAs later.
			else{
				CallStack cs = compositeElement.getCallStack();
				updatedCallStack = cs.clone();
				CallElement ce = updatedCallStack.pop();
				CompositeElement compElemBeforeCall = ce.getState();
				// TODO use summary edge as a cache later
				CallToReturnEdge summaryEdge = cfaEdge.getSuccessor().getEnteringSummaryEdge();
				summaryEdge.setAbstractElement(compElemBeforeCall);
			}
		}
		
		for (int idx = 0; idx < transferRelations.size (); idx++)
		{
			TransferRelation transfer = transferRelations.get (idx);
			AbstractElement subElement = null;
			AbstractElement successor = null;
			subElement = inputElements.get (idx);
			// handling a call edge
			
			successor = transfer.getAbstractSuccessor (subElement, cfaEdge);
			resultingElements.add (successor);
		}
		
		CompositeElement successorState = new CompositeElement (resultingElements, updatedCallStack);
		return successorState;
	}

	public List<AbstractElement> getAllAbstractSuccessors (AbstractElement element) throws CPAException, CPATransferException
	{

		//TODO CPACheckerStatistics.noOfTransferRelations++;

		CompositeElement compositeElement = (CompositeElement) element;
		List<AbstractElement> abstractElements = compositeElement.getElements ();

		CFANode node = null;

		AbstractElement elem = abstractElements.get(0);
		if (elem instanceof AbstractElementWithLocation) {
			AbstractElementWithLocation wl = 
				(AbstractElementWithLocation)elem;
			node = wl.getLocationNode();
		} else {
			throw new CPAException("No Location information available, impossible to continue");
		}

		List<AbstractElement> results = new ArrayList<AbstractElement> ();

		for (int edgeIdx = 0; edgeIdx < node.getNumLeavingEdges (); edgeIdx++)
		{
			CFAEdge edge = node.getLeavingEdge (edgeIdx);
			results.add (getAbstractSuccessor (element, edge));
		}

		return results;
	}
}
