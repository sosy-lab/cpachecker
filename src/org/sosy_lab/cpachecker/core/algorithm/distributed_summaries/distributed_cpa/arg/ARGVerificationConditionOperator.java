// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.arg;

import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.VerificationConditionOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.java_smt.api.SolverException;

public class ARGVerificationConditionOperator implements VerificationConditionOperator {

  private final DistributedConfigurableProgramAnalysis wrappedCPA;

  public ARGVerificationConditionOperator(DistributedConfigurableProgramAnalysis pWrappedCPA) {
    wrappedCPA = pWrappedCPA;
  }

  @Override
  public Optional<AbstractState> computeVerificationCondition(
      ARGPath pARGPath, Optional<ARGState> pPreviousCondition)
      throws InterruptedException, CPATransferException, SolverException {
    return wrappedCPA
        .getVerificationConditionOperator()
        .computeVerificationCondition(pARGPath, pPreviousCondition)
        .map(state -> new ARGState(state, null));
  }
}
