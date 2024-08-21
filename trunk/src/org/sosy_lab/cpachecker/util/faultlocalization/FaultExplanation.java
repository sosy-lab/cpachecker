// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.faultlocalization;

import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.explanation.NoContextExplanation;

/**
 * Every reason needs a description. If there is a similar way to map Faults to a description a
 * FaultExplanation can be created. After processing a Fault it returns a String on why this Fault
 * leads to an error.
 */
@FunctionalInterface
public interface FaultExplanation {

  /**
   * Map a set of FaultContributions to an explanation string. This string can be used by RankInfo
   * as a description. For an example see NoContextExplanation.
   *
   * @param subset set to find a reason for
   * @return explanation as string
   * @see NoContextExplanation
   */
  String explanationFor(Fault subset);

  static void explain(Fault pFault, FaultExplanation... pExplanations) {
    for (FaultExplanation explanation : pExplanations) {
      String explanationString = explanation.explanationFor(pFault);
      if (!explanationString.isBlank()) {
        pFault.addInfo(FaultInfo.fix(explanationString));
      }
    }
  }
}
