// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

/**
 * The collaborators one {@link DssBlockAnalysis} delegates the behavior of its block to, as
 * assembled by {@link DssBlockAnalysisType}.
 *
 * @param preconditions remembers the postconditions received from predecessor blocks
 * @param violationConditions remembers the violation conditions received from successor blocks
 * @param engine explores the block from what the two handlers currently hold
 */
record DssBlockAnalysisComponents(
    DssPreconditionHandler preconditions,
    DssViolationConditionHandler violationConditions,
    DssExplorationEngine engine) {}
