// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
   * @param result The result of any FaultLocalizationWithTraceFormula
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
