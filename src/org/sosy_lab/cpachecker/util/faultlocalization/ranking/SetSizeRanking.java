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
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;

import java.util.stream.Collectors;

import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRanking;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRankingUtils;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultReason;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRankingUtils.RankingResults;

public class SetSizeRanking implements FaultRanking {

  /**
   * Ranks the sets according to the size of the Faults. The smaller a set
   * the higher the rank.
   * @param result The result of any FaultLocalizationAlgorithm
   * @return Ranked list of faults
   */
  @Override
  public List<Fault> rank(
      Set<Fault> result) {

    if (result.isEmpty()) {
      return ImmutableList.of();
    }
    RankingResults ranking = FaultRankingUtils.rankedListFor(result,
        e -> 1d/e.size());

    if(ranking.getRankedList().size()==1){
      Fault current = ranking.getRankedList().get(0);
      current.addReason(FaultReason.justify(
              "The set has a size of " + current.size() + ".",
              1d));
      return ranking.getRankedList();
    }

    List<Double> sortedLikelihood = ranking.getLikelihoodMap().values().stream().distinct().sorted().collect(Collectors.toList());
    Map<Double, Integer> index = new HashMap<>();
    for(int i = 0; i < sortedLikelihood.size(); i++){
      index.put(sortedLikelihood.get(i), i);
    }

    int total = 0;

    for(Double val: ranking.getLikelihoodMap().values()){
      total += 1<<index.get(val);
    }

    double single = 1d/total;

    for(Map.Entry<Fault, Double> entry: ranking.getLikelihoodMap().entrySet()){
      Fault current = entry.getKey();
      current.addReason(FaultReason.justify(
              "The set has a size of " + current.size() + ".",
              Math.pow(2, index.get(entry.getValue()))*single));
    }

    return ranking.getRankedList();
  }
}
