// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.faultlocalization.ranking;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRanking;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRankingUtils;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRankingUtils.RankingResults;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;

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

    int max = result.stream().mapToInt(f -> f.size()).max().orElseThrow();
    RankingResults results = FaultRankingUtils.rankedListFor(result, f -> (double)max + 1 - f.size());
    double sum = results.getLikelihoodMap().values().stream().mapToDouble(Double::valueOf).sum();

    for (Fault fault : result) {
      fault.addInfo(FaultInfo.rankInfo("The set has a size of " + fault.size() + ".", results.getLikelihoodMap().get(fault)/sum));
    }

    return results.getRankedList();
  }
}
