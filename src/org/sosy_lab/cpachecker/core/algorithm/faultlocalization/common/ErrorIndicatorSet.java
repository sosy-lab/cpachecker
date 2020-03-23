package org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common;

import com.google.common.collect.ForwardingSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ErrorIndicatorSet<I extends FaultLocalizationOutput> extends ForwardingSet<Set<I>> {

  private Set<Set<I>> delegate;

  public ErrorIndicatorSet(Set<Set<I>> pSet) {
    delegate = pSet;
  }

  public ErrorIndicatorSet() {
    delegate = new HashSet<>();
  }

  @Override
  protected Set<Set<I>> delegate() {
    return delegate;
  }

  public void add(I pSingleton) {
    delegate.add(Collections.singleton(pSingleton));
  }
}
