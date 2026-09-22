// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import java.util.Optional;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Explores a block from the conditions its handlers currently hold.
 *
 * <p>The engine is the collaborator of a {@link DssBlockAnalysis} that runs the CPA. It reads the
 * preconditions of a {@link DssPreconditionHandler} and the violation conditions of a {@link
 * DssViolationConditionHandler}, decides how to combine them into CPA runs, and reports what those
 * runs found as an {@link AnalysisResult}. Turning that result into messages for other blocks is
 * left to {@link DssBlockAnalysis}, so that all engines publish their findings the same way.
 *
 * <p>An engine is created for the concrete handlers it reads (see {@link
 * DssBlockAnalysisType#createComponents}), because how a handler groups what it stores is exactly
 * what the engine has to know in order to explore it.
 *
 * @see AlwaysReplaceExplorationEngine
 */
interface DssExplorationEngine {

  /** Explores the block before any message was received. */
  AnalysisResult exploreInitially() throws CPAException, InterruptedException;

  /**
   * Re-explores the block after one of the handlers asked the analysis to proceed.
   *
   * @param pViolationConditionSender the successor block whose violation conditions triggered this
   *     round, or empty if a received postcondition triggered it
   */
  AnalysisResult explore(Optional<String> pViolationConditionSender)
      throws CPAException, InterruptedException;
}
