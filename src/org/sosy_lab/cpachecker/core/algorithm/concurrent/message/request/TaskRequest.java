// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.message.request;

import com.google.common.collect.Table;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.Scheduler.MessageProcessingVisitor;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.Message;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.Task;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.backward.BackwardAnalysisFull;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.forward.ForwardAnalysisCore;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.ShareableBooleanFormula;

public interface TaskRequest extends Message {
  /**
   * Preprocess the task before it gets scheduled. This method is invoked by the central scheduler
   * and runs within its thread. As such, all calls to {@link #process(Table, Map, Set)} across
   * different tasks are executed in sequential order. Because they run in the context of the
   * scheduler thread, its crucial that this method performs as little work as possible, because
   * further scheduling of new tasks gets blocked until the method completes.
   *
   * <p>The passed {@link Table} {@code pSummaries} and {@link Map} {@code pSummaryVersions} are
   * maintained by the scheduler and centrally manage the latest known block summary and its version
   * for each edge between two {@link Block}s. Depending on the task type, the method can a) update
   * block summaries within it, or b) modify the task parameters with the information stored in the
   * table. A {@link ForwardAnalysisCore} always updates <em>all</em> block summaries along outgoing
   * edges of a {@link Block}. Therefore, the versions of all such summaries are identical, and are
   * stored as only one value for the whole {@link Block}. Therefore, {@code pSummaryVersions} is a
   * simple {@link Map}, while {@code pSummaries} requires a {@link Table}.
   *
   * <p>Similarily, the {@link Set} {@code pAlreadyPropagated} centrally stores whether the
   * algorithm has already scheduled a {@link BackwardAnalysisFull} starting from a {@link CFANode}. It
   * can be used to prevent the repeated propagation of the same error condition, which would
   * otherwise occur if a refined {@link ForwardAnalysisCore} encounters the same target location as a
   * preceding one before.
   *
   * @param pSummaries         Latest block summaries
   * @param pSummaryVersions   Version counter for block summaries
   * @param pAlreadyPropagated CFANodes from which a BackwardAnalysisFull has already emerged
   */
  Task process(
      final Table<Block, Block, ShareableBooleanFormula> pSummaries,
      final Map<Block, Integer> pSummaryVersions,
      final Set<CFANode> pAlreadyPropagated) throws RequestInvalidatedException;
  
  @Override default void accept(final MessageProcessingVisitor visitor) {
    visitor.visit(this);
  }
}
