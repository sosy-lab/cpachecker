package compositeCPA;

import java.util.ArrayList;
import java.util.List;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAEdgeType;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.CallToReturnEdge;

import exceptions.CPATransferException;
import cpa.common.CallElement;
import cpa.common.CallStack;
import cpa.common.CompositeDomain;
import cpa.common.CompositeElement;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;

public class CompositeTransferRelation implements TransferRelation{

	private final CompositeDomain compositeDomain;
	private final List<TransferRelation> transferRelations;

	// private LocationTransferRelation locationTransferRelation;

	public CompositeTransferRelation (CompositeDomain compositeDomain, List<TransferRelation> transferRelations)
	{
		this.compositeDomain = compositeDomain;
		this.transferRelations = transferRelations;

		//TransferRelation first = transferRelations.get (0);
		//if (first instanceof LocationTransferRelation)
		//{
		//	locationTransferRelation = (LocationTransferRelation) first;
		//}
	}

	public AbstractDomain getAbstractDomain ()
	{
		return compositeDomain;
	}

	public AbstractElement getAbstractSuccessor (AbstractElement element, CFAEdge cfaEdge, Precision precision) throws CPATransferException
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

			successor = transfer.getAbstractSuccessor (subElement, cfaEdge, precision);
			resultingElements.add (successor);
		}

		CompositeElement successorState = new CompositeElement (resultingElements, updatedCallStack);
		return successorState;
	}

	public List<AbstractElementWithLocation> getAllAbstractSuccessors (AbstractElementWithLocation element, Precision precision) throws CPAException, CPATransferException
	{

		//TODO CPACheckerStatistics.noOfTransferRelations++;

		CompositeElement compositeElement = (CompositeElement) element;
		CFANode node = compositeElement.getLocationNode();

		List<AbstractElementWithLocation> results = new ArrayList<AbstractElementWithLocation> ();

		for (int edgeIdx = 0; edgeIdx < node.getNumLeavingEdges (); edgeIdx++)
		{
			CFAEdge edge = node.getLeavingEdge (edgeIdx);
			results.add ((CompositeElement) getAbstractSuccessor (element, edge, precision));
		}

		return results;
	}
}
