// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.total_strict_order;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

/**
 * A program contains a Total Strict Order (TS0) e.g. when one thread t0 calls pthread_join(t1)
 * while t1 has not terminated yet -> t1 needs to execute all edges until termination (=
 * precedingEdges) until t0 can execute pthread_join(t1) (= subsequentEdge).
 */
public class TSO {

  // TODO Total strict order in program location q ("a <q b") possible cases:
  //  pthread_mutex_lock / mutex_unlock
  //  pthread_join
  //  pthread_barrier_wait
  //  pthread_mutex_cond_wait / cond_signal
  //  pthread_rwlock_rdlock / unlock
  //  pthread_rwlock_wrlock / unlock
  //  pthread_key_create / setspecific
  //  flags (e.g. while (flag == 0); though this is difficult to extract from the code?)
  //  __atomic_store_n / __atomic_load_n
  //  atomic blocks
  //  sequential blocks

  /** How the TotalStrictOrder was induced. Only used for debugging purposes. */
  public final TSOType type;

  /** The thread executing {@link TSO#precedingEdges}. */
  public final MPORThread precedingThread;

  /** The thread executing {@link TSO#subsequentEdges}. */
  public final MPORThread subsequentThread;

  /** The set of CFAEdges that must be executed before {@link TSO#subsequentEdges}. */
  public final ImmutableSet<CFAEdge> precedingEdges;

  /**
   * The CFAEdges that can be executed only once all {@link TSO#precedingEdges} are executed.
   * Usually, there is only one subsequent edge, but they can also be nondeterministic. The
   * predecessor CFANode of all subsequentEdges is always the same.
   */
  public final ImmutableSet<CFAEdge> subsequentEdges;

  public TSO(
      TSOType pType,
      MPORThread pPrecedingThread,
      MPORThread pSubsequentThread,
      ImmutableSet<CFAEdge> pPrecedingEdges,
      ImmutableSet<CFAEdge> pSubsequentEdges) {
    type = pType;
    precedingThread = pPrecedingThread;
    precedingEdges = pPrecedingEdges;
    subsequentThread = pSubsequentThread;
    subsequentEdges = pSubsequentEdges;
  }
}