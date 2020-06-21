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

import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRanking;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRankingUtils;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;

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

    if(result.isEmpty()){
      return ImmutableList.of();
    }

    Map<Fault, Double> faultValue = new HashMap<>();
    for(Fault f1: result) {
      double value = 0;
      for(Fault f2: result) {
        Set<FaultContribution> intersection = new HashSet<>(f1);
        intersection.removeAll(f2);
        value += f1.size() - intersection.size();
      }
      faultValue.put(f1, value);
    }

    double sum = faultValue.values().stream().mapToDouble(Double::valueOf).sum();
    if(sum == 0) {
      for(Fault f: result) {
        f.addInfo(FaultInfo.rankInfo("Overall occurrence ranking", 1d/result.size()));
      }
    } else {
      for(Fault f: result) {
        f.addInfo(FaultInfo.rankInfo("Overall occurrence ranking", faultValue.get(f)/sum));
      }
    }

    return FaultRankingUtils.rankedListFor(result, f -> faultValue.get(f)).getRankedList();
  }
}
