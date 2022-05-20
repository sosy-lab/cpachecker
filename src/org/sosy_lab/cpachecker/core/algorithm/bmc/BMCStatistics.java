// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import java.io.PrintStream;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

public class BMCStatistics implements Statistics {

  final Timer bmcPreparation = new Timer();
  final Timer satCheck = new Timer();
  final Timer errorPathCreation = new Timer();
  final Timer assertionsCheck = new Timer();

  final Timer inductionPreparation = new Timer();
  final Timer inductionCheck = new Timer();

  @Override
  public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
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