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
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

public class FaultLocalizationInfo<I extends FaultLocalizationOutput> extends CounterexampleInfo {

  private Map<I, Integer> mapOutToRank;
  private Map<ErrorIndicator<I>, Integer> mapSetToRank;

  private Map<CFAEdge, I> mapEdgeToInfo;
  private MultiMap<CFAEdge, ErrorIndicator<I>> mapEdgeToSetInfo;
  private Map<CFAEdge, Integer> mapEdgeToMinRank;
  private Map<CFAEdge, String> mapEdgeToDescription;

  private Set<CFAEdge> bannedEdges;

  /**
   * Object to represent a ErrorIndicatorSet obtained by any FaultLocalizationAlgorithm. *
   * Note that there is no need to create multiple instances of this object if more than one
   * heuristic should be applied. FaultLocalizationUtils provides a method that creates a new
   * heuristic out of multiple heuristics.
   *
   * If no heuristics are given (both are Optional.empty()) the default heuristic will be used (ranking error indicators by the order of the iterator).
   *
   * To see the result of FaultLocalizationInfo replace the CounterexampleInfo of the target state by this.
   *
   * @param pErrorIndicators set of indicators obtained by a fault localization algorithm
   * @param pCreated the counterexample info of the target state
   * @param pRanking optional of a heuristic that ranks FaultLocalizationOutputs
   * @param pSetRanking optional of a heuristic that ranks ErrorIndicators
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
      mapSetToRank = pSetRanking.get().rankSubsets(pErrorIndicators);
    } else {
      if(pRanking.isEmpty()){
        mapSetToRank = new SetIdentityHeuristic<I>().rankSubsets(pErrorIndicators);
      } else {
        mapSetToRank = Collections.emptyMap();
      }
    }

    mapEdgeToInfo = new HashMap<>();
    mapEdgeToSetInfo = new MultiMap<>();
    mapEdgeToMinRank = new HashMap<>();
    mapEdgeToDescription = new HashMap<>();

    for (I out : mapOutToRank.keySet()) {
      mapEdgeToInfo.put(out.correspondingEdge(), out);
    }

    for (ErrorIndicator<I> set : mapSetToRank.keySet()) {
      boolean alreadyAssigned = false;
      if(set.isEmpty()) continue;
      for(I elem: set){
        mapEdgeToMinRank.merge(elem.correspondingEdge(), mapSetToRank.get(set), Integer::min);
        mapEdgeToDescription.merge(elem.correspondingEdge(), set.toHtml(), (a, b) -> {
          if(mapSetToRank.get(set).intValue() == mapEdgeToMinRank.get(elem.correspondingEdge()).intValue()){
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

  public int getRankOfOutput(I key) {
    return mapOutToRank.get(key);
  }

  public int getRankOfSet(ErrorIndicator<I> set) {
    return mapSetToRank.get(set);
  }

  @Override
  public String toString() {
    StringBuilder toString = new StringBuilder();
    if(!mapOutToRank.isEmpty()){
      List<I> ranked = new ArrayList<>(mapOutToRank.keySet());
      ranked.sort(Comparator.comparingInt(l -> mapOutToRank.get(l)));
      if (!ranked.isEmpty()) {
        toString.append("Ranking edges:\n").append(ranked.stream().map(l -> l.textRepresentation()).collect(Collectors.joining("\n\n"))).append(mapSetToRank.isEmpty()?"":"\n\n");
      }
    }
    if(!mapSetToRank.isEmpty()){
      List<ErrorIndicator<I>> ranked = new ArrayList<>(mapSetToRank.keySet());
      ranked.sort(Comparator.comparingInt(l -> mapSetToRank.get(l)));
      if(!ranked.isEmpty()){
        toString.append("Ranking sets:\n").append(ranked.stream().map(l -> l.toString()).collect(Collectors.joining("\n\n")));
      }
    }
    return toString.toString();
  }

  /**
   * Transform a set of sets of CFAEdges to a ErrorIndicatorSet.
   * Use this as a default way. No actual implementation of FaultLocalizationOutput is needed then.
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

  /**
   * append additional information to the CounterexampleInfo output
   * @param elem maps a property of edge to an object
   * @param edge the edge that is currently transformed into JSON format.
   */
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
        elem.put("fault", infoEdge.toHtml());
        elem.put("score", (int) (100*infoEdge.getScore()));
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
        scores.add((int)(info.calculateScore()*100));
        ranks.add(mapSetToRank.get(info));
      }
      elem.put("setnumber", reasons.size());
      elem.put("setreason", reasons);
      elem.put("setlines", concatLines); //array
      elem.put("setscores", scores);
      elem.put("setdescriptions", descriptions); //array
      elem.put("setrank", ranks);
    }
  }

  /**
   * to show the result in the report.html pass the target state
   * @param lastState
   */
  public void applyTo(ARGState lastState){
    assert lastState.isTarget();
    lastState.replaceCounterexampleInformation(this);
  }
}
