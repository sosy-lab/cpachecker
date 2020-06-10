// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.tarantula.TarantulaDatastructure;

import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
/** Class represents a special fault where a line has its CFAEdge with its suspicious */
public class TarantulaFault implements Comparable<TarantulaFault> {

  private final double lineScore;
  private final Fault fault;
  private final TarantulaCFAEdgeSuspicious tarantulaCFAEdgeSuspicious;

  public TarantulaFault(
      double pLineScore, Fault pFault, TarantulaCFAEdgeSuspicious pTarantulaCFAEdgeSuspicious) {

    this.lineScore = pLineScore;
    this.fault = pFault;
    this.tarantulaCFAEdgeSuspicious = pTarantulaCFAEdgeSuspicious;
  }

  public double getLineScore() {
    return lineScore;
  }

  public Fault getFault() {
    return fault;
  }

  public TarantulaCFAEdgeSuspicious getTarantulaCFAEdgeSuspicious() {
    return tarantulaCFAEdgeSuspicious;
  }

  @Override
  public int compareTo(TarantulaFault o) {
    if (o.getLineScore() < this.getLineScore()) {
      return -1;
    } else if (this.getLineScore() < o.getLineScore()) {
      return 1;
    }
    return 0;
  }

  @Override
  public String toString() {
    return "TarantulaFault{"
        + "score="
        + lineScore
        + ", fault="
        + fault
        + ", tarantulaCFAEdgeSuspicious="
        + tarantulaCFAEdgeSuspicious
        + '}';
  }
}
