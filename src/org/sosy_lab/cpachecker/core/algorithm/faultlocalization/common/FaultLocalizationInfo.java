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

import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.misc.MultiMap;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.heuristics.SetIdentityHeuristic;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAdditionalInfo;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;

public class FaultLocalizationInfo<I extends FaultLocalizationOutput> extends CounterexampleInfo {

  private Map<I, Integer> mapOutToRank;
  private Map<ErrorIndicator<I>, Integer> mapRankToSet;

  private Map<CFAEdge, I> mapEdgeToInfo;
  private MultiMap<CFAEdge, ErrorIndicator<I>> mapEdgeToSetInfo;
  private Map<CFAEdge, Integer> mapEdgeToMinRank;
  private Map<CFAEdge, String> mapEdgeToDescription;

  private Set<CFAEdge> bannedEdges;

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
      CounterexampleInfo pCreated,
      Optional<FaultLocalizationHeuristic<I>> pRanking,
      Optional<FaultLocalizationSetHeuristic<I>> pSetRanking) {
    super(
        pCreated.isSpurious(),
        pCreated.getTargetPath(),
        pCreated.getCFAPathWithAssignments(),
        pCreated.isPreciseCounterExample(),
        CFAPathWithAdditionalInfo.empty());
    bannedEdges = new HashSet<>();
    if(pRanking.isPresent()){
      mapOutToRank = pRanking.get().rank(pErrorIndicators);
    } else {
      mapOutToRank = Collections.emptyMap();
    }

    if(pSetRanking.isPresent()){
      mapRankToSet = pSetRanking.get().rankSubsets(pErrorIndicators);
    } else {
      if(pRanking.isEmpty()){
        mapRankToSet = new SetIdentityHeuristic<I>().rankSubsets(pErrorIndicators);
      } else {
        mapRankToSet = Collections.emptyMap();
      }
    }

    mapEdgeToInfo = new HashMap<>();
    mapEdgeToSetInfo = new MultiMap<>();
    mapEdgeToMinRank = new HashMap<>();
    mapEdgeToDescription = new HashMap<>();

    for (I out : mapOutToRank.keySet()) {
      mapEdgeToInfo.put(out.correspondingEdge(), out);
    }

    for (ErrorIndicator<I> set : mapRankToSet.keySet()) {
      boolean alreadyAssigned = false;
      if(set.isEmpty()) continue;
      for(I elem: set){
        mapEdgeToMinRank.merge(elem.correspondingEdge(), mapRankToSet.get(set), Integer::min);
        mapEdgeToDescription.merge(elem.correspondingEdge(), set.toHtml(), (a, b) -> {
          if(mapRankToSet.get(set).intValue() == mapEdgeToMinRank.get(elem.correspondingEdge()).intValue()){
            return a;
          } else {
            return b;
          }
        });
        if(!alreadyAssigned){
          mapEdgeToSetInfo.map(elem.correspondingEdge(), set);
          alreadyAssigned = true;
        }
      }
    }
  }

  public Map<I, Integer> getMapOutToRank() {
    return mapOutToRank;
  }

  public Map<ErrorIndicator<I>, Integer> getMapRankToSet() {
    return mapRankToSet;
  }

  @Override
  public String toString() {
    List<I> ranked = new ArrayList<>(mapOutToRank.keySet());
    List<ErrorIndicator<I>> rankedSet = new ArrayList<>(mapRankToSet.keySet());
    ranked.sort(Comparator.comparingInt(l -> mapOutToRank.get(l)));
    String edgeRanking = "Ranking of single edges:\n" +
        ranked.stream()
        .map(FaultLocalizationOutput::textRepresentation)
        .collect(Collectors.joining("\n"));
    String setRanking = "Ranking of sets:\n" +
        rankedSet.stream().map(ErrorIndicator::toString).collect(Collectors.joining("\n"));
    if(mapOutToRank.isEmpty() ^ mapRankToSet.isEmpty()){
      return mapOutToRank.isEmpty() ? setRanking : edgeRanking;
    }
    if(mapOutToRank.isEmpty()&& mapRankToSet.isEmpty()){
      return "No heuristic provided.";
    }
    return edgeRanking + "\n\n" + setRanking;
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
      transformed.add(new ErrorIndicator<>(
          errorIndicator.stream().map(FaultLocalizationOutput::of).collect(Collectors.toSet())));
    }
    return transformed;
  }

  public static <I extends FaultLocalizationOutput>
      FaultLocalizationInfo<I> withPredefinedHeuristics(
          ErrorIndicatorSet<I> pResult, CounterexampleInfo pInfo, FaultLocalizationHeuristicUtils.RankingMode pRankingMode) {
    Optional<FaultLocalizationHeuristic<I>> predefinedHeuristic = Optional.of(m -> FaultLocalizationHeuristicUtils
        .rank(m, pRankingMode));
    return new FaultLocalizationInfo<>(
        pResult, pInfo, predefinedHeuristic, Optional.empty());
  }

  @Override
  protected void addAdditionalInfo(Map<String, Object> elem, CFAEdge edge) {
    elem.put("fault", "");
    elem.put("score", 0);
    elem.put("rank", "-");
    elem.put("enabled", false);
    elem.put("set-indicator", false);
    elem.put("setminrank", "-");

    if(mapEdgeToMinRank.get(edge)!= null){
      elem.put("setminrank", mapEdgeToMinRank.get(edge));
      elem.put("setminrankreason", mapEdgeToDescription.get(edge));
    } else {
      elem.put("setminrank", "-");
      elem.put("setminrankreason", "-");
    }

    if (mapEdgeToInfo.get(edge) != null) {
      I infoEdge = mapEdgeToInfo.get(edge);
      elem.put("enabled", true);
      if (mapEdgeToInfo.get(edge).hasReasons()) {
        elem.put("fault", infoEdge.htmlRepresentation());
        elem.put("score", (int) infoEdge.getScore());
        // TODO map
        elem.put("rank", mapOutToRank.get(infoEdge));
      }
    }
    if(mapEdgeToSetInfo.get(edge) != null && !bannedEdges.contains(edge)){
      bannedEdges.add(edge);
      elem.put("setindicator", true);
      List<ErrorIndicator<I>> infoSet = mapEdgeToSetInfo.get(edge);

      List<List<Integer>> concatLines = new ArrayList<>();
      List<String> reasons = new ArrayList<>();
      List<List<String>> descriptions = new ArrayList<>();
      List<Integer> scores = new ArrayList<>();
      List<Integer> ranks = new ArrayList<>();
      for (ErrorIndicator<I> info : infoSet) {
        descriptions.add(info
            .stream()
            .sorted(Comparator.comparingInt(a -> a.correspondingEdge().getFileLocation().getStartingLineInOrigin()))
            .map(l -> {
              CFAEdge cfaEdge = l.correspondingEdge();
              if(cfaEdge.getEdgeType().equals(CFAEdgeType.FunctionReturnEdge)){
                return Splitter.on(":").split(cfaEdge.getDescription()).iterator().next();
              }
              return l.correspondingEdge().getDescription();
            })
            .collect(Collectors.toList()));
        concatLines.add(info.sortedLineNumbers());
        reasons.add(info.toHtml());
        scores.add((int)info.calculateScore());
        ranks.add(mapRankToSet.get(info));
      }
      elem.put("setnumber", reasons.size());
      elem.put("setreason", reasons);
      elem.put("setlines", concatLines); //array
      elem.put("setscores", scores);
      elem.put("setdescriptions", descriptions); //array
      elem.put("setrank", ranks);
    }
  }
}
