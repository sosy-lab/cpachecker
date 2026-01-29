// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition;

import java.util.Optional;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Interface for operators that compute the verification condition for a given path regarding
 * previous conditions.
 */
public interface ViolationConditionOperator {

  /**
   * Compute the verification condition for the given path.
   *
   * @param pARGPath The path to compute the verification condition for.
   * @param pPreviousCondition The previous condition to consider.
   * @return The computed verification condition. Empty if forward analysis of block does not match
   *     the given previous condition.
   */
  Optional<AbstractState> computeViolationCondition(
      ARGPath pARGPath, Optional<ARGState> pPreviousCondition)
      throws InterruptedException, CPATransferException, SolverException;
}
