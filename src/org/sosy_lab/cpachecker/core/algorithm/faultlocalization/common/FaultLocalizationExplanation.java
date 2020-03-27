package org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common;

import java.util.Set;

@FunctionalInterface
public interface FaultLocalizationExplanation {

  //Use singleton sets for explanation for edges
  String explanationFor(Set<? extends FaultLocalizationOutput> subset);
}
