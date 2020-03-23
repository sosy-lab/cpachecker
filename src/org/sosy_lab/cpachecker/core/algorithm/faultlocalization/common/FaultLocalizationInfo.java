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
package org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAdditionalInfo;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;

public class FaultLocalizationInfo<I extends FaultLocalizationOutput> extends CounterexampleInfo {

  private ErrorIndicatorSet<I> errorIndicators;
  private FaultLocalizationHeuristic<I> ranking;
  private List<I> rankedList;
  private Map<CFAEdge, I> edgeToInfoMap;

  /**
   * Object to represent a result set obtained from any FaultLocalizationAlgorithm Note: there is no
   * need to create multiple instances of this object if more than one heuristic should be applied.
   * FaultLocalzationImpl provides a method that creates an FaultLocalizationHeuristic out of
   * multiple heuristics.
   *
   * @param pErrorIndicators The set of possible candidates.
   * @param pRanking a ranking algorithm. Examples can be found in FaultLocalizationInfoImpl
   */
  public FaultLocalizationInfo(
      ErrorIndicatorSet<I> pErrorIndicators,
      FaultLocalizationHeuristic<I> pRanking,
      CounterexampleInfo pCreated) {
    super(
        pCreated.isSpurious(),
        pCreated.getTargetPath(),
        pCreated.getCFAPathWithAssignments(),
        pCreated.isPreciseCounterExample(),
        CFAPathWithAdditionalInfo.empty());
    ranking = pRanking;
    errorIndicators = pErrorIndicators;
    edgeToInfoMap = new HashMap<>();
    rankedList = rank();

    for (I out : rankedList) {
      edgeToInfoMap.put(out.correspondingEdge(), out);
    }
  }

  public List<I> getRankedList() {
    return rankedList;
  }

  private List<I> rank() {
    return ranking.rank(errorIndicators);
  }

  public ErrorIndicatorSet<I> getErrorIndicators() {
    return errorIndicators;
  }

  @Override
  public String toString() {
    return rankedList.stream()
        .map(FaultLocalizationOutput::textRepresentation)
        .collect(Collectors.joining("\n"));
  }

  /**
   * Use this if the algorithm of your choice returns CFAEdges and does not use
   * FaultLocalizationOutput at all.
   *
   * @param pErrorIndicators possible candidates for the error
   * @return FaultLocalizationOutputs of the CFAEdges.
   */
  public static ErrorIndicatorSet<FaultLocalizationOutput> transform(
      Set<Set<CFAEdge>> pErrorIndicators) {
    ErrorIndicatorSet<FaultLocalizationOutput> transformed = new ErrorIndicatorSet<>();
    for (Set<CFAEdge> errorIndicator : pErrorIndicators) {
      transformed.add(
          errorIndicator.stream().map(FaultLocalizationOutput::of).collect(Collectors.toSet()));
    }
    return transformed;
  }

  public static <I extends FaultLocalizationOutput>
      FaultLocalizationInfo<I> withPredefinedHeuristics(
          ErrorIndicatorSet<I> pResult, RankingMode pRankingMode, CounterexampleInfo pInfo) {
    return new FaultLocalizationInfo<>(
        pResult, m -> FaultLocalizationHeuristicImpl.rank(m, pRankingMode), pInfo);
  }

  @Override
  protected void addAdditionalInfo(Map<String, Object> elem, CFAEdge edge) {
    elem.put("fault", "");
    elem.put("score", 0);
    elem.put("rank", "-");
    elem.put("enabled", false);
    if (edgeToInfoMap.get(edge) != null) {
      I infoEdge = edgeToInfoMap.get(edge);
      elem.put("enabled", true);
      if (edgeToInfoMap.get(edge).hasReasons()) {
        elem.put("fault", infoEdge.htmlRepresentation());
        elem.put("score", (int) (infoEdge.getScore()));
        // TODO map
        elem.put("rank", rankedList.indexOf(infoEdge) + 1);
      }
    }
  }
}
