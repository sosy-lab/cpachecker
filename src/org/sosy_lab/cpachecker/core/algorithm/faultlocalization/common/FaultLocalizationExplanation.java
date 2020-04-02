package org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common;

import java.util.Set;

@FunctionalInterface
public interface FaultLocalizationExplanation {

  /**
   * Map a set of FaultLocalizationOutputs to a explanation string.
   * This string can be used by FaultLocalizationReason as description.
   * @param subset set to find a reason for
   * @return explanation as string
   */
  String explanationFor(Set<? extends FaultLocalizationOutput> subset);
}
