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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;

/**
 * Every FaultContribution represents an edge in the program that contains a fault or is
 * responsible for a fault in combination with the other edges in the set.
 * Reasons can be added to a FaultContribution object. They should explain why this object is
 * responsible for the error.
 * The assigned score is used to rank the FaultContributions.
 */
public class FaultContribution {

  protected List<FaultInfo> infos;
  private CFAEdge correspondingEdge;

  /**
   * The calculation of the score of FaultContribution is not implemented.
   * The score is used to rank FaultContributions.
   * The recommended way is to calculate the score based on the likelihood of the appended RankInfos instead of setting it to an value manually.
   * The score will be printed to the user as an indicator of how likely this edge is to fix the error when changed.
   * However, there exists an example method for calculating the score. For more details see FaultRankingUtils.
   * @see FaultRankingUtils#assignScoreTo(FaultContribution)
   */
  private double score;

  public FaultContribution(CFAEdge pCorrespondingEdge){
    infos = new ArrayList<>();
    correspondingEdge = pCorrespondingEdge;
    score = 0;
  }

  public double getScore() {
    return score;
  }

  public void setScore(double pScore) {
    score = pScore;
  }

  public void addInfo(
      FaultInfo pFaultInfo) {
    infos.add(pFaultInfo);
  }

  public List<FaultInfo> getInfos() {
    return infos;
  }

  public String textRepresentation() {
    Collections.sort(infos);
    StringBuilder out =
        new StringBuilder(
            "Error suspected on line "
                + correspondingEdge().getFileLocation().getStartingLineInOrigin()
                + ". (Score: " + (int)(getScore()*100) + ")\n");
    List<FaultInfo> copy = new ArrayList<>(infos);
    for (FaultInfo faultInfo : copy) {
      switch(faultInfo.getType()){
        case RANK_INFO:
          out.append(" ".repeat(2));
          break;
        case REASON:
          out.append(" ".repeat(5));
          break;
        case HINT:
          out.append(" ".repeat(7));
          break;
        case FIX:
          out.append(" ".repeat(8));
          break;
      }
      out.append(faultInfo).append("\n");
    }

    return out.toString();
  }

  @Override
  public String toString(){
    return textRepresentation();
  }

  public boolean hasReasons() {
    return !infos.isEmpty();
  }

  public CFAEdge correspondingEdge(){
    return correspondingEdge;
  }

  @Override
  public boolean equals(Object q) {
    if (q instanceof FaultContribution){
      FaultContribution casted = (FaultContribution)q;
      if (correspondingEdge.equals(casted.correspondingEdge())){
        if(casted.getInfos().size() == getInfos().size()){
          List<FaultInfo> copy = new ArrayList<>(infos);
          List<FaultInfo> copy2 = new ArrayList<>(casted.infos);
          Collections.sort(copy);
          Collections.sort(copy2);

          for(int i = 0; i < copy.size(); i++){
            if(!copy.get(i).equals(copy2.get(i))){
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
    for(FaultInfo reason: infos){
      result = Objects.hash(reason, result);
    }
    result = Objects.hash(correspondingEdge, result);
    return result;
  }
}
