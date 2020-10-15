// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.numeric.merge_operator;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.numeric.NumericState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.numericdomains.DomainFunction;
import org.sosy_lab.numericdomains.Manager;

/**
 * Merges two states using the join operation.
 *
 * <p>This leads to a reduction in states at the cost of precision.
 */
class NumericMergeJoinOperator implements MergeOperator {
  public NumericMergeJoinOperator(Manager pManager) throws InvalidConfigurationException {
    if (!pManager.implementsFunction(DomainFunction.JOIN)) {
      throw new InvalidConfigurationException("Cannot use mergeJoin with chosen domain.");
    }
  }

  @Override
  public AbstractState merge(AbstractState pState1, AbstractState pState2, Precision precision)
      throws CPAException, InterruptedException {
    if (!(pState1 instanceof NumericState && pState2 instanceof NumericState)) {
      throw new AssertionError(
          "Can not use NumericMergeJoinOperator to merge states other than NumericStates");
    }
    NumericState state1 = (NumericState) pState1;
    NumericState state2 = (NumericState) pState2;

    return state1.join(state2);
  }
}
