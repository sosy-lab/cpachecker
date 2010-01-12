package cpa.mustmay;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.alwaystop.AlwaysTopCPA;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import cpa.concrete.ConcreteAnalysisCPA;
import exceptions.CPAException;

public class SimpleMustMayAnalysisCPA implements ConfigurableProgramAnalysis {

  MustMayAnalysisCPA mMustMayAnalysisCPA;
  
  public SimpleMustMayAnalysisCPA(String pMergeType, String pStopType) throws CPAException {
    AlwaysTopCPA lMayCPA = new AlwaysTopCPA();
    ConcreteAnalysisCPA lMustCPA = new ConcreteAnalysisCPA();
    
    mMustMayAnalysisCPA = new MustMayAnalysisCPA(lMustCPA, lMayCPA);
  }
  
  @Override
  public <AE extends AbstractElement> AE getInitialElement(
      CFAFunctionDefinitionNode pNode) {
    return (AE) mMustMayAnalysisCPA.getInitialElement(pNode);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return mMustMayAnalysisCPA.getAbstractDomain();
  }

  @Override
  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return mMustMayAnalysisCPA.getInitialPrecision(pNode);
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mMustMayAnalysisCPA.getMergeOperator();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return mMustMayAnalysisCPA.getPrecisionAdjustment();
  }

  @Override
  public StopOperator getStopOperator() {
    return mMustMayAnalysisCPA.getStopOperator();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return mMustMayAnalysisCPA.getTransferRelation();
  }

}
