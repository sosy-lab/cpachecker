package org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common;

import com.google.common.collect.ForwardingSet;
import java.util.HashSet;
import java.util.Set;

public class ErrorIndicatorSet<I extends FaultLocalizationOutput> extends ForwardingSet<ErrorIndicator<I>> {

  private Set<ErrorIndicator<I>> delegate;

  /**
   * Delegates a set of ErrorIndicators.
   * To obtain a ErrorIndicatorSet of a Set of Sets of CFAEdges call <code>FaultLocalizationInfo.transform(...)</code>
   * @param pSet set to delegate
   */
  public ErrorIndicatorSet(Set<ErrorIndicator<I>> pSet) {
    delegate = pSet;
  }

  public ErrorIndicatorSet() {
    delegate = new HashSet<>();
  }

  @Override
  protected Set<ErrorIndicator<I>> delegate() {
    return delegate;
  }

}
