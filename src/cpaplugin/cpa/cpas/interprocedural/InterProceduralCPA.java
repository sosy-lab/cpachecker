package cpaplugin.cpa.cpas.interprocedural;

import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
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

	public InterProceduralCPA (String mergeType) throws CPAException{
		InterProceduralDomain interProceduralDomain = new InterProceduralDomain ();
        MergeOperator interProceduralMergeOp = null;
        if(mergeType.equals("sep")){
        	interProceduralMergeOp = new InterProceduralMergeSep (interProceduralDomain);
        }
        if(mergeType.equals("join")){
        	throw new CPAException("Interprocedural elements cannot be merged");
        }
        StopOperator interProceduralStopOp = new InterProceduralStopSep (interProceduralDomain);
        TransferRelation interProceduralRelation = new InterProceduralTransferRelation (interProceduralDomain);
		
        this.abstractDomain = interProceduralDomain;
		this.mergeOperator = interProceduralMergeOp;
		this.stopOperator = interProceduralStopOp;
		this.transferRelation = interProceduralRelation;
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
