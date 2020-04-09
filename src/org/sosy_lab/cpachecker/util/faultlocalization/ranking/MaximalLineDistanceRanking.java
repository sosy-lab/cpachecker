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
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRanking;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRankingUtils;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultReason;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRankingUtils.RankingResults;

public class MaximalLineDistanceRanking implements FaultRanking {

  private int errorLocation;

  /**
   * Sorts the result set by absolute distance to the error location based on the linenumber
   *
   * @param pErrorLocation the error location
   */
  public MaximalLineDistanceRanking(CFAEdge pErrorLocation) {
    errorLocation = pErrorLocation.getFileLocation().getStartingLineInOrigin();
  }

  @Override
  public List<Fault> rank(Set<Fault> result) {
    if(result.isEmpty()){
      return Collections.emptyList();
    }
   RankingResults ranking = FaultRankingUtils.rankedListFor(result,
        e -> e.stream()
        .mapToDouble(fc -> Math.abs(errorLocation - fc.correspondingEdge().getFileLocation().getStartingLineInOrigin()))
        .max()
        .orElse(0.0));

    if(ranking.getRankedList().size()==1){
      Fault current = ranking.getRankedList().get(0);
      current.addReason(FaultReason.justify(
          "Maximal distance to error location: " + (int)ranking.getLikelihoodMap().get(current).doubleValue() + " line(s)",
          1d));
      return ranking.getRankedList();
    }
    BigDecimal sum = BigDecimal.valueOf(2).pow(ranking.getRankedList().size()).subtract(BigDecimal.ONE);
    for(int i = 0; i < ranking.getRankedList().size(); i++){
      Fault current = ranking.getRankedList().get(i);
      current.addReason(FaultReason.justify(
          "Maximal distance to error location: " + (int)ranking.getLikelihoodMap().get(current).doubleValue() + " line(s)",
          BigDecimal.valueOf(2).pow(i).divide(sum, 5, RoundingMode.HALF_UP).doubleValue()));
    }

    return ranking.getRankedList();

  }
}

