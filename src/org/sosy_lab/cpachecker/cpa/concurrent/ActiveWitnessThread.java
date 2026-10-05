// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.concurrent;

/**
 * The identifier by which a violation witness refers to the thread that executes the edge POR is
 * currently handling.
 *
 * <p>{@link ConcurrentTransferRelation} writes it before handing an edge to the wrapped transfer
 * relation, and {@link OriginalEdgeCPA} reads it while the witness automaton evaluates that edge's
 * guards. The handover is a mutable field because the identifier belongs to the edge rather than to
 * any state of the composite, the same reason {@code MutexState} is told an edge's PID by {@link
 * ConcurrentTransferRelation} instead of deriving it; one edge is handled at a time, so no two
 * values are ever in flight.
 */
final class ActiveWitnessThread {

  private int witnessThreadId = ThreadState.NO_WITNESS_THREAD_ID;

  void set(int pWitnessThreadId) {
    witnessThreadId = pWitnessThreadId;
  }

  int get() {
    return witnessThreadId;
  }
}
