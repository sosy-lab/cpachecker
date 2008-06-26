package deprecated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cpaplugin.CPAConfig;
import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cpa.common.CompositeDomain;
import cpaplugin.cpa.common.CompositeElement;
import cpaplugin.cpa.common.SimpleCPA;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.ConfigurableProblemAnalysis;
import cpaplugin.cpa.common.interfaces.MergeOperator;
import cpaplugin.cpa.common.interfaces.PreOrder;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.cpa.cpas.interprocedural.InterProceduralDomain;
import cpaplugin.cpa.cpas.interprocedural.InterProceduralElement;
import cpaplugin.cpa.cpas.interprocedural.InterProceduralMergeSep;
import cpaplugin.cpa.cpas.interprocedural.InterProceduralStopSep;
import cpaplugin.cpa.cpas.interprocedural.InterProceduralTransferRelation;
import cpaplugin.cpa.cpas.location.LocationDomain;
import cpaplugin.cpa.cpas.location.LocationElement;
import cpaplugin.cpa.cpas.location.LocationMergeSep;
import cpaplugin.cpa.cpas.location.LocationStopSep;
import cpaplugin.cpa.cpas.location.LocationTransferRelation;
import cpaplugin.cpa.cpas.octagon.OctConstants;
import cpaplugin.cpa.cpas.octagon.OctDomain;
import cpaplugin.cpa.cpas.octagon.OctElement;
import cpaplugin.cpa.cpas.octagon.OctMergeSep;
import cpaplugin.cpa.cpas.octagon.OctStopSep;
import cpaplugin.cpa.cpas.octagon.OctTransferRelation;
import cpaplugin.cpa.cpas.octagon.OctWideningControl;
import cpaplugin.exceptions.CPAException;


public class JavaOctagonDemo {

	private static class CompositeMergeOperator implements MergeOperator
	{
		private CompositeDomain compositeDomain;
		private List<MergeOperator> mergeOperators;
		private OctWideningControl wideningControl = new OctWideningControl();

		public CompositeMergeOperator (CompositeDomain compositeDomain, List<MergeOperator> mergeOperators)
		{
			this.compositeDomain = compositeDomain;
			this.mergeOperators = mergeOperators;
		}

		public AbstractDomain getAbstractDomain ()
		{
			return compositeDomain;
		}

		public AbstractElement merge (AbstractElement element1, AbstractElement element2)
		{
			// Merge Sep Code
			CompositeElement comp1 = (CompositeElement) element1;
			CompositeElement comp2 = (CompositeElement) element2;

			LocationElement locationElement1 = (LocationElement) comp1.getElements ().get (0);
			LocationElement locationElement2 = (LocationElement) comp2.getElements ().get (0);

			OctElement octElement1 = (OctElement) comp1.getElements ().get (1);
			OctElement octElement2 = (OctElement) comp2.getElements ().get (1);

			if (!locationElement1.equals (locationElement2))
				return element2;

			List<AbstractElement> mergedElements = new ArrayList<AbstractElement> ();
			mergedElements.add (mergeOperators.get (0).merge (locationElement1, locationElement2));
			if(wideningControl.isWideningUsed(locationElement2)){
				OctConstants.useWidening = true;
			}
			mergedElements.add (mergeOperators.get (1).merge (octElement1, octElement2));
			return new CompositeElement (mergedElements);
		}
	}

	private static class CompositeStopOperator implements StopOperator
	{
		private CompositeDomain compositeDomain;
		private List<StopOperator> stopOperators;

		public CompositeStopOperator (CompositeDomain compositeDomain, List<StopOperator> stopOperators)
		{
			this.compositeDomain = compositeDomain;
			this.stopOperators = stopOperators;
		}

		public AbstractDomain getAbstractDomain ()
		{
			return compositeDomain;
		}

		public boolean stop (AbstractElement element, Collection<AbstractElement> reached) throws CPAException
		{
			PreOrder preOrder = compositeDomain.getPreOrder ();
			for (AbstractElement testElement : reached)
			{
				if (preOrder.satisfiesPreOrder (element, testElement))
				{
					return true;
				}
			}
			return false;
		}
	}

	private static class CompositeTransferRelation implements TransferRelation
	{
		private CompositeDomain compositeDomain;
		private List<TransferRelation> transferRelations;

		private LocationTransferRelation locationTransferRelation;
		private InterProceduralTransferRelation interProceduralTransferRelation;

		public CompositeTransferRelation (CompositeDomain compositeDomain, List<TransferRelation> transferRelations)
		{
			this.compositeDomain = compositeDomain;
			this.transferRelations = transferRelations;

			TransferRelation first = transferRelations.get (0);
			if (first instanceof LocationTransferRelation)
			{
				locationTransferRelation = (LocationTransferRelation) first;
			}

			if(CPAConfig.isAnalysisInterprocedural){
				TransferRelation second = transferRelations.get (1);
				if (second instanceof InterProceduralTransferRelation)
				{
					interProceduralTransferRelation = (InterProceduralTransferRelation) second;
				}
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

			if(CPAConfig.isAnalysisInterprocedural){
				if (interProceduralTransferRelation == null)
					throw new CPAException ("If the analysis is interprocedural, the second abstract domain " +
					"must be a location domain");
			}

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

	public static ConfigurableProblemAnalysis getOctDemoCPA ()
	{
		LocationDomain locationDomain = new LocationDomain ();
		MergeOperator locationMergeOp = new LocationMergeSep (locationDomain);
		StopOperator locationStopOp = new LocationStopSep (locationDomain);
		LocationTransferRelation locationTransferRelation = new LocationTransferRelation (locationDomain);

		InterProceduralDomain interProceduralDomain = new InterProceduralDomain ();
		MergeOperator interProceduralMergeOp = new InterProceduralMergeSep (interProceduralDomain);
		StopOperator interProceduralStopOp = new InterProceduralStopSep (interProceduralDomain);
		InterProceduralTransferRelation interProceduralTransferRelation = new InterProceduralTransferRelation (interProceduralDomain);
		
		OctDomain octDomain = new OctDomain ();
		MergeOperator octMergeOp = new OctMergeSep (octDomain);
		//MergeOperator octMergeOp = new OctMergeJoin (octDomain);
		StopOperator octStopOp = new OctStopSep (octDomain);
		TransferRelation octTransferRelation = new OctTransferRelation (octDomain);
		List<AbstractDomain> domains = new ArrayList<AbstractDomain> ();
		domains.add (locationDomain);
		if(CPAConfig.isAnalysisInterprocedural){
			domains.add (interProceduralDomain);
		}
		domains.add (octDomain);

		List<MergeOperator> mergeOperators = new ArrayList<MergeOperator> ();
		mergeOperators.add (locationMergeOp);
		if(CPAConfig.isAnalysisInterprocedural){
			mergeOperators.add (interProceduralMergeOp);
		}
		mergeOperators.add (octMergeOp);

		List<StopOperator> stopOperators = new ArrayList<StopOperator> ();
		stopOperators.add (locationStopOp);
		if(CPAConfig.isAnalysisInterprocedural){
			stopOperators.add (interProceduralStopOp);
		}
		stopOperators.add (octStopOp);

		List<TransferRelation> transferRelations = new ArrayList<TransferRelation> ();
		transferRelations.add (locationTransferRelation);
		if(CPAConfig.isAnalysisInterprocedural){
			transferRelations.add (interProceduralTransferRelation);
		}
		transferRelations.add (octTransferRelation);

		CompositeDomain compositeDomain = new CompositeDomain (domains);
		CompositeMergeOperator compositeMerge = new CompositeMergeOperator (compositeDomain, mergeOperators);
		CompositeStopOperator compositeStop = new CompositeStopOperator (compositeDomain, stopOperators);
		CompositeTransferRelation compositeTransfer = new CompositeTransferRelation (compositeDomain, transferRelations);

		ConfigurableProblemAnalysis cpa = SimpleCPA.createSimpleCPA (compositeDomain, compositeMerge, compositeStop, compositeTransfer);
		return cpa;
	}

	public static AbstractElement getInitialElement (CFAFunctionDefinitionNode node)
	{
		LocationElement initialLocationElement = new LocationElement (node);
		OctElement octInitialElement = new OctElement ();
		InterProceduralElement interProInitialElement = new InterProceduralElement();

		List<AbstractElement> initialElements = new ArrayList<AbstractElement> ();
		initialElements.add (initialLocationElement);
		if(CPAConfig.isAnalysisInterprocedural){
			initialElements.add (interProInitialElement);
		}
		initialElements.add (octInitialElement);

		CompositeElement initialElement = new CompositeElement (initialElements);

		return initialElement;
	}

}
