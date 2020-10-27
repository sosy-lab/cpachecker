// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.faultlocalization;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo.InfoType;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.RankInfo;

/**
 * Provides a variety of methods that are useful for ranking and assigning scores.
 */
public class FaultRankingUtils {

  private static Function<List<FaultInfo>, Double> evaluationFunction =
      r ->
          r.stream()
              .filter(c -> c.getType().equals(InfoType.RANK_INFO))
              .mapToDouble(FaultInfo::getScore)
              .average()
              .orElse(0);


  public static FaultScoring concatHeuristics(FaultScoring... pRanking) {
    return new FaultScoring() {

      @Override
      public RankInfo scoreFault(Fault fault) {
        return FaultInfo.rankInfo("After concatenating rankings there is no need to call scoreFault.",0);
      }

      @Override
      public void balancedScore(Set<Fault> faults) {
        for (FaultScoring faultScoring : pRanking) {
          faultScoring.balancedScore(faults);
        }
      }
    };
  }


  /**
   * Assign a score to a Fault with the default score evaluation function (average of all likelihoods).
   * When implementing an own method that assigns a score to a Fault make sure that hints are not included in the calculation.
   * @param fault Assigns a score to the Fault.
   */
  public static void assignScoreTo(Fault fault){
    fault.setScore(evaluationFunction.apply(fault.getInfos()));
  }

  /**
   * Assign a score to a FaultContribution with the default score evaluation function (average of all likelihoods).
   * When implementing an own method that assigns a score to a FaultContribution make sure that hints are not included in the calculation.
   * @param faultContribution Assigns a score to the FaultContribution.
   */
  public static void assignScoreTo(FaultContribution faultContribution){
    faultContribution.setScore(evaluationFunction.apply(faultContribution.getInfos()));
  }

}
