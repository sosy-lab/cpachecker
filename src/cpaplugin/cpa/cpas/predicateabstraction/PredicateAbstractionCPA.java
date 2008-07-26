package cpaplugin.cpa.cpas.predicateabstraction;

import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cmdline.CPAMain;
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

	public PredicateAbstractionCPA (String mergeType, String stopType) throws CPAException{
		System.out.println("this is called");
		PredicateAbstractionDomain predicateAbstractionDomain = new PredicateAbstractionDomain ();
		MergeOperator predicateAbstractionMergeOp = null;
		if(mergeType.equals("sep")){
			predicateAbstractionMergeOp = new PredicateAbstractionMergeSep (predicateAbstractionDomain);
		}
		else if(mergeType.equals("join")){
			predicateAbstractionMergeOp = new PredicateAbstractionMergeJoin (predicateAbstractionDomain);
		}

		StopOperator predicateAbstractionStopOp = null;

		if(stopType.equals("sep")){
			predicateAbstractionStopOp = new PredicateAbstractionStopSep (predicateAbstractionDomain);
		}
		else if(stopType.equals("join")){
			predicateAbstractionStopOp = new PredicateAbstractionStopJoin (predicateAbstractionDomain);
		}

		TransferRelation predicateAbstractionTransferRelation = new PredicateAbstractionTransferRelation (predicateAbstractionDomain);

		this.abstractDomain = predicateAbstractionDomain;
		this.mergeOperator = predicateAbstractionMergeOp;
		this.stopOperator = predicateAbstractionStopOp;
		this.transferRelation = predicateAbstractionTransferRelation;
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
		return new PredicateAbstractionElement (CPAMain.cpaConfig.getProperty("analysis.entryFunction"), fileName);
	}

}
