// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.CoverageOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class FunctionPointerStateCoverageOperator implements CoverageOperator {

  @Override
  public boolean isSubsumed(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    FunctionPointerState functionPointerState1 = (FunctionPointerState) state1;
    FunctionPointerState functionPointerState2 = (FunctionPointerState) state2;
    FunctionPointerState.Builder builder1 = functionPointerState1.createBuilder();
    FunctionPointerState.Builder builder2 = functionPointerState2.createBuilder();
    return builder1.getValues().equals(builder2.getValues());
  }

  @Override
  public boolean isBasedOnEquality() {
    return true;
  }
}
