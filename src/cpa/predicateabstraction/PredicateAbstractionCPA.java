package cpa.predicateabstraction;

import cmdline.CPAMain;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.PrecisionDomain;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;

public class PredicateAbstractionCPA implements ConfigurableProgramAnalysis{

  private AbstractDomain abstractDomain;
  private PrecisionDomain precisionDomain;
  private TransferRelation transferRelation;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private PrecisionAdjustment precisionAdjustment;

  public PredicateAbstractionCPA (String mergeType, String stopType) throws CPAException{
    PredicateAbstractionDomain predicateAbstractionDomain = new PredicateAbstractionDomain ();

    this.precisionDomain = new PredicateAbstractionPrecisionDomain ();

    this.transferRelation = new PredicateAbstractionTransferRelation (predicateAbstractionDomain);

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

    this.precisionAdjustment = new PredicateAbstractionPrecisionAdjustment ();

    this.abstractDomain = predicateAbstractionDomain;
    this.mergeOperator = predicateAbstractionMergeOp;
    this.stopOperator = predicateAbstractionStopOp;
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
    String fileName = node.getContainingFileName();
    return new PredicateAbstractionElement (CPAMain.cpaConfig.getProperty("analysis.entryFunction"), fileName);
  }

  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    // TODO Auto-generated method stub
    return null;
  }
}
