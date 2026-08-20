// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Remembers the postconditions a block receives from its predecessors, decides when the block has
 * seen enough of them to stop, and re-explores the block from those it considers relevant.
 *
 * @see AlwaysReplacePreconditionHandler
 */
interface DssPreconditionHandler {

  /** Explores the block before any message was received. */
  Collection<DssMessage> runInitialAnalysis()
      throws CPAException, InterruptedException, SolverException;

  /**
   * Stores a received postcondition.
   *
   * @return whether the block has to be re-explored because of this update
   */
  DssMessageProcessing store(DssPostConditionMessage pReceived)
      throws InterruptedException, SolverException, CPAException;

  /** Re-explores the block under all known violation conditions. */
  Collection<DssMessage> analyze() throws SolverException, InterruptedException, CPAException;

  /** Re-explores the block under the violation conditions received from one specific block. */
  Collection<DssMessage> analyzeFor(String pViolationConditionSender)
      throws SolverException, InterruptedException, CPAException;

  /** All preconditions known so far, e.g. to export them as a correctness witness. */
  ImmutableList<@NonNull StateAndPrecision> getKnownPreconditions();

  /** Notified if violation condition changes. */
  void violationConditionsChanged();
}
