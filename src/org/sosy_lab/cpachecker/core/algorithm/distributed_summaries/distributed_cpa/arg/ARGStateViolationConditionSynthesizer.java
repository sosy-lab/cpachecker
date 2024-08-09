// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.arg;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.violation_condition.ViolationCondition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.violation_condition.ViolationConditionSynthesizer;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.java_smt.api.SolverException;

public class ARGStateViolationConditionSynthesizer implements ViolationConditionSynthesizer {

  private final DistributedConfigurableProgramAnalysis wrapped;

  public ARGStateViolationConditionSynthesizer(DistributedConfigurableProgramAnalysis pWrapped) {
    wrapped = pWrapped;
  }

  @Override
  public ViolationCondition computeViolationCondition(ARGPath pARGPath, ARGState pPreviousCondition)
      throws InterruptedException, CPATransferException, SolverException {
    ViolationCondition violationCondition =
        wrapped.computeViolationCondition(pARGPath, pPreviousCondition);
    if (violationCondition.isFeasible()) {
      return ViolationCondition.feasibleCondition(
          new ARGState(violationCondition.getViolation(), null));
    }
    return violationCondition;
  }
}
