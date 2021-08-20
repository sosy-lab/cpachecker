// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.task;

import com.google.common.collect.Table;
import java.util.Map;
import java.util.concurrent.Callable;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.ShareableBooleanFormula;

/**
 * {@link Task} provides a common interface for all classes which implement subtasks of concurrent
 * analysis.
 */
public interface Task extends Callable<AlgorithmStatus> {
  /**
   * Preprocess the task before it gets scheduled. This method gets invoked by the central scheduler
   * and runs within its thread. As such, all calls to {@link #preprocess(Table, Map)} across
   * different tasks are executed in sequential order. Because they run in the context of the
   * scheduler thread, its crucial that this method performs as little work as possible, because
   * further scheduling of new tasks gets blocked until the method completes.
   *
   * <p>The passed {@link Table} 'summaries' is maintained by the scheduler and centrally manages
   * the latest known block summary for each edge between two {@link Block}s. Depending on the task
   * type, the method can a) update block summaries within it, or b) modify the task parameters with
   * the information stored in the table.
   *
   * @param pSummaries Latest block summaries
   * @param pSummaryVersions Version counter for block summaries
   */
  TaskValidity preprocess(
      final Table<Block, Block, ShareableBooleanFormula> pSummaries,
      final Map<Block, Integer> pSummaryVersions);
}
