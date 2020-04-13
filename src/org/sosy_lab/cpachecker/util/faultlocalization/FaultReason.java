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

import org.sosy_lab.cpachecker.util.faultlocalization.ranking.NoContextExplanation;

/**
 * A FaultReason is a hint, fix or justification of why a certain Fault or FaultContribution is
 * directly connected to an error. The description of a Reason will be shown to the user in the final
 * report.
 */
public class FaultReason {

  public enum ReasonType{
    /** A hint is displayed separately and ignored when calculating score. */
    HINT,
    /** Not implemented yet. Same behaviour as DEFAULT */
    FIX,
    /** Reasons of this type are scored and ranked. */
    DEFAULT
  }

  private String description;
  private double likelihood;
  private ReasonType reasonType;

  /**
   * Reasons can be attached to Faults and FaultContributions.
   * They try to explain why this error occurred or why a certain edge is in the ranked list of potential fault indicators.
   * @param pReasonType Type of the reason
   * @param pDescription A short description which will be shown to the user.
   * @param pLikelihood Score for the reason. A high score means that the reason is more important.
   */
  public FaultReason(ReasonType pReasonType, String pDescription, double pLikelihood) {
    description = pDescription;
    likelihood = pLikelihood;
    reasonType = pReasonType;
  }

  public ReasonType getReasonType(){
    return reasonType;
  }

  public void setReasonType(ReasonType pReasonType) {
    reasonType = pReasonType;
  }

  public boolean isHint(){
    return ReasonType.HINT == reasonType;
  }

  public String getDescription() {
    return description;
  }

  public double getLikelihood() {
    return likelihood;
  }

  public void setLikelihood(double pLikelihood) {
    likelihood = pLikelihood;
  }

  public void setDescription(String pDescription) {
    description = pDescription;
  }

  @Override
  public String toString() {
    String percent = ((int) (likelihood * 10000)) / 100d + "%";
    if(reasonType == ReasonType.HINT){
      return description;
    }
    return description + " (" + percent + ")";
  }

  public static FaultReason explain(ReasonType pType, FaultExplanation pExplanation, Fault indicator, double pLikelihood){
    return new FaultReason(pType, pExplanation.explanationFor(indicator), pLikelihood);
  }

  public static FaultReason hint(String pDescription){
    return new FaultReason(ReasonType.HINT, pDescription, 0);
  }

  /**
   * Returns a possible fix for pSet. It is classified as a hint because the suggested fix is a guess and does not guarantee to be a fix.
   * The set has to have size 1 because NoContextExplanation is designed to explain singletons only.
   * @param pSet the singleton set to calculate the explanation for
   * @return Explanation for pSet
   */
  public static FaultReason hintFor(Fault pSet){
    return new FaultReason(ReasonType.HINT, new NoContextExplanation().explanationFor(pSet), 0);
  }

  public static FaultReason fix(String pDescription, double pLikelihood){
    return new FaultReason(ReasonType.FIX, pDescription, pLikelihood);
  }

  public static FaultReason justify(String pDescription, double pLikelihood){
    return new FaultReason(ReasonType.DEFAULT, pDescription, pLikelihood);
  }

  @Override
  public boolean equals(Object q){
    if(q instanceof FaultReason){
      return ((FaultReason)q).description.equals(description);
    }
    return false;
  }

  @Override
  public int hashCode(){
    return 17 * description.hashCode();
  }
}
