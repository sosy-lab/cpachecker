// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.concurrent;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Random;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

/** Interface for a partial order reduction algorithm that guides the state space exploration. */
public interface PartialOrderReductionStrategy {

  ConcurrentState getDynamicThreadState(
      AbstractState pWrappedState,
      CFA pCfa,
      LogManager pLogger,
      ImmutableMap<Integer, ThreadState> pThreads,
      ImmutableSet<Integer> pLivePids,
      ImmutableMap<String, Integer> pHandleHints,
      Random pRandom);

  default ConcurrentState emptyState(
      AbstractState pWrappedInitialState, CFA pCfa, LogManager pLogger, Random pRandom) {
    return getDynamicThreadState(
        pWrappedInitialState,
        pCfa,
        pLogger,
        ImmutableMap.of(),
        ImmutableSet.of(),
        ImmutableMap.of(),
        pRandom);
  }
}
