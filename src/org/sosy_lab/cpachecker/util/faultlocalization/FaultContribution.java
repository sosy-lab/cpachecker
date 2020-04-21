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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * Every FaultContribution represents an edge in the program that contains a fault or is
 * responsible for a fault in combination with the other edges in the set.
 * Reasons can be added to a FaultContribution object. They should explain why this object is
 * responsible for the error.
 * The assigned score is used to rank the FaultContributions.
 */
public class FaultContribution {

  protected List<FaultReason> reasons;
  private CFAEdge correspondingEdge;

  /**
   * The calculation of the score of FaultContribution is not implemented.
   * The score is used to rank FaultContributions.
   * The recommended way is to calculate the score based on the likelihood of the appended reasons instead of setting it to an value manually.
   * The score will be printed to the user as an indicator of how likely this edge is to fix the error when changed.
   * However, there exists an example method for calculating the score. For more details see the documentation of setScore() below.
   */
  private double score;

  public FaultContribution(CFAEdge pCorrespondingEdge){
    reasons = new ArrayList<>();
    correspondingEdge = pCorrespondingEdge;
    score = 0;
  }

  public double getScore() {
    return score;
  }

  public void setScore(double pScore) {
    score = pScore;
  }

  public void addReason(
      FaultReason pFaultReason) {
    reasons.add(pFaultReason);
  }

  public List<FaultReason> getReasons() {
    return reasons;
  }

  public String textRepresentation() {
    //Sort by Hints and then by likelihood
    Comparator<FaultReason> sortReasons =
        Comparator.comparingInt(l -> l.isHint() ? 0 : 1);
    sortReasons = sortReasons.thenComparingDouble(b -> 1/b.getLikelihood());
    reasons.sort(sortReasons);
    StringBuilder stringRepresentation =
        new StringBuilder(
            "Error suspected on line "
                + correspondingEdge().getFileLocation().getStartingLineInOrigin()
                + ". (Score: " + (int)(getScore()*100) + ")\n");
    int numberReasons = reasons.stream().filter(l -> !l.isHint()).mapToInt(l -> 1).sum();
    stringRepresentation.append("Detected ")
        .append(numberReasons)
        .append(" possible reason")
        .append(numberReasons == 1 ? ":\n" : "s:\n");

    //style:\"list-style-type:none;\">
    int lastHint = 0;
    for (int i = 0; i < reasons.size(); i++) {
      FaultReason current = reasons.get(i);
      if (current.isHint()) {
        stringRepresentation.append(" Hint: ").append(current.toString()).append("\n");
        lastHint = i+1;
      } else {
        stringRepresentation.append("    ").append(i+1-lastHint).append(") ").append(current.toString()).append("\n");
      }
    }

    return stringRepresentation.toString();
  }

  @Override
  public String toString(){
    return textRepresentation();
  }

  public boolean hasReasons() {
    return !reasons.isEmpty();
  }

  public CFAEdge correspondingEdge(){
    return correspondingEdge;
  }

  @Override
  public boolean equals(Object q) {
    if (q instanceof FaultContribution){
      FaultContribution casted = (FaultContribution)q;
      if (correspondingEdge.equals(casted.correspondingEdge())){
        if(casted.getReasons().size() == getReasons().size()){
          for(int i = 0; i < getReasons().size(); i++){
            if(!getReasons().get(i).equals(casted.getReasons().get(i))){
              return false;
            }
          }
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    int result = 5;
    for(FaultReason reason: reasons){
      result = Objects.hash(reason, result);
    }
    result = Objects.hash(correspondingEdge, result);
    return result;
  }
}
