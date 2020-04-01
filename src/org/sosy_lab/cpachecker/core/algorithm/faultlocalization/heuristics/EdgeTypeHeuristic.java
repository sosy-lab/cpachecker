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
package org.sosy_lab.cpachecker.core.algorithm.faultlocalization.heuristics;

import com.google.common.base.Splitter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.ErrorIndicator;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.ErrorIndicatorSet;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationHeuristic;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationHeuristicUtils;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationOutput;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationReason;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationReason.ReasonType;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationSetHeuristic;

public class EdgeTypeHeuristic<I extends FaultLocalizationOutput> implements
                                                                  FaultLocalizationHeuristic<I>,
                                                                  FaultLocalizationSetHeuristic<I> {
  @Override
  public Map<I, Integer> rank(ErrorIndicatorSet<I> result) {
    Set<I> condense = FaultLocalizationHeuristicUtils.condenseErrorIndicatorSet(result);
    Map<I, Double> scoreMap = new HashMap<>();
    for(I elem: condense){
      switch(elem.correspondingEdge().getEdgeType()){
        case AssumeEdge: scoreMap.put(elem, 100.0); break;
        case StatementEdge: scoreMap.put(elem, 50.0);
        case ReturnStatementEdge: scoreMap.put(elem, 25.0); break;
        case FunctionReturnEdge:
        case CallToReturnEdge:
        case FunctionCallEdge: scoreMap.put(elem, 12.5); break;
        case DeclarationEdge:
        case BlankEdge:
        default: scoreMap.put(elem, 0d); break;
      }
    }
    double sum = scoreMap.values().stream().mapToDouble(l -> l).sum();
    for(Entry<I, Double> entry: scoreMap.entrySet()){
      entry.getKey().addReason(new FaultLocalizationReason(ReasonType.HEURISTIC, "Based on the edge type: \"" + readableName(entry.getKey().correspondingEdge().getEdgeType()) + "\"", entry.getValue()/sum));
    }
    return FaultLocalizationHeuristicUtils.scoreToRankMap(scoreMap);
  }

  @Override
  public Map<ErrorIndicator<I>, Integer> rankSubsets(
      ErrorIndicatorSet<I> errorIndicators) {
    return null;
  }

  private String readableName(CFAEdgeType type){
    StringJoiner joiner = new StringJoiner(" ");
    for (String s : Splitter.onPattern("(?=\\p{Upper})").split(type.name())) {
      joiner.add(s);
    }
    return joiner.toString();
  }
}
