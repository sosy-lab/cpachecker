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
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;

public class TarantulaFault {

  double score;
  Fault fault;
  FaultContribution faultContribution;

  public TarantulaFault(double pScore, Fault pFault, FaultContribution pFaultContribution) {

    this.score = pScore;
    this.fault = pFault;
    this.faultContribution = pFaultContribution;
  }

  public double getScore() {
    return score;
  }

  public FaultContribution getFaultContribution() {
    return faultContribution;
  }

  public Fault getFault() {
    return fault;
  }

  @Override
  public String toString() {
    return "TarantulaFault{"
        + "score="
        + score
        + ", fault="
        + fault
        + ", faultContribution="
        + faultContribution
        + '}';
  }
}
