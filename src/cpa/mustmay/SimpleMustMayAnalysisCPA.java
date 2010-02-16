package cpa.mustmay;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.alwaystop.AlwaysTopCPA;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.concrete.ConcreteAnalysisCPA;

public class SimpleMustMayAnalysisCPA implements ConfigurableProgramAnalysis {

  MustMayAnalysisCPA mMustMayAnalysisCPA;
  
  public SimpleMustMayAnalysisCPA(String pMergeType, String pStopType) {
    AlwaysTopCPA lMayCPA = new AlwaysTopCPA();
    ConcreteAnalysisCPA lMustCPA = new ConcreteAnalysisCPA();
    
    mMustMayAnalysisCPA = new MustMayAnalysisCPA(lMustCPA, lMayCPA);
  }
  
  @Override
  public MustMayAnalysisElement getInitialElement(
      CFAFunctionDefinitionNode pNode) {
    return mMustMayAnalysisCPA.getInitialElement(pNode);
  }

  @Override
  public MustMayAnalysisDomain getAbstractDomain() {
    return mMustMayAnalysisCPA.getAbstractDomain();
  }

  @Override
  public MustMayAnalysisPrecision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
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
  public MustMayAnalysisTransferRelation getTransferRelation() {
    return mMustMayAnalysisCPA.getTransferRelation();
  }

}
