package cpa.defuse;

import java.util.ArrayList;
import java.util.List;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.c.FunctionDefinitionNode;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.PrecisionDomain;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import cpa.defuse.DefUseDefinition;
import cpa.defuse.DefUseDomain;
import cpa.defuse.DefUseElement;
import cpa.defuse.DefUseMergeJoin;
import cpa.defuse.DefUseMergeSep;
import cpa.defuse.DefUseStopJoin;
import cpa.defuse.DefUseStopSep;
import cpa.defuse.DefUseTransferRelation;
import exceptions.CPAException;

public class DefUseCPA implements ConfigurableProgramAnalysis{

  private AbstractDomain abstractDomain;
  private PrecisionDomain precisionDomain;
  private TransferRelation transferRelation;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private PrecisionAdjustment precisionAdjustment;


  public DefUseCPA (String mergeType, String stopType) throws CPAException{
    DefUseDomain defUseDomain = new DefUseDomain ();
    this.abstractDomain = defUseDomain;

    this.precisionDomain = new DefUsePrecisionDomain ();

    this.transferRelation = new DefUseTransferRelation (defUseDomain);

    this.mergeOperator = null;
    if(mergeType.equals("sep")){
      this.mergeOperator = new DefUseMergeSep (defUseDomain);
    } else if(mergeType.equals("join")){
      this.mergeOperator = new DefUseMergeJoin (defUseDomain);
    }

    this.stopOperator = null;
    if(stopType.equals("sep")){
      this.stopOperator = new DefUseStopSep (defUseDomain);
    } else if(stopType.equals("join")){
      this.stopOperator = new DefUseStopJoin (defUseDomain);
    }

    this.precisionAdjustment = new DefUsePrecisionAdjustment ();
  }

  public AbstractDomain getAbstractDomain ()
  {
    return abstractDomain;
  }

  public PrecisionDomain getPrecisionDomain() {
    return precisionDomain;
  }

  public TransferRelation getTransferRelation ()
  {
    return transferRelation;
  }

  public MergeOperator getMergeOperator ()
  {
    return mergeOperator;
  }

  public StopOperator getStopOperator ()
  {
    return stopOperator;
  }

  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
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

  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    // TODO Auto-generated method stub
    return null;
  }
}
