package org.sosy_lab.cpachecker.fllesh.cpa.productautomaton;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

public class ProductAutomatonCPA implements ConfigurableProgramAnalysis {

  private static ProductAutomatonCPA mInstance = new ProductAutomatonCPA();
  
  public static ProductAutomatonCPA getInstance() {
    return mInstance;
  }
  
  private ProductAutomatonDomain mDomain;
  private ProductAutomatonTransferRelation mTransferRelation;
  private StopSepOperator mStopOperator;
  
  public ProductAutomatonCPA() {
    mDomain = new ProductAutomatonDomain();
    mTransferRelation = new ProductAutomatonTransferRelation();
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
  public PrecisionAdjustment getPrecisionAdjustment() {
    return ProductAutomatonPrecisionAdjustment.getInstance();
  }

  @Override
  public StopOperator getStopOperator() {
    return mStopOperator;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return mTransferRelation;
  }

}
