package org.sosy_lab.cpachecker.fllesh.cpa.productautomaton;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;

public class ProductAutomatonCPA implements ConfigurableProgramAnalysis {

  private static final ProductAutomatonCPA sInstance = new ProductAutomatonCPA();
  
  public static ProductAutomatonCPA getInstance() {
    return sInstance;
  }
  
  private final ProductAutomatonDomain mDomain;
  private final StopSepOperator mStopOperator;
  
  private ProductAutomatonCPA() {
    mDomain = ProductAutomatonDomain.getInstance();
    mStopOperator = new StopSepOperator(mDomain.getPartialOrder());
  }
  
  @Override
  public ProductAutomatonDomain getAbstractDomain() {
    return mDomain;
  }

  @Override
  /*
   * This CPA can't accept in the first state.
   */
  public ProductAutomatonNonAcceptingElement getInitialElement(CFAFunctionDefinitionNode pNode) {
    return ProductAutomatonNonAcceptingElement.getInstance();
  }

  @Override
  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return SingletonPrecision.getInstance();
  }

  @Override
  public MergeOperator getMergeOperator() {
    return MergeSepOperator.getInstance();
  }

  @Override
  public ProductAutomatonPrecisionAdjustment getPrecisionAdjustment() {
    return ProductAutomatonPrecisionAdjustment.getInstance();
  }

  @Override
  public StopOperator getStopOperator() {
    return mStopOperator;
  }

  @Override
  public ProductAutomatonTransferRelation getTransferRelation() {
    return ProductAutomatonTransferRelation.getInstance();
  }

}
