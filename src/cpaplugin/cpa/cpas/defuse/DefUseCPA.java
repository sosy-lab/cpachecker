package cpaplugin.cpa.cpas.defuse;

import java.util.ArrayList;
import java.util.List;

import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.c.FunctionDefinitionNode;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.ConfigurableProblemAnalysis;
import cpaplugin.cpa.common.interfaces.MergeOperator;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.exceptions.CPAException;

public class DefUseCPA implements ConfigurableProblemAnalysis{

	private AbstractDomain abstractDomain;
	private MergeOperator mergeOperator;
	private StopOperator stopOperator;
	private TransferRelation transferRelation;

	public DefUseCPA (String mergeType, String stopType) throws CPAException{
		DefUseDomain defUseDomain = new DefUseDomain ();
		MergeOperator defUseMergeOp = null;
		if(mergeType.equals("sep")){
			defUseMergeOp = new DefUseMergeSep (defUseDomain);
		}
		if(mergeType.equals("join")){
			defUseMergeOp = new DefUseMergeJoin (defUseDomain);
		}

		StopOperator defUseStopOp = null;

		if(stopType.equals("sep")){
			defUseStopOp = new DefUseStopSep (defUseDomain);
		}
		if(stopType.equals("join")){
			defUseStopOp = new DefUseStopJoin (defUseDomain);
		}

		TransferRelation defUseTransferRelation = new DefUseTransferRelation (defUseDomain);

		this.abstractDomain = defUseDomain;
		this.mergeOperator = defUseMergeOp;
		this.stopOperator = defUseStopOp;
		this.transferRelation = defUseTransferRelation;
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
        List<DefUseDefinition> defUseDefinitions = null;
        if (node instanceof FunctionDefinitionNode)
        {
            List<String> parameterNames = ((FunctionDefinitionNode)node).getFunctionParameterNames ();
            defUseDefinitions = new ArrayList<DefUseDefinition> ();
            
            for (String parameterName : parameterNames)
            {
                DefUseDefinition newDef = new DefUseDefinition (parameterName, null);
                defUseDefinitions.add (newDef);
            }
        }

        return new DefUseElement (defUseDefinitions);
    }

}
