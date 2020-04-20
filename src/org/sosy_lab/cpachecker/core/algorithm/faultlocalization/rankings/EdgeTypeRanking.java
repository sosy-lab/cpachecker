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
package org.sosy_lab.cpachecker.core.algorithm.faultlocalization.rankings;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRanking;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRankingUtils;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultReason;

public class EdgeTypeRanking implements FaultRanking {

  @Override
  public List<Fault> rank(Set<Fault> result) {
    FaultRankingUtils.RankingResults results =
        FaultRankingUtils.rankedListFor(result, f -> f.stream().mapToDouble(fc -> getScore(fc.correspondingEdge().getEdgeType())).sum());
    double overallSum = results.getLikelihoodMap().values().stream().mapToDouble(Double::doubleValue).sum();
    for (Entry<Fault, Double> faultDoubleEntry : results.getLikelihoodMap().entrySet()) {
      faultDoubleEntry.getKey().addReason(FaultReason.justify("Score calculated by edge type(s).", overallSum==0?1d:faultDoubleEntry.getValue()/overallSum));
    }
    return results.getRankedList();
  }

  private double getScore(CFAEdgeType edgeType) {
    switch (edgeType) {
      case AssumeEdge:
        return 100d;
      case StatementEdge:
        return 50d;
      case ReturnStatementEdge:
        return 25d;
        // Fall through
      case FunctionReturnEdge:
      case CallToReturnEdge:
      case FunctionCallEdge:
        return 12.5;
        // Fall through
      case DeclarationEdge:
      case BlankEdge:
      default:
        return 0;
    }
  }

/*  private String readableName(CFAEdgeType type){
    StringJoiner joiner = new StringJoiner(" ");
    for (String s : Splitter.onPattern("(?=\\p{Upper})").split(type.name())) {
      joiner.add(s);
    }
    return joiner.toString();
  }*/

}
