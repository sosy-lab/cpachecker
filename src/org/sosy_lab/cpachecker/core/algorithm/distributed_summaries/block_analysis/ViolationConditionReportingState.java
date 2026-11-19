// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * Interface for states that can report a violation condition.
 *
 * <p>This is used to extract the violation condition from the analysis state after block analysis.
 */
public interface ViolationConditionReportingState {

  /**
   * Get the violation condition represented by this state.
   *
   * @param manager the formula manager to use
   * @return the violation condition
   */
  BooleanFormula getViolationCondition(FormulaManagerView manager);
}
