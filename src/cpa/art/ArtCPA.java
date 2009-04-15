package cpa.art;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
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

  private ConfigurableProgramAnalysis wrappedCPA;

  public ArtCPA(String mergeType, String stopType, ConfigurableProgramAnalysis cpa) throws CPAException {
    wrappedCPA = cpa;
    abstractDomain = new ArtDomain();
    transferRelation = new ArtTransferRelation(cpa.getTransferRelation());
    precisionAdjustment = new ArtPrecisionAdjustment();
    if(mergeType.equals("sep")){
      mergeOperator = new ArtMergeSep();
    }
    else if(mergeType.equals("join")){
      throw new CPAException("Location domain elements cannot be joined");
    }
    if(stopType.equals("sep")){
      stopOperator = new ArtStopSep(abstractDomain, wrappedCPA);
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

  public AbstractElement getInitialElement (CFAFunctionDefinitionNode pNode) {
    return new ArtElement((AbstractElementWithLocation)wrappedCPA.getInitialElement(pNode), 
        null);
  }

  public Precision getInitialPrecision 
  (CFAFunctionDefinitionNode pNode) {
    return new ArtPrecision(wrappedCPA.getInitialPrecision(pNode));
  }

  public static ConfigurableProgramAnalysis getCompositeCPA 
  (CFAFunctionDefinitionNode node, ConfigurableProgramAnalysis cpa) throws CPAException{
    // TODO we assume that we always use sep-sep for merge and join
    return new ArtCPA("sep", "sep", cpa);
  }
}
