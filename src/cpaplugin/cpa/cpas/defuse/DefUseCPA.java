package cpaplugin.cpa.cpas.defuse;

import java.util.ArrayList;
import java.util.List;

import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.c.FunctionDefinitionNode;
import cpaplugin.compositeCPA.MergeType;
import cpaplugin.compositeCPA.StopType;
import cpaplugin.cpa.common.CompositeElement;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.ConfigurableProblemAnalysis;
import cpaplugin.cpa.common.interfaces.MergeOperator;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.cpa.cpas.location.LocationElement;
import cpaplugin.exceptions.CPAException;

public class DefUseCPA implements ConfigurableProblemAnalysis{

	private AbstractDomain abstractDomain;
	private MergeOperator mergeOperator;
	private StopOperator stopOperator;
	private TransferRelation transferRelation;

	private DefUseCPA (AbstractDomain abstractDomain,
			MergeOperator mergeOperator,
			StopOperator stopOperator,
			TransferRelation transferRelation)
	{
		this.abstractDomain = abstractDomain;
		this.mergeOperator = mergeOperator;
		this.stopOperator = stopOperator;
		this.transferRelation = transferRelation;
	}

	public static DefUseCPA createDefUseCPA (AbstractDomain abstractDomain,
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

		return new DefUseCPA (abstractDomain, mergeOperator, stopOperator, transferRelation);
	}

	public static DefUseCPA createNewDefUseCPA (MergeType mergeType, StopType stopType) throws CPAException{
		DefUseDomain defUseDomain = new DefUseDomain ();
		MergeOperator defUseMergeOp = null;
		if(mergeType == MergeType.MergeSep){
			defUseMergeOp = new DefUseMergeSep (defUseDomain);
		}
		if(mergeType == MergeType.MergeJoin){
			defUseMergeOp = new DefUseMergeJoin (defUseDomain);
		}

		StopOperator defUseStopOp = null;

		if(stopType == StopType.StopSep){
			defUseStopOp = new DefUseStopSep (defUseDomain);
		}
		if(stopType == StopType.StopJoin){
			defUseStopOp = new DefUseStopJoin (defUseDomain);
		}

		TransferRelation defUseTransferRelation = new DefUseTransferRelation (defUseDomain);

		return new DefUseCPA (defUseDomain, defUseMergeOp, defUseStopOp, defUseTransferRelation);
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
