package cpaplugin.cpa.cpas.predicateabstraction;

import java.util.ArrayList;
import java.util.List;

import cpaplugin.CPAConfig;
import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.c.FunctionDefinitionNode;
import cpaplugin.compositeCPA.MergeType;
import cpaplugin.compositeCPA.StopType;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.ConfigurableProblemAnalysis;
import cpaplugin.cpa.common.interfaces.MergeOperator;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.exceptions.CPAException;

public class PredicateAbstractionCPA implements ConfigurableProblemAnalysis{

	private AbstractDomain abstractDomain;
	private MergeOperator mergeOperator;
	private StopOperator stopOperator;
	private TransferRelation transferRelation;

	private PredicateAbstractionCPA (AbstractDomain abstractDomain,
			MergeOperator mergeOperator,
			StopOperator stopOperator,
			TransferRelation transferRelation)
	{
		this.abstractDomain = abstractDomain;
		this.mergeOperator = mergeOperator;
		this.stopOperator = stopOperator;
		this.transferRelation = transferRelation;
	}

	public static PredicateAbstractionCPA createPredicateAbstractionCPA (AbstractDomain abstractDomain,
			MergeOperator mergeOperator,
			StopOperator stopOperator,
			TransferRelation transferRelation)
	{
		if (abstractDomain == null || mergeOperator == null ||
				stopOperator == null || transferRelation == null)
			return null;

		if (mergeOperator.getAbstractDomain () != abstractDomain ||
				stopOperator.getAbstractDomain () != abstractDomain ||
				transferRelation.getAbstractDomain () != abstractDomain)
			return null;

		return new PredicateAbstractionCPA (abstractDomain, mergeOperator, stopOperator, transferRelation);
	}

	public static PredicateAbstractionCPA createNewPredicateAbstractionCPA (MergeType mergeType, StopType stopType) throws CPAException{
		PredicateAbstractionDomain predicateAbstractionDomain = new PredicateAbstractionDomain ();
		MergeOperator predicateAbstractionMergeOp = null;
		if(mergeType == MergeType.MergeSep){
			predicateAbstractionMergeOp = new PredicateAbstractionMergeSep (predicateAbstractionDomain);
		}
		else if(mergeType == MergeType.MergeJoin){
			predicateAbstractionMergeOp = new PredicateAbstractionMergeJoin (predicateAbstractionDomain);
		}

		StopOperator predicateAbstractionStopOp = null;

		if(stopType == StopType.StopSep){
			predicateAbstractionStopOp = new PredicateAbstractionStopSep (predicateAbstractionDomain);
		}
		else if(stopType == StopType.StopJoin){
			predicateAbstractionStopOp = new PredicateAbstractionStopJoin (predicateAbstractionDomain);
		}

		TransferRelation predicateAbstractionTransferRelation = new PredicateAbstractionTransferRelation (predicateAbstractionDomain);

		return new PredicateAbstractionCPA (predicateAbstractionDomain, predicateAbstractionMergeOp, predicateAbstractionStopOp, predicateAbstractionTransferRelation);
	}

	public AbstractDomain getAbstractDomain ()
	{
		return abstractDomain;
	}

	public MergeOperator getMergeOperator ()
	{
		return mergeOperator;
	}

	public StopOperator getStopOperator ()
	{
		return stopOperator;
	}

	public TransferRelation getTransferRelation ()
	{
		return transferRelation;
	}

	public AbstractElement getInitialElement (CFAFunctionDefinitionNode node)
	{
		String fileName = node.getContainingFileName(); 
		return new PredicateAbstractionElement (CPAConfig.entryFunction, fileName);
	}

}
