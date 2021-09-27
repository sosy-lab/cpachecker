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
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.ShareableBooleanFormula;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.backward.BackwardAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.forward.ForwardAnalysis;

public interface TaskRequest {
  /**
   * Preprocess the task before it gets scheduled. This method is invoked by the central scheduler
   * and runs within its thread. As such, all calls to {@link #finalize(Table, Map, Table)} across
   * different tasks are executed in sequential order. Because they run in the context of the
   * scheduler thread, its crucial that this method performs as little work as possible, because
   * further scheduling of new tasks gets blocked until the method completes.
   *
   * <p>The passed {@link Table} {@code pSummaries} and {@link Map} {@code pSummaryVersions} are
   * maintained by the scheduler and centrally manage the latest known block summary and its version
   * for each edge between two {@link Block}s. Depending on the task type, the method can a) update
   * block summaries within it, or b) modify the task parameters with the information stored in the
   * table. A {@link ForwardAnalysis} always updates <em>all</em> block summaries along outgoing
   * edges of a {@link Block}. Therefore, the versions of all such summaries are identical, and are
   * stored as only one value for the whole {@link Block}. Therefore, {@code pSummaryVersions} is a
   * simple {@link Map}, while {@code pSummaries} requires a {@link Table}.
   *
   * <p>Similarily, the {@link Set} {@code pAlreadyPropagated} centrally stores whether the
   * algorithm has already scheduled a {@link BackwardAnalysis} starting from a {@link CFANode}. It
   * can be used to prevent the repeated propagation of the same error condition, which would
   * otherwise occur if a refined {@link ForwardAnalysis} encounters the same target location as a
   * preceding one before.
   *
   * @param pSummaries         Latest block summaries
   * @param pSummaryVersions   Version counter for block summaries
   * @param pAlreadyPropagated CFANodes from which a BackwardAnalysis has already emerged
   */
  Task finalize(
      final Table<Block, Block, ShareableBooleanFormula> pSummaries,
      final Map<Block, Integer> pSummaryVersions,
      final Set<CFANode> pAlreadyPropagated)
      throws TaskInvalidatedException;
}
