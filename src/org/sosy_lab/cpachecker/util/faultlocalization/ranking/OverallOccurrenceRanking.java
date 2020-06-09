// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.faultlocalization.ranking;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRanking;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRankingUtils;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRankingUtils.RankingResults;

public class OverallOccurrenceRanking implements FaultRanking {

  /**
   * Count how often a certain FaultContribution is in a Fault.
   * Afterwards assign a likelihood to every fault equal to the relative frequency of all elements in result
   * @param result The result of any FaultLocalizationAlgorithm
   * @return ranked list of Faults
   */
  @Override
  public List<Fault> rank(
      Set<Fault> result) {
    Set<FaultContribution> alreadyAttached = new HashSet<>();
    Map<FaultContribution, Double> occurrence = new HashMap<>();
    double elements = 0;
    for (Fault faultLocalizationOutputs : result) {
      for (FaultContribution faultContribution : faultLocalizationOutputs) {
        occurrence.merge(faultContribution, 1d, Double::sum);
        elements++;
      }
    }

    RankingResults rankingResults = FaultRankingUtils.rankedListFor(result,
        c -> c.stream().mapToDouble(occurrence::get).sum());

    for (Entry<Fault, Double> entry : rankingResults.getLikelihoodMap().entrySet()) {
      entry.getKey().addInfo(
          FaultInfo.rankInfo("Overall occurrence of elements in this set.", entry.getValue()/elements));
      for (FaultContribution faultContribution : entry.getKey()) {
        if (!alreadyAttached.contains(faultContribution)) {
          double elementOverall = occurrence.get(faultContribution);
          faultContribution.addInfo(
              FaultInfo.rankInfo(
                  "Overall occurrence in the sets: " + (int) elementOverall,
                  elementOverall / elements));
          alreadyAttached.add(faultContribution);
        }
      }
    }

    return rankingResults.getRankedList();
  }
}
