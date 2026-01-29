// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Distributed Summary Synthesis (DSS) partitions the Control Flow Automaton into blocks and
 * distributes verification across multiple workers that communicate via message passing. Each block
 * computes postconditions (forward propagation) and violation conditions (backward propagation)
 * that are exchanged until a fixpoint is reached.
 *
 * @see org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DistributedSummarySynthesis
 */
package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries;
