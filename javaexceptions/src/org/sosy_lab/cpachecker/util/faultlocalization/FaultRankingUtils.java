// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.faultlocalization;

import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo.InfoType;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.RankInfo;

/** Provides a variety of methods that are useful for ranking and assigning scores. */
public class FaultRankingUtils {

  public static final String NON_VARIABLE_TOKENS = "[\\[|\\]|(|)|{|}|<|>|=|\\+|\\-|\\*|/|%|!|;]";
  public static final Pattern BLANK_CHARACTERS = Pattern.compile("\\p{javaSpaceChar}+");

  private static double computeScore(List<FaultInfo> faultInfos) {
    return faultInfos.stream()
        .filter(c -> c.getType().equals(InfoType.RANK_INFO))
        .mapToDouble(FaultInfo::getScore)
        .average()
        .orElse(0);
  }

  public static FaultScoring concatHeuristics(FaultScoring... pRanking) {
    return new FaultScoring() {

      @Override
      public RankInfo scoreFault(Fault fault) {
        throw new UnsupportedOperationException(
            "Calling method 'scoreFault' after concatenating heuristics not possible.");
      }

      @Override
      public void balancedScore(Collection<Fault> faults) {
        for (FaultScoring faultScoring : pRanking) {
          faultScoring.balancedScore(faults);
        }
      }
    };
  }

  public static ImmutableList<Fault> rank(FaultScoring scoring, Collection<Fault> faults) {
    scoring.balancedScore(faults);
    List<Fault> rankedList = new ArrayList<>();
    for (Fault fault : faults) {
      FaultRankingUtils.assignScoreTo(fault);
      for (FaultContribution faultContribution : fault) {
        FaultRankingUtils.assignScoreTo(faultContribution);
      }
      rankedList.add(fault);
    }
    return ImmutableList.sortedCopyOf(rankedList);
  }

  /**
   * Assign a score to a Fault with the default score evaluation function (average of all
   * likelihoods). When implementing an own method that assigns a score to a Fault make sure that
   * hints are not included in the calculation.
   *
   * @param fault Assigns a score to the Fault.
   */
  public static void assignScoreTo(Fault fault) {
    fault.setScore(computeScore(fault.getInfos()));
  }

  /**
   * Assign a score to a FaultContribution with the default score evaluation function (average of
   * all likelihoods). When implementing an own method that assigns a score to a FaultContribution
   * make sure that hints are not included in the calculation.
   *
   * @param faultContribution Assigns a score to the FaultContribution.
   */
  public static void assignScoreTo(FaultContribution faultContribution) {
    faultContribution.setScore(computeScore(faultContribution.getInfos()));
  }

  public static Set<String> findTokensInFault(Fault pFault) {
    return FluentIterable.from(pFault)
        .transform(
            fc -> fc.correspondingEdge().getRawStatement().replaceAll(NON_VARIABLE_TOKENS, " "))
        .transformAndConcat(s -> Splitter.on(BLANK_CHARACTERS).splitToList(s))
        .toSet();
  }
}
