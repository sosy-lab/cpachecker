package org.sosy_lab.cpachecker.fllesh.cpa.cfapath;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopNeverOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

public class CFAPathCPA implements ConfigurableProgramAnalysis {

  private final CFAPathDomain mDomain;
  private final CFAPathTransferRelation mTransferRelation;
  private final PrecisionAdjustment mPrecisionAdjustment;
  private final Precision mPrecision;
  private final CFAPathStandardElement mInitialElement;
  private final StopOperator mStopOperator;
  private final MergeOperator mMergeOperator;
  
  private static final CFAPathCPA sInstance = new CFAPathCPA();
  
  public static CFAPathCPA getInstance() {
    return sInstance;
  }
  
  public CFAPathCPA() {
    mDomain = CFAPathDomain.getInstance();
    mTransferRelation = new CFAPathTransferRelation();
    mPrecisionAdjustment = StaticPrecisionAdjustment.getInstance();
    mPrecision = SingletonPrecision.getInstance();
    mInitialElement = CFAPathStandardElement.getEmptyPath();
    mStopOperator = StopNeverOperator.getInstance();
    mMergeOperator = MergeSepOperator.getInstance();
  }
  
  @Override
  public AbstractDomain getAbstractDomain() {
    return mDomain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return mTransferRelation;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mMergeOperator;
  }

  @Override
  public StopOperator getStopOperator() {
    return mStopOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return mPrecisionAdjustment;
  }

  @Override
  public CFAPathElement getInitialElement(CFAFunctionDefinitionNode pNode) {
    return mInitialElement;
  }

  @Override
  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return mPrecision;
  }

}
