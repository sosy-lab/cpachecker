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
package org.sosy_lab.cpachecker.util.faultlocalization.appendables;

import java.util.Objects;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.ranking.NoContextExplanation;

public abstract class FaultInfo implements Comparable<FaultInfo>{

  public enum InfoType{
    /** The reason why a fault localization algorithm created the fault */
    REASON(0),
    /** Provides a possible fix */
    FIX(1),
    /** Hints and explanations for the user */
    HINT(2),
    /** Information provided by the rankings */
    RANK_INFO(3);

    private final int reportRank;
    InfoType(int pReportRank) {
      reportRank = pReportRank;
    }
  }

  protected double score;
  protected String description;
  private InfoType type;

  public FaultInfo(InfoType pType){
    type = pType;
  }

  /**
   * Returns a possible fix for pSet. It may be a guess.
   * The set has to have size 1 because NoContextExplanation is designed to explain singletons only.
   * @param pSet the singleton set to calculate the explanation for
   * @return Explanation for pSet
   */
  public static FaultInfo possibleFixFor(Fault pSet){
    return new PotentialFix(InfoType.FIX, new NoContextExplanation().explanationFor(pSet));
  }

  public static FaultInfo fix(String pDescription){
    return new PotentialFix(InfoType.FIX, pDescription);
  }

  public static FaultInfo rankInfo(String pDescription, double pLikelihood){
    return new RankInfo(InfoType.RANK_INFO, pDescription, pLikelihood);
  }

  public static FaultInfo justify(String pDescription){
    return new FaultReason(InfoType.REASON, pDescription);
  }

  public static FaultInfo hint(String pDescription){
    return new Hint(InfoType.HINT, pDescription);
  }

  public double getScore(){
    return score;
  }

  public String getDescription() {
    return description;
  }

  public InfoType getType() {
    return type;
  }

  /**
   * Sort by InfoType then by score.
   * @param info FaultInfo for comparison
   * @return Is this object smaller equal or greater than info
   */
  @Override
  public int compareTo(FaultInfo info){
    if(type.equals(info.type)){
      return Double.compare(info.score, score);
    } else {
      return type.reportRank - info.type.reportRank;
    }
  }

  @Override
  public int hashCode(){
    return Objects.hash(31, description, score, type);
  }

  @Override
  public boolean equals(Object q){
    if(q instanceof FaultInfo){
      FaultInfo r = (FaultInfo)q;
      if(type.equals(r.type)){
        return r.description.equals(description) && score
            == r.score;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    String percent = ((int) (score * 10000)) / 100d + "%";
    return type + ": " + description + " (" + percent + ")";
  }
}
