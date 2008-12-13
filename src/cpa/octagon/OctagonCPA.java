package cpa.octagon;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;

public class OctagonCPA implements ConfigurableProgramAnalysis{

  private AbstractDomain abstractDomain;
  private PrecisionDomain precisionDomain;
  private TransferRelation transferRelation;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private PrecisionAdjustment precisionAdjustment;

  public OctagonCPA (String mergeType, String stopType) throws CPAException{
    OctDomain octagonDomain = new OctDomain ();

    this.precisionDomain = new OctPrecisionDomain ();

    this.transferRelation = new OctTransferRelation (octagonDomain);

    MergeOperator octagonMergeOp = null;
    if(mergeType.equals("sep")){
      octagonMergeOp = new OctMergeSep (octagonDomain);
    }
    else if(mergeType.equals("join")){
      octagonMergeOp = new OctMergeJoin (octagonDomain);
    }

    StopOperator octagonStopOp = null;

    if(stopType.equals("sep")){
      octagonStopOp = new OctStopSep (octagonDomain);
    }
    else if(stopType.equals("join")){
      octagonStopOp = new OctStopJoin (octagonDomain);
    }

    this.abstractDomain = octagonDomain;
    this.mergeOperator = octagonMergeOp;
    this.stopOperator = octagonStopOp;
    this.precisionAdjustment = new OctPrecisionAdjustment ();
  }

  public AbstractDomain getAbstractDomain ()
  {
    return abstractDomain;
  }



  public TransferRelation getTransferRelation ()
  {
    return transferRelation;
  }

  public PrecisionDomain getPrecisionDomain() {
    return precisionDomain;
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

  public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
    return new OctElement ();
  }

  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    // TODO Auto-generated method stub
    return null;
  }
}
