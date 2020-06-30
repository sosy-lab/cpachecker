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
