package org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common;

import com.google.common.collect.ForwardingSet;
import java.util.HashSet;
import java.util.Set;

public class ErrorIndicatorSet<I extends FaultLocalizationOutput> extends ForwardingSet<ErrorIndicator<I>> {

  private Set<ErrorIndicator<I>> delegate;

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
