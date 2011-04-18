package org.sosy_lab.cpachecker.cpa.assume;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;

/*
 * CPA that stores parameter of a prespecified external function.
 * Used for modeling assumptions. PredicateTransferRelation strengthens
 * its abstract element with respect to the given parameter. 
 * 
 */
public class AssumeCPA implements ConfigurableProgramAnalysis {

  private AssumeDomain mDomain;
  private StopOperator mStopOperator;
  private AssumeTransferRelation mTransferRelation;
  
  public static AssumeCPA getCBMCAssume() {
    return new AssumeCPA("__CPROVER_assume");
  }
  
  public AssumeCPA(String pAssumeFunctionName) {
    mDomain = new AssumeDomain();
    mStopOperator = new StopSepOperator(mDomain);
    mTransferRelation = new AssumeTransferRelation(pAssumeFunctionName);
  }
  
  @Override
  public AssumeDomain getAbstractDomain() {
    return mDomain;
  }

  @Override
  public AssumeElement getInitialElement(CFANode pNode) {
    return UnconstrainedAssumeElement.getInstance();
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return SingletonPrecision.getInstance();
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
  public AssumeTransferRelation getTransferRelation() {
    return mTransferRelation;
  }

}
