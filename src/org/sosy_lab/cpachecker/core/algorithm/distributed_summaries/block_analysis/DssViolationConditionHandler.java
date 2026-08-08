// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssViolationConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Remembers the violation conditions a block receives from its successors and decides when a
 * received condition adds nothing new.
 *
 * @see AlwaysReplaceViolationConditionHandler
 * @see PathBasedViolationConditionHandler
 */
interface DssViolationConditionHandler {

  /**
   * Stores the violation conditions of one received message.
   *
   * @return whether the block has to be re-explored because of this update
   */
  DssMessageProcessing store(DssViolationConditionMessage pReceived)
      throws InterruptedException, SolverException;

  /** Whether no violation condition is known at all. */
  boolean isEmpty();

  /** Whether no violation condition of the given block is known. */
  boolean isEmptyFor(String pSenderId);

  /**
   * The conditions to explore the block under: those received from one specific block, or all known
   * ones if no block is given.
   */
  ImmutableList<AbstractState> statesOf(Optional<String> pSenderId);
}
