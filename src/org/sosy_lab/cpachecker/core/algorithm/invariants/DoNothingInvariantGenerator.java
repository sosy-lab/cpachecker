// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.invariants;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Trivial implementation of an invariant generator that does nothing and always returns the
 * invariant true.
 */
public class DoNothingInvariantGenerator extends AbstractInvariantGenerator {

  @Override
  protected void startImpl(CFANode pInitialLocation) {}

  @Override
  public void cancel() {}

  @Override
  public boolean isProgramSafe() {
    return false;
  }

  @Override
  public InvariantSupplier getSupplier() throws CPAException, InterruptedException {
    return InvariantSupplier.TrivialInvariantSupplier.INSTANCE;
  }

  @Override
  public ExpressionTreeSupplier getExpressionTreeSupplier()
      throws CPAException, InterruptedException {
    return ExpressionTreeSupplier.TrivialInvariantSupplier.INSTANCE;
  }
}
