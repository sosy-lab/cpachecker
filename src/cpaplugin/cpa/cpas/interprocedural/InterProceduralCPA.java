package cpaplugin.cpa.cpas.interprocedural;

import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.compositeCPA.MergeType;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.ConfigurableProblemAnalysis;
import cpaplugin.cpa.common.interfaces.MergeOperator;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.exceptions.CPAException;

public class InterProceduralCPA implements ConfigurableProblemAnalysis{

	private AbstractDomain abstractDomain;
	private MergeOperator mergeOperator;
	private StopOperator stopOperator;
	private TransferRelation transferRelation;

	private InterProceduralCPA (AbstractDomain abstractDomain,
			MergeOperator mergeOperator,
			StopOperator stopOperator,
			TransferRelation transferRelation)
	{
		this.abstractDomain = abstractDomain;
		this.mergeOperator = mergeOperator;
		this.stopOperator = stopOperator;
		this.transferRelation = transferRelation;
	}

	public static InterProceduralCPA createInterProceduralCPA (AbstractDomain abstractDomain,
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

		return new InterProceduralCPA (abstractDomain, mergeOperator, stopOperator, transferRelation);
	}
	
	public static InterProceduralCPA createNewInterProceduralCPA(MergeType mergeType) throws CPAException{
		InterProceduralDomain interProceduralDomain = new InterProceduralDomain ();
        MergeOperator interProceduralMergeOp = null;
        if(mergeType == MergeType.MergeSep){
        	interProceduralMergeOp = new InterProceduralMergeSep (interProceduralDomain);
        }
        if(mergeType == MergeType.MergeJoin){
        	throw new CPAException("Interprocedural elements cannot be merged");
        }
        StopOperator interProceduralStopOp = new InterProceduralStopSep (interProceduralDomain);
        TransferRelation interProceduralRelation = new InterProceduralTransferRelation (interProceduralDomain);
		
		return new InterProceduralCPA (interProceduralDomain, interProceduralMergeOp, interProceduralStopOp, interProceduralRelation);
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
		return new InterProceduralElement();
	}
}
