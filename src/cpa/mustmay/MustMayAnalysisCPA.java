package cpa.mustmay;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.common.defaults.MergeSepOperator;
import cpa.common.defaults.StopSepOperator;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.defaults.StaticPrecisionAdjustment;

public class MustMayAnalysisCPA implements ConfigurableProgramAnalysis {
  
  MustMayAnalysisDomain mDomain;
  
  ConfigurableProgramAnalysis mMustCPA;
  ConfigurableProgramAnalysis mMayCPA;
  
  Precision mPrecision;
  
  StopOperator mStopOperator;
  
  MustMayAnalysisTransferRelation mTransferRelation;
  
  public MustMayAnalysisCPA(ConfigurableProgramAnalysis pMustCPA, ConfigurableProgramAnalysis pMayCPA) {
    assert(pMustCPA != null);
    assert(pMayCPA != null);
    
    mMustCPA = pMustCPA;
    mMayCPA = pMayCPA;
    
    AbstractDomain lMustDomain = mMustCPA.getAbstractDomain();
    AbstractDomain lMayDomain = mMayCPA.getAbstractDomain();
    
    mDomain = new MustMayAnalysisDomain(lMustDomain, lMayDomain);
    
    mStopOperator = new StopSepOperator(mDomain.getPartialOrder());
    
    mTransferRelation = new MustMayAnalysisTransferRelation(pMustCPA.getTransferRelation(), pMayCPA.getTransferRelation(), mDomain.getBottomElement());
  }
  
  @Override
  public MustMayAnalysisDomain getAbstractDomain() {
    return mDomain;
  }

  @Override
  public MustMayAnalysisElement getInitialElement(
      CFAFunctionDefinitionNode pNode) {
    AbstractElement lInitialMustElement = mMustCPA.getInitialElement(pNode);
    AbstractElement lInitialMayElement = mMayCPA.getInitialElement(pNode);
    
    return new MustMayAnalysisElement(lInitialMustElement, lInitialMayElement);
  }

  @Override
  public MustMayAnalysisPrecision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    Precision lInitialMustPrecision = mMustCPA.getInitialPrecision(pNode);
    Precision lInitialMayPrecision = mMayCPA.getInitialPrecision(pNode);
    
    return new MustMayAnalysisPrecision(lInitialMustPrecision, lInitialMayPrecision);
  }

  @Override
  public MergeOperator getMergeOperator() {
    return MergeSepOperator.getInstance();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public StopOperator getStopOperator() {
    return mStopOperator;
  }

  @Override
  public MustMayAnalysisTransferRelation getTransferRelation() {
    return mTransferRelation;
  }

}
