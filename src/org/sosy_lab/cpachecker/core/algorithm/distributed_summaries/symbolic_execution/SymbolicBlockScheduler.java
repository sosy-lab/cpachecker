// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.symbolic_execution;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/** Decides which thread analyzes which block when, until the program is proven safe or unsafe. */
interface SymbolicBlockScheduler {

  /** Creates the analysis of one thread, which stops once the given notifier requests it. */
  @FunctionalInterface
  interface AnalysisFactory {
    SymbolicBlockAnalysis create(ShutdownNotifier pShutdownNotifier)
        throws InvalidConfigurationException;
  }

  /**
   * Runs the symbolic execution of the given blocks.
   *
   * @return {@link Result#FALSE} if a block found a violation, {@link Result#TRUE} if no block has
   *     work left
   */
  Result run(ImmutableList<SymbolicBlockWorker> pWorkers) throws CPAException, InterruptedException;
}
