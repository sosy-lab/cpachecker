// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssViolationConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Remembers the violation conditions a block receives from its successors and decides when a
 * received condition adds nothing new.
 *
 * <p>A handler only keeps track of what arrived. Exploring the block under what it holds is the job
 * of a {@link DssExplorationEngine}, which asks for the conditions through {@link #states()}.
 *
 * @see AlwaysReplaceViolationConditionHandler
 */
interface DssViolationConditionHandler {

  /**
   * Stores the violation conditions of one received message.
   *
   * @return whether the block has to be re-explored because of this update
   */
  DssMessageProcessing store(DssViolationConditionMessage pReceived)
      throws InterruptedException, SolverException, CPAException;

  /** All known violation conditions, i.e., the conditions to explore the block under. */
  ImmutableList<AbstractState> states();
}
