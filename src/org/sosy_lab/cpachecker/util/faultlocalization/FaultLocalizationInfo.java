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

public class FaultLocalizationInfo extends CounterexampleInfo {

  private  CounterexampleInfo created;

  private List<Fault> rankedList;
  private FaultReportWriter htmlWriter;

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
   * @param pFaults Ranked list of faults obtained by a fault localization algorithm
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
    created = pCreated;

    rankedList = pFaults;
    htmlWriter = new FaultReportWriter();

    bannedEdges = new HashSet<>();

    mapFaultToRank = new HashMap<>();

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

    scoreMap.keySet().forEach(fc -> mapEdgeToFaultContribution.put(fc.correspondingEdge(),fc));

    // find the best rank and the related description for all edges
    for (Fault set : mapFaultToRank.keySet()) {
      boolean alreadyAssigned = false;
      if(set.isEmpty()) continue;
      for(FaultContribution elem: set){
        int newRank = mapEdgeToBestRank.merge(elem.correspondingEdge(), mapFaultToRank.get(set), Integer::min);
        mapEdgeToBestDescription.merge(elem.correspondingEdge(), htmlWriter.toHtml(set), (a, b) -> {
          if(mapFaultToRank.get(set) == newRank){
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

  public int getRankOfSet(Fault set) {
    return mapFaultToRank.get(set);
  }

  @Override
  public String toString() {
    StringBuilder toString = new StringBuilder();
    if(!mapFaultToRank.isEmpty()){
      if(!rankedList.isEmpty()){
        toString.append(rankedList.stream().map(l -> l.toString()).collect(Collectors.joining("\n\n")));
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
    // if the edge is contained in more than one set store the best rank here.
    elem.put("bestrank", "-");
    // the reason corresponding to the above rank
    elem.put("bestreason", "");
    // in how many sets is the current edge contained.
    elem.put("numbersets", 0);
    // array of edge descriptions
    elem.put("descriptions", new ArrayList<>());
    // array of all reasons of the sets this edge is contained in
    elem.put("reasons", new ArrayList<>());
    // array of all lines of the sets this edge is contained in
    elem.put("lines", new ArrayList<>());
    //Ranks and scores of all sets this is contained in
    elem.put("scores", new ArrayList<>());
    elem.put("ranks", new ArrayList<>());

    if(bannedEdges.contains(edge)){
      return;
    }

    boolean isFault = mapEdgeToBestRank.containsKey(edge);
    elem.put("isfault",isFault);
    if(isFault){
      elem.put("bestrank", mapEdgeToBestRank.get(edge));
      FaultContribution corresponding = mapEdgeToFaultContribution.get(edge);
      if(corresponding.hasReasons()){
        elem.put("bestreason", mapEdgeToBestDescription.get(edge) + "<br><br><strong>Additional information has been provided:</strong><br>" + htmlWriter.toHtml(corresponding));
      } else {
        elem.put("bestreason", mapEdgeToBestDescription.get(edge));
      }
    }

    if(mapEdgeToFault.containsKey(edge)){
      bannedEdges.add(edge);
      List<Fault> associatedFaults = mapEdgeToFault.get(edge);
      elem.put("numbersets", associatedFaults.size());
      // Calculate list.
      List<List<String>> descriptions = new ArrayList<>();
      List<String> reasons = new ArrayList<>();
      List<List<Integer>> lines = new ArrayList<>();
      List<Integer> scores = new ArrayList<>();
      List<Integer> ranks = new ArrayList<>();
      for(Fault fault: associatedFaults){
        // get description of all edge types in the Fault
        // e.g. Fault = {StatementEdge[i=0], AssumeEdge[i<5]} then description = {[i = 0;], [i < 5;]}
        descriptions.add(fault
            .stream()
            .sorted(Comparator.comparingInt(fc -> fc.correspondingEdge().getFileLocation().getStartingLineInOrigin()))
            .map(fc -> {
              CFAEdge cfaEdge = fc.correspondingEdge();
              if(cfaEdge.getEdgeType().equals(CFAEdgeType.FunctionReturnEdge)){
                return Splitter.on(":").split(cfaEdge.getDescription()).iterator().next();
              }
              return fc.correspondingEdge().getDescription();
            })
            .collect(Collectors.toList()));

        lines.add(fault.sortedLineNumbers());
        reasons.add(htmlWriter.toHtml(fault));
        scores.add((int)(fault.getScore()*100));
        ranks.add(mapFaultToRank.get(fault));
      }
      elem.put("descriptions", descriptions);
      elem.put("reasons", reasons);
      elem.put("lines", lines);
      elem.put("scores", scores);
      elem.put("ranks", ranks);
    }
  }

  public void replaceHtmlWriter(FaultReportWriter pFaultToHtml){
    htmlWriter = pFaultToHtml;
  }

  /**
   * Replace default CounterexampleInfo with this extended version of a CounterexampleInfo.
   */
  public void apply(){
    created.getTargetPath().getLastState().replaceCounterexampleInformation(this);
  }
}
