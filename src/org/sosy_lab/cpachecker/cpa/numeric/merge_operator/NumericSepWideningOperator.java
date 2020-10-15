// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.numeric.merge_operator;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.numeric.NumericState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.numericdomains.DomainFunction;
import org.sosy_lab.numericdomains.Manager;

/**
 * Merges two states using the widening operation if both states are loop heads, otherwise it works
 * like the {@link MergeSepOperator}.
 *
 * <p>If both states
 */
class NumericSepWideningOperator implements NumericMergeOperator {
  private final MergeOperator mergeSep;

  public NumericSepWideningOperator(Manager pManager) throws InvalidConfigurationException {
    mergeSep = new MergeSepOperator();
    if (!pManager.implementsFunction(DomainFunction.WIDENING)
        && !pManager.implementsFunction(DomainFunction.IS_LEQ)) {
      throw new InvalidConfigurationException("Cannot use sepWidening with chosen domain.");
    }
  }

  @Override
  public AbstractState merge(AbstractState pState1, AbstractState pState2, Precision precision)
      throws CPAException, InterruptedException {
    if (!(pState1 instanceof NumericState && pState2 instanceof NumericState)) {
      throw new AssertionError(
          "Can not use NumericMergeWideningOperator to merge states other than NumericStates");
    }
    NumericState state1 = (NumericState) pState1;
    NumericState state2 = (NumericState) pState2;

    if (state1.isLoopHead() && state2.isLoopHead()) {
      return state1.widening(state2);
    } else {
      return mergeSep.merge(pState1, pState2, precision);
    }
  }

  @Override
  public boolean usesLoopInformation() {
    return true;
  }
}
