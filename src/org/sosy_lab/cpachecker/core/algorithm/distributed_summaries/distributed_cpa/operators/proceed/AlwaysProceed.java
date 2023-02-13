// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.BlockSummaryMessageProcessing;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class AlwaysProceed implements ProceedOperator {
  @Override
  public BlockSummaryMessageProcessing proceedForward(AbstractState pState) {
    return proceed(pState);
  }

  @Override
  public BlockSummaryMessageProcessing proceedBackward(AbstractState pState) {
    return proceed(pState);
  }

  @Override
  public BlockSummaryMessageProcessing proceed(AbstractState pState) {
    return BlockSummaryMessageProcessing.proceed();
  }
}
