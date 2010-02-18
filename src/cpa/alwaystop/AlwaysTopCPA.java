package cpa.alwaystop;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.common.defaults.AbstractCPAFactory;
import cpa.common.defaults.FlatLatticeDomain;
import cpa.common.defaults.MergeSepOperator;
import cpa.common.defaults.SingletonPrecision;
import cpa.common.defaults.StaticPrecisionAdjustment;
import cpa.common.defaults.StopSepOperator;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.CPAFactory;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;

public class AlwaysTopCPA implements ConfigurableProgramAnalysis {

  private static class AlwaysTopCPAFactory extends AbstractCPAFactory {
    
    @Override
    public ConfigurableProgramAnalysis createInstance() throws CPAException {
      return new AlwaysTopCPA();
    }
  }
  
  public static CPAFactory factory() {
    return new AlwaysTopCPAFactory();
  }
  
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

  @Override
  public AbstractElement getInitialElement(CFAFunctionDefinitionNode pNode) {
    return AlwaysTopTopElement.getInstance();
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
