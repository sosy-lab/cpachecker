// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * ID of a thread instance in the concurrency analyses: the main thread has ID 0, and the other
 * threads get IDs in the order of their creation (as in the witnesses). It is also the ID of the
 * per-thread CFA clone of the thread (see {@link CFANode#getCloneThreadId()}).
 */
public record ThreadId(int id) implements Comparable<ThreadId> {

  public static final ThreadId MAIN = new ThreadId(0);

  public ThreadId {
    checkArgument(id >= 0, "Negative thread ID %s", id);
  }

  @Override
  public int compareTo(ThreadId pOther) {
    return Integer.compare(id, pOther.id);
  }

  @Override
  public String toString() {
    return Integer.toString(id);
  }
}
