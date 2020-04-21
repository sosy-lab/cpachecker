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

import com.google.common.collect.ForwardingSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A Fault is a set of FaultContributions.
 * The set has to be obtained by a fault localizing algorithm.
 * FaultReasons can be appended to a Fault to explain why this set of FaultContributions caused an error.
 * The score of a Fault is used to rank the Faults. The higher the score the higher the rank.
 */
public class Fault extends ForwardingSet<FaultContribution> {

  private Set<FaultContribution> errorSet;
  private List<FaultReason> reasons;

  /**
   * The recommended way is to calculate the score based on the likelihoods of the appended reasons.
   */
  private double score;

  /**
   * Error Indicators indicate a subset of all edges that most likely contain an error.
   * @param pErrorSet set to forward
   */
  public Fault(Set<FaultContribution> pErrorSet){
    errorSet = pErrorSet;
    reasons = new ArrayList<>();
    score = 0;
  }

  public Fault(){
    errorSet = new HashSet<>();
    reasons = new ArrayList<>();
    score = 0;
  }

  /**
   * Creates a mutable set with only one member
   * @param singleton a FaultContribution that is transformed into a singleton set
   */
  public Fault(FaultContribution singleton){
    errorSet = new HashSet<>(Collections.singleton(singleton));
    reasons = new ArrayList<>();
    score = 0;
  }

  public List<Integer> sortedLineNumbers(){
    List<Integer> lines = new ArrayList<>();
    for(FaultContribution elem: errorSet){
      lines.add(elem.correspondingEdge().getFileLocation().getStartingLineInOrigin());
    }
    Collections.sort(lines);
    return lines;
  }

  /**
   * Add a reason to explain why this set indicates an error.
   * Appending reasons in heuristics is the recommended way.
   * @param reason Fix, Hint or Reason why this might be an error or why a heuristic did add a FaultReason to this set.
   */
  public void addReason(FaultReason reason){
    reasons.add(reason);
  }

  public List<FaultReason> getReasons() {
    return reasons;
  }

  public double getScore() {
    return score;
  }

  /**
   * Set the score for this Fault.
   * There exists a default implementation of calculating the score for this class.
   * @see  FaultRankingUtils#assignScoreTo(Fault)
   * @param pScore the score
   */
  public void setScore(double pScore) {
    score = pScore;
  }

  @Override
  public String toString(){
    List<FaultReason> copy = new ArrayList<>(reasons);
    sortReasonsByReasonTypeThenByLikelihood(copy);
    int numberReasons = copy.stream().filter(l -> !l.isHint()).mapToInt(l -> 1).sum();

    String header = "Error suspected on line(s): " + listDistinctLinesAndJoin();

    String amountReasons = "Detected " + numberReasons + " possible reason(s):\n";
    StringBuilder reasonString = new StringBuilder();
    int lastHint = 0;
    for (int i = 0; i < copy.size(); i++) {
      FaultReason current = copy.get(i);
      if (current.isHint()) {
        reasonString.append(" Hint: ").append(current.toString()).append("\n");
        lastHint = i+1;
      } else {
        reasonString.append("    ").append(i+1-lastHint).append(") ").append(current.toString()).append("\n");
      }
    }
    return header + "\n" + amountReasons + reasonString;
  }

  private void sortReasonsByReasonTypeThenByLikelihood(List<FaultReason> pReasons){
    Comparator<FaultReason> sortReasons =
        Comparator.comparingInt(l -> l.isHint() ? 0 : 1);
    sortReasons = sortReasons.thenComparingDouble(b -> 1d/b.getLikelihood());
    pReasons.sort(sortReasons);
  }

  private String listDistinctLinesAndJoin(){
    return errorSet
        .stream()
        .mapToInt(l -> l.correspondingEdge().getFileLocation().getStartingLineInOrigin())
        .sorted()
        .distinct()
        .mapToObj(l -> (Integer)l + "")
        .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
          int lastIndex = list.size() - 1;
          if (lastIndex < 1) {
            return String.join("", list);
          }
          if (lastIndex == 1) {
            return String.join(" and ", list);
          }
          return String.join(" and ",
              String.join(", ", list.subList(0, lastIndex)),
              list.get(lastIndex));
        }))
        + " (Score: " + (int)(score*100) + ")";
  }

  @Override
  public boolean equals(Object q){
    if(q instanceof Fault){
      Fault comp = (Fault)q;
      if(comp.size() == size()){
        for (FaultContribution faultContribution : comp) {
          if(!contains(faultContribution)){
            return false;
          }
        }
        return true;
      }
    }
    return false;
  }

  @Override
  public int hashCode(){
    int result = 4;
    for(FaultContribution contribution: this){
      result = Objects.hash(contribution, result);
    }
    return result;
  }

  @Override
  protected Set<FaultContribution> delegate() {
    return errorSet;
  }
}
