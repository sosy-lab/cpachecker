// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.faultlocalization.appendables;

/**
 * A RankInfo is a hint, fix or justification of why a certain Fault or FaultContribution is
 * directly connected to an error. The description of a Reason will be shown to the user in the
 * final report.
 */
public class RankInfo extends FaultInfo {

  /**
   * Reasons can be attached to Faults and FaultContributions. They try to explain why this error
   * occurred or why a certain edge is in the ranked list of potential fault indicators.
   *
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
