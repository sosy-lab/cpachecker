// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.arg;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
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
  public DssMessageProcessing processForward(AbstractState pState)
      throws InterruptedException, SolverException {
    Preconditions.checkArgument(
        pState instanceof ARGState, "%s is not an instance of %s", pState, ARGState.class);
    return wrapped.getProceedOperator().processForward(((ARGState) pState).getWrappedState());
  }

  @Override
  public DssMessageProcessing processBackward(AbstractState pState)
      throws InterruptedException, SolverException {
    Preconditions.checkArgument(
        pState instanceof ARGState, "%s is not an instance of %s", pState, ARGState.class);
    return wrapped.getProceedOperator().processBackward(((ARGState) pState).getWrappedState());
  }
}
