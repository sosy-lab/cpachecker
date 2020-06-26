// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.faultlocalization.ranking;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRanking;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;

public class HintRanking implements FaultRanking {
  private int maxNumberOfHints;

  /**
   * Create hints for the first pMaxNumberOfHints sets in the ErrorIndicatorSet
   * @param pMaxNumberOfHints number of hints to be printed. Passing -1 leads to hints for all elements in the set.
   */
  public HintRanking(int pMaxNumberOfHints){
    maxNumberOfHints=pMaxNumberOfHints;
  }

  @Override
  public List<Fault> rank(
      Set<Fault> result) {
    // if maxNumberOfHints is negative create hints for all elements in the set.
    boolean maxNumberOfHintsNegative = maxNumberOfHints < 0;
    Set<FaultContribution> alreadyAttached = new HashSet<>();
    for (Fault faultLocalizationOutputs : result) {
      int hints = 0;
      for (FaultContribution faultContribution : faultLocalizationOutputs) {
        FaultInfo
            potFix = FaultInfo.possibleFixFor(new Fault(faultContribution));
        if(maxNumberOfHintsNegative || hints < maxNumberOfHints){
          faultLocalizationOutputs.addInfo(potFix);
        }
        //Prevent attaching the same hint twice
        if(!alreadyAttached.contains(faultContribution)) {
          faultContribution.addInfo(potFix);
          alreadyAttached.add(faultContribution);
        }
        hints++;
      }
    }
    return new ArrayList<>(result);
  }
}
