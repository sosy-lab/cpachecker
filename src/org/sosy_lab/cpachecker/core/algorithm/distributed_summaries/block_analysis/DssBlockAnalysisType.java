// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

// TODO Only changes Precondition handler -> refactor!
/**
 * The pair of handlers a {@link DssBlockAnalysis} is assembled from, selected by the configuration
 * option {@code distributedSummaries.blockAnalysisType}.
 */
public enum DssBlockAnalysisType {

  /** Distinguishes contexts by the path through the block graph that produced them. */
  PATH_BASED {
    @Override
    DssPreconditionHandler createPreconditionHandler(DssBlockAnalysis pAnalysis)
        throws InterruptedException {
      return new PathBasedPreconditionHandler(pAnalysis);
    }

    @Override
    DssViolationConditionHandler createViolationConditionHandler(DssBlockAnalysis pAnalysis) {
      return new PathBasedViolationConditionHandler(pAnalysis);
    }
  },

  /** Keeps only the latest message of every neighboring block. */
  ALWAYS_REPLACE {
    @Override
    DssPreconditionHandler createPreconditionHandler(DssBlockAnalysis pAnalysis)
        throws InterruptedException {
      return new AlwaysReplacePreconditionHandler(pAnalysis);
    }

    @Override
    DssViolationConditionHandler createViolationConditionHandler(DssBlockAnalysis pAnalysis) {
      return new AlwaysReplaceViolationConditionHandler(pAnalysis);
    }
  };

  abstract DssPreconditionHandler createPreconditionHandler(DssBlockAnalysis pAnalysis)
      throws InterruptedException;

  abstract DssViolationConditionHandler createViolationConditionHandler(DssBlockAnalysis pAnalysis);
}
