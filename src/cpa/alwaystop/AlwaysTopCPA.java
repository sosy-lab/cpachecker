package cpa.alwaystop;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.common.defaults.FlatLatticeDomain;
import cpa.common.defaults.MergeSepOperator;
import cpa.common.defaults.StaticPrecisionAdjustment;
import cpa.common.defaults.SingletonPrecision;
import cpa.common.defaults.StopSepOperator;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;

public class AlwaysTopCPA implements ConfigurableProgramAnalysis {

  FlatLatticeDomain mDomain;
  MergeSepOperator mMergeOperator;
  StopSepOperator mStopOperator;
  
  public AlwaysTopCPA(String pMergeType, String pStopType) {
    this();
  }
  
  public AlwaysTopCPA() {
    mDomain = new FlatLatticeDomain(AlwaysTopTopElement.getInstance(), AlwaysTopBottomElement.getInstance());
    mMergeOperator = new MergeSepOperator();
    mStopOperator = new StopSepOperator(mDomain.getPartialOrder());
  }
  
  @Override
  public AbstractDomain getAbstractDomain() {
    return mDomain;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <AE extends AbstractElement> AE getInitialElement(
      CFAFunctionDefinitionNode pNode) {
    return (AE) AlwaysTopTopElement.getInstance();
  }

  @Override
  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return SingletonPrecision.getInstance();
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mMergeOperator;
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
  public TransferRelation getTransferRelation() {
    return AlwaysTopTransferRelation.getInstance();
  }

}
