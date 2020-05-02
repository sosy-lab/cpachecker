/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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
