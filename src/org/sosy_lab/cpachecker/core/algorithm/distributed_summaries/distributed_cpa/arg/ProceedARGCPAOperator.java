// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.arg;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.BlockSummaryMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.java_smt.api.SolverException;

public class ProceedARGCPAOperator implements ProceedOperator {

  private final DistributedConfigurableProgramAnalysis wrapped;

  public ProceedARGCPAOperator(DistributedConfigurableProgramAnalysis pWrapped) {
    wrapped = pWrapped;
  }

  @Override
  public BlockSummaryMessageProcessing proceedForward(AbstractState pState)
      throws InterruptedException, SolverException {
    assert pState instanceof ARGState : pState + " is not an instance of " + ARGState.class;
    return wrapped.getProceedOperator().proceedForward(((ARGState) pState).getWrappedState());
  }

  @Override
  public BlockSummaryMessageProcessing proceedBackward(AbstractState pState)
      throws InterruptedException, SolverException {
    assert pState instanceof ARGState : pState + " is not an instance of " + ARGState.class;
    return wrapped.getProceedOperator().proceedBackward(((ARGState) pState).getWrappedState());
  }

  @Override
  public BlockSummaryMessageProcessing proceed(AbstractState pState)
      throws InterruptedException, SolverException {
    assert pState instanceof ARGState : pState + " is not an instance of " + ARGState.class;
    return wrapped.getProceedOperator().proceed(((ARGState) pState).getWrappedState());
  }
}
