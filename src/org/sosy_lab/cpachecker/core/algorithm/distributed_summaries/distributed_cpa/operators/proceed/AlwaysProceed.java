// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed;

import java.util.Collection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class AlwaysProceed implements ProceedOperator {

  AlwaysProceed() {}

  @Override
  public DssMessageProcessing processForward(AbstractState pState) {
    return DssMessageProcessing.proceed();
  }

  @Override
  public DssMessageProcessing processBackward(
      AbstractState pState, Collection<AbstractState> pKnownStates) {
    return DssMessageProcessing.proceed();
  }
}
