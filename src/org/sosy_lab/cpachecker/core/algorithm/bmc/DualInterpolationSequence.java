// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * This class provides a sequence representation for {@link DARAlgorithm}. It stores forward and
 * backward reachability sequences. It can also strengthen the sequences by forward and backward
 * interpolants.
 */
public class DualInterpolationSequence {
  private List<BooleanFormula> forwardReachVector;
  private List<BooleanFormula> backwardReachVector;

  public DualInterpolationSequence() {
    forwardReachVector = new ArrayList<>();
    backwardReachVector = new ArrayList<>();
  }

  public void initializeSequences(PartitionedFormulas pFormulas) {
    extendBackwardReachVector(pFormulas.getAssertionFormula());
    extendForwardReachVector(pFormulas.getPrefixFormula());
  }
  public void updateForwardReachVector(BooleanFormula pNewFormula, int pIndex) {
    forwardReachVector.set(pIndex, pNewFormula);
  }

  public void extendForwardReachVector(BooleanFormula pNewFormula) {
    forwardReachVector.add(pNewFormula);
  }

  public void updateBackwardReachVector(BooleanFormula pNewFormula, int pIndex) {
    backwardReachVector.set(pIndex, pNewFormula);
  }

  public void extendBackwardReachVector(BooleanFormula pNewFormula) {
    backwardReachVector.add(pNewFormula);
  }

  public int getSize() {
    assert forwardReachVector.size() == backwardReachVector.size();
    return forwardReachVector.size();
  }

  public List<BooleanFormula> getForwardReachVector() {
    return forwardReachVector;
  }

  public List<BooleanFormula> getBackwardReachVector() {
    return backwardReachVector;
  }
}
