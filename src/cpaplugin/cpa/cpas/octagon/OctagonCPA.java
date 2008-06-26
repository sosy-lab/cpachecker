package cpaplugin.cpa.cpas.octagon;

import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.compositeCPA.MergeType;
import cpaplugin.compositeCPA.StopType;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.ConfigurableProblemAnalysis;
import cpaplugin.cpa.common.interfaces.MergeOperator;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.cpa.cpas.defuse.DefUseCPA;
import cpaplugin.cpa.cpas.defuse.DefUseDomain;
import cpaplugin.cpa.cpas.defuse.DefUseMergeJoin;
import cpaplugin.cpa.cpas.defuse.DefUseMergeSep;
import cpaplugin.cpa.cpas.defuse.DefUseStopJoin;
import cpaplugin.cpa.cpas.defuse.DefUseStopSep;
import cpaplugin.cpa.cpas.defuse.DefUseTransferRelation;
import cpaplugin.exceptions.CPAException;

public class OctagonCPA implements ConfigurableProblemAnalysis{

	private AbstractDomain abstractDomain;
	private MergeOperator mergeOperator;
	private StopOperator stopOperator;
	private TransferRelation transferRelation;

	private OctagonCPA (AbstractDomain abstractDomain,
			MergeOperator mergeOperator,
			StopOperator stopOperator,
			TransferRelation transferRelation)
	{
		this.abstractDomain = abstractDomain;
		this.mergeOperator = mergeOperator;
		this.stopOperator = stopOperator;
		this.transferRelation = transferRelation;
	}

	public static OctagonCPA createLocationCPA (AbstractDomain abstractDomain,
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

		return new OctagonCPA (abstractDomain, mergeOperator, stopOperator, transferRelation);
	}
	
	public static OctagonCPA createNewDefUseCPA (MergeType mergeType, StopType stopType) throws CPAException{
		OctDomain octagonDomain = new OctDomain ();
		MergeOperator octagonMergeOp = null;
		if(mergeType == MergeType.MergeSep){
			System.out.println("mergesep");
			octagonMergeOp = new OctMergeSep (octagonDomain);
		}
		else if(mergeType == MergeType.MergeJoin){
			System.out.println("mergejoin");
			octagonMergeOp = new OctMergeJoin (octagonDomain);
		}

		StopOperator octagonStopOp = null;

		if(stopType == StopType.StopSep){
			octagonStopOp = new OctStopSep (octagonDomain);
		}
		else if(stopType == StopType.StopJoin){
			octagonStopOp = new OctStopJoin (octagonDomain);
		}

		TransferRelation octagonTransferRelation = new OctTransferRelation (octagonDomain);

		return new OctagonCPA (octagonDomain, octagonMergeOp, octagonStopOp, octagonTransferRelation);
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

	public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
		return new OctElement ();
	}
}
