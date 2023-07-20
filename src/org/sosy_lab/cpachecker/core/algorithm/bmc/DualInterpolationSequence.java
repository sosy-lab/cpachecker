// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import java.util.List;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class DualInterpolationSequence {
  private boolean isLocallyUnsafe;
  private List<BooleanFormula> forwardReachVector;
  private List<BooleanFormula> backwardReachVector;

  public DualInterpolationSequence
      (List<BooleanFormula> pForwardReachVector, List<BooleanFormula> pBackwardReachVector,
       boolean pIsLocallyUnsafe) {
    isLocallyUnsafe = pIsLocallyUnsafe;
    forwardReachVector = pForwardReachVector;
    backwardReachVector = pBackwardReachVector;
  }
  public boolean isLocallyUnsafe() {
    return isLocallyUnsafe;
  }
  public List<BooleanFormula> getForwardReachVector() {
    return forwardReachVector;
  }
  public List<BooleanFormula> getBackwardReachVector() {
    return backwardReachVector;
  }
}
