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

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRanking;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRankingUtils;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultReason;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRankingUtils.RankingResults;

public class SetSizeRanking implements FaultRanking {

  /**
   *
   * @param result The result of any FaultLocalizationAlgorithm
   * @return Ranked list of faults
   */
  @Override
  public List<Fault> rank(
      Set<Fault> result) {

    double max = result.stream().mapToDouble(c -> c.size()).max().orElse(0);
    RankingResults rankingResults = FaultRankingUtils
        .rankedListFor(result, l -> max - (double)l.size());
    double sum = rankingResults.getLikelihoodMap().values().stream().mapToDouble(Double::doubleValue).sum();

    for (Entry<Fault, Double> entry : rankingResults.getLikelihoodMap().entrySet()) {
      entry.getKey().addReason(
          FaultReason.justify("The set has a size of " + entry.getKey().size(), entry.getValue()/sum));
    }

    return rankingResults.getRankedList();
  }
}
