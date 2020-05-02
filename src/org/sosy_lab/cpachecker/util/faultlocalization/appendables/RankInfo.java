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

/**
 * A RankInfo is a hint, fix or justification of why a certain Fault or FaultContribution is
 * directly connected to an error. The description of a Reason will be shown to the user in the final
 * report.
 */
public class RankInfo extends FaultInfo {

  /**
   * Reasons can be attached to Faults and FaultContributions.
   * They try to explain why this error occurred or why a certain edge is in the ranked list of potential fault indicators.
   * @param pDescription A short description which will be shown to the user.
   * @param pLikelihood Score for the reason. A high score means that the reason is more important.
   */
  protected RankInfo(InfoType pType, String pDescription, double pLikelihood) {
    super(pType);
    description = pDescription;
    score = pLikelihood;
  }

  public void setScore(double pScore) {
    score = pScore;
  }

  public void setDescription(String pDescription) {
    description = pDescription;
  }

}
