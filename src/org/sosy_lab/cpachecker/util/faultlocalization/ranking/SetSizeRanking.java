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
import java.util.Collections;
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

    if(result.isEmpty()){
      return Collections.emptyList();
    }
    RankingResults ranking = FaultRankingUtils.rankedListFor(result,
        e -> e.stream()
            .mapToDouble(fc -> fc.correspondingEdge().getFileLocation().getStartingLineInOrigin())
            .max()
            .orElse(0.0));

    BigDecimal sum = BigDecimal.valueOf(2).pow(ranking.getRankedList().size()+1);
    for(int i = 0; i < ranking.getRankedList().size(); i++){
      Fault current = ranking.getRankedList().get(i);
      current.addReason(FaultReason.justify(
          "The set has a size of " + current.size()+".",
          BigDecimal.valueOf(2).pow(i).divide(sum, 5, RoundingMode.HALF_UP).doubleValue()));
    }

    return ranking.getRankedList();
  }
}
