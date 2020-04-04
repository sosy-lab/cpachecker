package org.sosy_lab.cpachecker.util.faultlocalization;

import org.sosy_lab.cpachecker.util.faultlocalization.ranking.NoContextExplanation;

@FunctionalInterface
public interface FaultExplanation {

  /**
   * Map a set of FaultContributions to an explanation string.
   * This string can be used by FaultReason as a description.
   * For an example see NoContextExplanation.
   * @see NoContextExplanation
   * @param subset set to find a reason for
   * @return explanation as string
   */
  String explanationFor(Fault subset);
}
