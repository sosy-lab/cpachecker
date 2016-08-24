/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.bmc;

import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

import java.io.PrintStream;

public class BMCStatistics implements Statistics {

  final Timer bmcPreparation = new Timer();
  final Timer satCheck = new Timer();
  final Timer errorPathCreation = new Timer();
  final Timer assertionsCheck = new Timer();

  final Timer inductionPreparation = new Timer();
  final Timer inductionCheck = new Timer();

  @Override
  public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
    if (bmcPreparation.getNumberOfIntervals() > 0) {
      out.println("Time for BMC formula creation:       " + bmcPreparation);
    }
    if (satCheck.getNumberOfIntervals() > 0) {
      out.println("Time for final sat check:            " + satCheck);
    }
    if (errorPathCreation.getNumberOfIntervals() > 0) {
      out.println("Time for error path creation:        " + errorPathCreation);
    }
    if (assertionsCheck.getNumberOfIntervals() > 0) {
      out.println("Time for bounding assertions check:  " + assertionsCheck);
    }
    if (inductionCheck.getNumberOfIntervals() > 0) {
      out.println("Time for induction formula creation: " + inductionPreparation);
      out.println("Time for induction check:            " + inductionCheck);
    }
  }

  @Override
  public String getName() {
    return "BMC algorithm";
  }
}