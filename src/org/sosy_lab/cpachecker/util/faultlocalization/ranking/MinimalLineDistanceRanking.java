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

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRanking;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRankingUtils;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRankingUtils.RankingResults;

public class MinimalLineDistanceRanking implements FaultRanking {

  private int errorLocation;

  /**
   * Sorts the result set by absolute distance to the error location based on the linenumber
   *
   * @param pErrorLocation the error location
   */
  public MinimalLineDistanceRanking(CFAEdge pErrorLocation) {
    errorLocation = pErrorLocation.getFileLocation().getStartingLineInOrigin();
  }

  @Override
  public List<Fault> rank(Set<Fault> result) {
    if(result.isEmpty()){
      return ImmutableList.of();
    }
    RankingResults ranking = FaultRankingUtils.rankedListFor(result,
        e -> e.stream()
            .mapToDouble(fc -> fc.correspondingEdge().getFileLocation().getStartingLineInOrigin())
            .max()
            .orElse(0.0));

    if(ranking.getRankedList().size()==1){
      Fault current = ranking.getRankedList().get(0);
      current.addInfo(FaultInfo.rankInfo(
          "Minimal distance to error location: " + (errorLocation-(int)ranking.getLikelihoodMap().get(current).doubleValue()) + " line(s)",
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
      current.addInfo(FaultInfo.rankInfo(
              "Minimal distance to error location: " + (errorLocation-(int)ranking.getLikelihoodMap().get(current).doubleValue()) + " line(s)",
              Math.pow(2, index.get(entry.getValue()))*single));
    }

    return ranking.getRankedList();

  }
}
