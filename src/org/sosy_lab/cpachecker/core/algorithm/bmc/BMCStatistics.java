// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import com.google.common.math.IntMath;
import java.io.PrintStream;
import java.math.RoundingMode;
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

  // IMC/ISMC/DAR operations
  final Timer interpolationPreparation = new Timer();
  final Timer fixedPointComputation = new Timer();
  int numOfInterpolationCalls = -1;
  int numOfInterpolants = -1;
  int numOfAtomsInInterpolants = -1;
  int minNumOfAtomsInInterpolants = -1;
  int maxNumOfAtomsInInterpolants = -1;
  int numOfVarsInInterpolants = -1;
  int minNumOfVarsInInterpolants = -1;
  int maxNumOfVarsInInterpolants = -1;
  int fixedPointConvergenceLength = -1;
  // DAR specific
  int numOfDARGlobalPhases = -1;
  int numOfDARLocalPhases = -1;
  int numOfDARLocalInterpolants = -1;

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
      out.println("Time for computing fixed-point by interpolation: " + fixedPointComputation);
    }
    if (numOfInterpolants >= 0) {
      out.println("Total number of computed interpolants:           " + numOfInterpolants);
    }
    if (numOfInterpolationCalls >= 0) {
      out.println("Total number of interpolation calls:             " + numOfInterpolationCalls);
    }
    if (numOfAtomsInInterpolants >= 0) {
      out.println("Total number of atoms in interpolants:           " + numOfAtomsInInterpolants);
      out.println(
          "  Avg. #atoms in itp:                            "
              + IntMath.divide(numOfAtomsInInterpolants, numOfInterpolants, RoundingMode.HALF_UP));
      out.println(
          "  Min. #atoms in itp:                            " + minNumOfAtomsInInterpolants);
      out.println(
          "  Max. #atoms in itp:                            " + maxNumOfAtomsInInterpolants);
    }
    if (numOfVarsInInterpolants >= 0) {
      out.println("Total number of vars in interpolants:            " + numOfVarsInInterpolants);
      out.println(
          "  Avg. #vars in itp:                             "
              + IntMath.divide(numOfVarsInInterpolants, numOfInterpolants, RoundingMode.HALF_UP));
      out.println("  Min. #vars in itp:                             " + minNumOfVarsInInterpolants);
      out.println("  Max. #vars in itp:                             " + maxNumOfVarsInInterpolants);
    }
    if (fixedPointConvergenceLength >= 0) {
      out.println(
          "Fixed-point convergence length:                  " + fixedPointConvergenceLength);
    }
    if (numOfDARGlobalPhases >= 0) {
      out.println("Number of DAR global strengthening phases:       " + numOfDARGlobalPhases);
      if (fixedPointConvergenceLength >= 0) {
        out.println(
            "  Ratio to convergence length:                   "
                + (float) numOfDARGlobalPhases / fixedPointConvergenceLength);
      }
    }
    if (numOfDARLocalPhases >= 0) {
      out.println("Number of DAR local strengthening phases:        " + numOfDARLocalPhases);
      assert numOfDARLocalInterpolants >= 0;
      out.println("  Number of local interpolants:                  " + numOfDARLocalInterpolants);
    }
  }

  @Override
  public String getName() {
    return "BMC algorithm";
  }
}
