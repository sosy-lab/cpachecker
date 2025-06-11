// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.nondeterminism;

public enum NondeterminismSource {
  NEXT_THREAD(false, true),
  NUM_STATEMENTS(true, false),
  NEXT_THREAD_AND_NUM_STATEMENTS(true, true);

  // TODO rename everything threadLoops, the name is not fitting anymore
  private final boolean hasThreadLoops;

  private final boolean hasNextThread;

  NondeterminismSource(boolean pHasThreadLoops, boolean pHasNextThread) {
    hasThreadLoops = pHasThreadLoops;
    hasNextThread = pHasNextThread;
  }

  public boolean hasThreadLoops() {
    return hasThreadLoops;
  }

  public boolean hasNextThread() {
    return hasNextThread;
  }
}
