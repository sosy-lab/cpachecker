package cpa.art;

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

public class ArtCPA implements ConfigurableProgramAnalysis {

  private AbstractDomain abstractDomain;
  private TransferRelation transferRelation;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private PrecisionAdjustment precisionAdjustment;

  public ArtCPA(String mergeType, String stopType) throws CPAException {
    abstractDomain = new ArtDomain();
    transferRelation = new ArtTransferRelation();
    precisionAdjustment = new ArtPrecisionAdjustment();
    if(mergeType.equals("sep")){
      mergeOperator = new ArtMergeSep();
    }
    else if(mergeType.equals("join")){
      throw new CPAException("Location domain elements cannot be joined");
    }
    if(stopType.equals("sep")){
      stopOperator = new ArtStopSep(abstractDomain);
    }
    else if(stopType.equals("join")){
      throw new CPAException("Location domain elements cannot be joined");
    }
  }
  
  public AbstractDomain getAbstractDomain ()
  {
    return abstractDomain;
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

  public PrecisionAdjustment getPrecisionAdjustment () {
    return precisionAdjustment;
  }
  
  public AbstractElement getInitialElement (CFAFunctionDefinitionNode node) {
    return new ArtElement();
  }
  
  public Precision getInitialPrecision (CFAFunctionDefinitionNode pNode) {
    return new ArtPrecision();
  }

}
