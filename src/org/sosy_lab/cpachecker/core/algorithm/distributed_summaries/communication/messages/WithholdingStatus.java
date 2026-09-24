// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

/**
 * Whether a block currently withholds the postcondition of at least one of its contexts, as
 * announced by that block.
 *
 * <p>Statuses are passed on from block to block, also around cycles of the block graph, so a block
 * may receive an outdated status after a newer one. The epoch orders the statuses of one block: it
 * grows with every change the block announces, and of two statuses the one with the higher epoch is
 * the current one.
 *
 * @param epoch the number of changes the block announced before this status
 * @param withholding whether the block withholds a postcondition
 */
public record WithholdingStatus(int epoch, boolean withholding) {

  public boolean isNewerThan(WithholdingStatus pOther) {
    return epoch > pOther.epoch;
  }
}
