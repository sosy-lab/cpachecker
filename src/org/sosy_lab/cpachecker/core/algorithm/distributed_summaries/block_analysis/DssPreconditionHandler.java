// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import com.google.common.collect.ImmutableList;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Remembers the postconditions a block receives from its predecessors, decides when the block has
 * seen enough of them to stop, and decides which of them are still relevant.
 *
 * <p>A handler only keeps track of what arrived. Exploring the block from what it holds is the job
 * of a {@link DssExplorationEngine}, which is built for one concrete handler and reads the stored
 * preconditions back from it in whatever grouping that handler maintains.
 *
 * @see AlwaysReplacePreconditionHandler
 */
interface DssPreconditionHandler {

  /**
   * Stores a received postcondition.
   *
   * @return whether the block has to be re-explored because of this update
   */
  DssMessageProcessing store(DssPostConditionMessage pReceived)
      throws InterruptedException, SolverException, CPAException;

  /** All preconditions known so far, e.g. to export them as a correctness witness. */
  ImmutableList<@NonNull StateAndPrecision> getKnownPreconditions();

  /** Notified if violation condition changes. */
  void violationConditionsChanged();
}
