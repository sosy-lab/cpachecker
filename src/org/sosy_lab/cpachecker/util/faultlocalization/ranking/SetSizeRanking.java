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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
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
          "The set has a size of " + current.size()+ ".",
          1d));
      return ranking.getRankedList();
    }
    BigDecimal sum = BigDecimal.valueOf(2).pow(ranking.getRankedList().size()).subtract(BigDecimal.ONE);
    for(int i = 0; i < ranking.getRankedList().size(); i++){
      Fault current = ranking.getRankedList().get(i);
      current.addReason(FaultReason.justify(
          "The set has a size of " + current.size() + ".",
          BigDecimal.valueOf(2).pow(i).divide(sum, 5, RoundingMode.HALF_UP).doubleValue()));
    }

    return ranking.getRankedList();
  }
}
