// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

/**
 * The collaborators a {@link DssBlockAnalysis} is assembled from, selected by the configuration
 * option {@code distributedSummaries.blockAnalysisType}.
 */
public enum DssBlockAnalysisType {

  /** Keeps only the latest message of every neighboring block and location hash. */
  ALWAYS_REPLACE {
    @Override
    DssBlockAnalysisComponents createComponents(DssBlockAnalysis pAnalysis)
        throws InterruptedException {
      AlwaysReplaceViolationConditionHandler violationConditions =
          new AlwaysReplaceViolationConditionHandler(pAnalysis);
      AlwaysReplacePreconditionHandler preconditions =
          new AlwaysReplacePreconditionHandler(pAnalysis);
      return new DssBlockAnalysisComponents(
          preconditions,
          violationConditions,
          new AlwaysReplaceExplorationEngine(pAnalysis, preconditions, violationConditions));
    }
  },

  /**
   * Keeps only the latest message of every neighboring block, skips predecessors whose
   * preconditions are covered by those of another predecessor, and re-explores only the
   * predecessors and violation conditions that changed. Ignores program points, so it is meant for
   * the inlining decomposition.
   */
  PARTIAL_REPLACE {
    @Override
    DssBlockAnalysisComponents createComponents(DssBlockAnalysis pAnalysis)
        throws InterruptedException {
      PartialReplaceViolationConditionHandler violationConditions =
          new PartialReplaceViolationConditionHandler(pAnalysis);
      PartialReplacePreconditionHandler preconditions =
          new PartialReplacePreconditionHandler(pAnalysis);
      return new DssBlockAnalysisComponents(
          preconditions,
          violationConditions,
          new PartialReplaceExplorationEngine(pAnalysis, preconditions, violationConditions));
    }
  };

  /**
   * Creates the precondition handler, the violation-condition handler and the exploration engine of
   * one block analysis.
   *
   * <p>All three are created together because an engine is built for the concrete handlers it
   * reads: how a handler groups what it stores is exactly what the engine has to know in order to
   * explore it.
   */
  abstract DssBlockAnalysisComponents createComponents(DssBlockAnalysis pAnalysis)
      throws InterruptedException;
}
