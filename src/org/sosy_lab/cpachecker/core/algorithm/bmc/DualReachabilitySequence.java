// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * This class provides a sequence representation for {@link DARAlgorithm}. It stores forward and
 * backward reachability sequences. It can also strengthen the sequences by forward and backward
 * interpolants.
 */
class DualReachabilitySequence {
  private List<BooleanFormula> forwardReachVector;
  private List<BooleanFormula> backwardReachVector;

  DualReachabilitySequence(PartitionedFormulas pFormulas) {
    forwardReachVector = new ArrayList<>();
    backwardReachVector = new ArrayList<>();
    extendBackwardReachVector(pFormulas.getAssertionFormula());
    extendForwardReachVector(pFormulas.getPrefixFormula());
  }

  void updateForwardReachVector(BooleanFormula pNewFormula, int pIndex) {
    forwardReachVector.set(pIndex, pNewFormula);
  }

  void extendForwardReachVector(BooleanFormula pNewFormula) {
    forwardReachVector.add(pNewFormula);
  }

  void updateBackwardReachVector(BooleanFormula pNewFormula, int pIndex) {
    backwardReachVector.set(pIndex, pNewFormula);
  }

  void extendBackwardReachVector(BooleanFormula pNewFormula) {
    backwardReachVector.add(pNewFormula);
  }

  int getSize() {
    assert forwardReachVector.size() == backwardReachVector.size();
    return forwardReachVector.size();
  }

  BooleanFormula getForwardImageAt(int i) {
    return forwardReachVector.get(i);
  }

  BooleanFormula getBackwardImageAt(int i) {
    return backwardReachVector.get(i);
  }

  ImmutableList<BooleanFormula> getForwardReachVector() {
    return ImmutableList.copyOf(forwardReachVector);
  }

  ImmutableList<BooleanFormula> getBackwardReachVector() {
    return ImmutableList.copyOf(backwardReachVector);
  }
}
