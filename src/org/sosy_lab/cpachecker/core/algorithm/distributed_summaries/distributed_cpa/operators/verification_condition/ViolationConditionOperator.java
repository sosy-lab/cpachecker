// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition;

import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssARGPathGraph;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Interface for operators that compute the violation conditions for a given path regarding previous
 * conditions.
 */
public interface ViolationConditionOperator {

  /**
   * Compute the violation conditions at the start of the given path.
   *
   * <p>The path may be a {@link DssARGPathGraph}, which stands for all paths it contains. The
   * result then covers every one of them. Only the composite operator and operators wrapping it
   * accept graphs; operators of component CPAs expect a single path.
   *
   * @param pARGPath The path or path graph to compute the violation conditions for.
   * @param pPreviousCondition The previous condition to consider.
   * @return The computed violation conditions. Empty if forward analysis of block does not match
   *     the given previous condition.
   */
  List<AbstractState> computeViolationConditions(
      ARGPath pARGPath, Optional<ARGState> pPreviousCondition)
      throws InterruptedException, CPATransferException, SolverException;
}
