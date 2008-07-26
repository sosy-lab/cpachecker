package cpaplugin.cpa.cpas.octagon;

import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.ConfigurableProblemAnalysis;
import cpaplugin.cpa.common.interfaces.MergeOperator;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.exceptions.CPAException;

public class OctagonCPA implements ConfigurableProblemAnalysis{

	private AbstractDomain abstractDomain;
	private MergeOperator mergeOperator;
	private StopOperator stopOperator;
	private TransferRelation transferRelation;

	public OctagonCPA (String mergeType, String stopType) throws CPAException{
		OctDomain octagonDomain = new OctDomain ();
		MergeOperator octagonMergeOp = null;
		if(mergeType.equals("sep")){
			System.out.println("mergesep");
			octagonMergeOp = new OctMergeSep (octagonDomain);
		}
		else if(mergeType.equals("join")){
			System.out.println("mergejoin");
			octagonMergeOp = new OctMergeJoin (octagonDomain);
		}

		StopOperator octagonStopOp = null;

		if(stopType.equals("sep")){
			octagonStopOp = new OctStopSep (octagonDomain);
		}
		else if(stopType.equals("join")){
			octagonStopOp = new OctStopJoin (octagonDomain);
		}

		TransferRelation octagonTransferRelation = new OctTransferRelation (octagonDomain);

		this.abstractDomain = octagonDomain;
		this.mergeOperator = octagonMergeOp;
		this.stopOperator = octagonStopOp;
		this.transferRelation = octagonTransferRelation;
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
