// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.nondeterminism;

public enum NondeterminismSource {
  NEXT_THREAD(false),
  NUM_STATEMENTS(true),
  NEXT_THREAD_AND_NUM_STATEMENTS(true);

  // TODO rename everything threadLoops, the name is not fitting anymore
  private final boolean hasThreadLoops;

  NondeterminismSource(boolean pHasThreadLoops) {
    hasThreadLoops = pHasThreadLoops;
  }

  public boolean hasThreadLoops() {
    return this.hasThreadLoops;
  }
}
