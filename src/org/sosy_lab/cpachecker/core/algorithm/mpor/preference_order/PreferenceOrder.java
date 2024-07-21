// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.preference_order;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * A program contains a PreferenceOrder e.g. when one thread t0 calls pthread_join(t1) while t1 has
 * not terminated yet -> t1 needs to execute all edges until termination (= precedingEdges) until t0
 * can execute pthread_join(t1) (= subsequentEdge).
 */
public class PreferenceOrder {

  // TODO positional preference order ("a <q b") possible cases:
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

  /** The set of CFAEdges that must be executed before {@link PreferenceOrder#subsequentEdge}. */
  public final ImmutableSet<CFAEdge> precedingEdges;

  /** The CFAEdge that is executed once all {@link PreferenceOrder#precedingEdges} are executed. */
  public final CFAEdge subsequentEdge;

  public PreferenceOrder(ImmutableSet<CFAEdge> pPrecedingEdges, CFAEdge pSubsequentEdge) {
    precedingEdges = pPrecedingEdges;
    subsequentEdge = pSubsequentEdge;
  }
}
