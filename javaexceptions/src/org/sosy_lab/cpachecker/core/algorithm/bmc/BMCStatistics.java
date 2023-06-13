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

  // General BMC operations
  final Timer bmcPreparation = new Timer();
  final Timer satCheck = new Timer();
  final Timer errorPathCreation = new Timer();
  final Timer errorPathProcessing = new Timer();
  final Timer assertionsCheck = new Timer();

  // k-Induction operations
  final Timer inductionPreparation = new Timer();
  final Timer inductionCheck = new Timer();

  // IMC/ISMC operations
  final Timer interpolationPreparation = new Timer();
  final Timer fixedPointComputation = new Timer();
  int numOfIMCInnerIterations = -1;

  @Override
  public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
    if (bmcPreparation.getNumberOfIntervals() > 0) {
      out.println("Time for BMC formula creation:                   " + bmcPreparation);
    }
    if (satCheck.getNumberOfIntervals() > 0) {
      out.println("Time for final sat check:                        " + satCheck);
    }
    if (errorPathCreation.getNumberOfIntervals() > 0) {
      out.println("Time for error-path creation:                    " + errorPathCreation);
    }
    if (errorPathProcessing.getNumberOfIntervals() > 0) {
      out.println("Time for error-path post-processing:             " + errorPathProcessing);
    }
    if (assertionsCheck.getNumberOfIntervals() > 0) {
      out.println("Time for bounding assertions check:              " + assertionsCheck);
    }
    if (inductionCheck.getNumberOfIntervals() > 0) {
      out.println("Time for induction formula creation:             " + inductionPreparation);
      out.println("Time for induction check:                        " + inductionCheck);
    }
    if (fixedPointComputation.getNumberOfIntervals() > 0) {
      out.println("Time for collecting formulas for interpolation:  " + interpolationPreparation);
      out.println("Time for computing fixed point by interpolation: " + fixedPointComputation);
    }
    if (numOfIMCInnerIterations >= 0) {
      out.println("Number of IMC inner iterations:                  " + numOfIMCInnerIterations);
    }
  }

  @Override
  public String getName() {
    return "BMC algorithm";
  }
}
