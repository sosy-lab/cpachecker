// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.type;

import static com.google.common.truth.Truth.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class SymbolicValueFactoryTest {

  private static final int THREADS = 4;
  private static final int IDS_PER_THREAD = 100_000;

  /** Analyses that run in parallel share the factory (duplicate ids crashed SMG2). */
  @Test
  public void testUniqueIdsWithParallelThreads() throws InterruptedException {
    long[][] ids = new long[THREADS][IDS_PER_THREAD];
    Thread[] threads = new Thread[THREADS];
    for (int t = 0; t < THREADS; t++) {
      long[] idsOfThread = ids[t];
      threads[t] =
          Thread.ofPlatform()
              .unstarted(
                  () -> {
                    for (int i = 0; i < IDS_PER_THREAD; i++) {
                      idsOfThread[i] =
                          SymbolicValueFactory.getInstance().newIdentifier(null).getId();
                    }
                  });
      threads[t].start();
    }
    for (Thread thread : threads) {
      thread.join();
    }

    Set<Long> distinctIds = new HashSet<>();
    for (long[] idsOfThread : ids) {
      for (long id : idsOfThread) {
        distinctIds.add(id);
      }
    }
    assertThat(distinctIds).hasSize(THREADS * IDS_PER_THREAD);
  }
}
