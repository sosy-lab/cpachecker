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
