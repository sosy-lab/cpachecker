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
package org.sosy_lab.cpachecker.util.faultlocalization;

import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.misc.MultiMap;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAdditionalInfo;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

public class FaultLocalizationInfo extends CounterexampleInfo {

  private List<Fault> rankedList;
  private FaultReportWriter htmlWriter;

  private Map<FaultContribution, Integer> mapFaultContribToRank;
  private Map<Fault, Integer> mapFaultToRank;

  private Map<CFAEdge, FaultContribution> mapEdgeToFaultContribution;
  private MultiMap<CFAEdge, Fault> mapEdgeToFault;
  private Map<CFAEdge, Integer> mapEdgeToBestRank;
  private Map<CFAEdge, String> mapEdgeToBestDescription;

  /**
   * already processed edges.
   * prevents adding the same information twice.
   */
  private Set<CFAEdge> bannedEdges;

  /**
   * Fault localization algorithms will result in a set of sets of CFAEdges that are most likely to fix a bug.
   * Transforming it into a Set of Faults enables the possibility to attach reasons of why this edge is in this set.
   * After ranking the set of faults an instance of this class can be created.
   *
   * The class should be used to display information to the user.
   *
   * Note that there is no need to create multiple instances of this object if more than one
   * heuristic should be applied. FaultRankingUtils provides a method that creates a new
   * heuristic out of multiple heuristics.
   *
   * To see the result of FaultLocalizationInfo replace the CounterexampleInfo of the target state by this.
   *
   * @param pFaults set of indicators obtained by a fault localization algorithm
   * @param pCreated the counterexample info of the target state
   */
  public FaultLocalizationInfo(
      List<Fault> pFaults,
      CounterexampleInfo pCreated) {
    super(
        pCreated.isSpurious(),
        pCreated.getTargetPath(),
        pCreated.getCFAPathWithAssignments(),
        pCreated.isPreciseCounterExample(),
        CFAPathWithAdditionalInfo.empty());
    rankedList = pFaults;
    htmlWriter = new FaultReportWriter();

    bannedEdges = new HashSet<>();

    mapFaultToRank = new HashMap<>();
    mapFaultContribToRank = new HashMap<>();

    mapEdgeToFaultContribution = new HashMap<>();
    mapEdgeToFault = new MultiMap<>();
    mapEdgeToBestRank = new HashMap<>();
    mapEdgeToBestDescription = new HashMap<>();

    //Create the mapping of a fault to a rank
    Map<FaultContribution, Double> scoreMap = new HashMap<>();
    for (int i = 0; i < pFaults.size(); i++) {
      Fault current = pFaults.get(i);
      if(current.isEmpty()){
        continue;
      }
      mapFaultToRank.put(current, i+1);
      for (FaultContribution faultContribution : current) {
        if(faultContribution.hasReasons()){
          scoreMap.merge(faultContribution, faultContribution.getScore(), Double::max);
        }
      }
    }

    //Rank FaultContrib and put them into the map
    List<FaultContribution> allFaultContributions = scoreMap
        .keySet()
        .stream()
        .sorted(Comparator.comparingDouble(d -> scoreMap.get(d)).reversed())
        .collect(Collectors.toList());

    //assign ranks to FaultContribution and map it to the corresponding edge
    for (int i = 0; i < allFaultContributions.size(); i++) {
      FaultContribution current = allFaultContributions.get(i);
      mapFaultContribToRank.put(current, i+1);
      mapEdgeToFaultContribution.put(current.correspondingEdge(), current);
    }

    // find the best rank and the related description for all edges
    for (Fault set : mapFaultToRank.keySet()) {
      boolean alreadyAssigned = false;
      if(set.isEmpty()) continue;
      for(FaultContribution elem: set){
        int newRank = mapEdgeToBestRank.merge(elem.correspondingEdge(), mapFaultToRank.get(set), Integer::min);
        mapEdgeToBestDescription.merge(elem.correspondingEdge(), htmlWriter.toHtml(set), (a, b) -> {
          if(mapFaultToRank.get(set).intValue() == newRank){
            return a;
          } else {
            return b;
          }
        });
        if(!alreadyAssigned){
          mapEdgeToFault.map(elem.correspondingEdge(), set);
          alreadyAssigned = true;
        }
      }
    }
  }

  public FaultLocalizationInfo(Set<Fault> pResult, FaultRanking pRanking, CounterexampleInfo pCreated){
    this(pRanking.rank(pResult), pCreated);
  }

  public int getRankOfOutput(FaultContribution key) {
    return mapFaultContribToRank.get(key);
  }

  public int getRankOfSet(Fault set) {
    return mapFaultToRank.get(set);
  }

  @Override
  public String toString() {
    StringBuilder toString = new StringBuilder();
    if(!mapFaultToRank.isEmpty()){
      if(!rankedList.isEmpty()){
        toString.append("Ranking sets:\n").append(rankedList.stream().map(l -> l.toString()).collect(Collectors.joining("\n\n")));
      }
    }
    return toString.toString();
  }

  /**
   * Transform a set of sets of CFAEdges to a set of Faults.
   *
   * @param pErrorIndicators possible candidates for the error
   * @return FaultLocalizationOutputs of the CFAEdges.
   */
  public static Set<Fault> transform(
      Set<Set<CFAEdge>> pErrorIndicators) {
    Set<Fault> transformed = new HashSet<>();
    for (Set<CFAEdge> errorIndicator : pErrorIndicators) {
      transformed.add(new Fault(
          errorIndicator.stream().map(FaultContribution::new).collect(Collectors.toSet())));
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

    if(mapEdgeToBestRank.get(edge)!= null){
      elem.put("setminrank", mapEdgeToBestRank.get(edge));
      elem.put("setminrankreason", mapEdgeToBestDescription.get(edge));
    } else {
      elem.put("setminrank", "-");
      elem.put("setminrankreason", "-");
    }

    if (mapEdgeToFaultContribution.get(edge) != null) {
      FaultContribution infoEdge = mapEdgeToFaultContribution.get(edge);
      elem.put("enabled", true);
      if (mapEdgeToFaultContribution.get(edge).hasReasons()) {
        elem.put("fault", htmlWriter.toHtml(infoEdge));
        elem.put("score", (int) (100*infoEdge.getScore()));
        elem.put("rank", mapFaultContribToRank.get(infoEdge));
      }
    }
    if(mapEdgeToFault.get(edge) != null && !bannedEdges.contains(edge)){
      bannedEdges.add(edge);
      elem.put("setindicator", true);
      List<Fault> infoSet = mapEdgeToFault.get(edge);

      List<List<Integer>> concatLines = new ArrayList<>();
      List<String> reasons = new ArrayList<>();
      List<List<String>> descriptions = new ArrayList<>();
      List<Integer> scores = new ArrayList<>();
      List<Integer> ranks = new ArrayList<>();
      for (Fault info : infoSet) {
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
        reasons.add(htmlWriter.toHtml(info));
        scores.add((int)(info.getScore()*100));
        ranks.add(mapFaultToRank.get(info));
      }
      elem.put("setnumber", reasons.size());
      elem.put("setreason", reasons);
      elem.put("setlines", concatLines); //array
      elem.put("setscores", scores);
      elem.put("setdescriptions", descriptions); //array
      elem.put("setrank", ranks);
    }
  }

  public void replaceHtmlWriter(FaultReportWriter pFaultToHtml){
    htmlWriter = pFaultToHtml;
  }

  /**
   * to show the result in the report.html pass the target state
   * @param lastState target state of the error trace
   */
  public void applyTo(ARGState lastState){
    assert lastState.isTarget();
    lastState.replaceCounterexampleInformation(this);
  }
}
