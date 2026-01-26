// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction;

public enum ReductionOrder {
  NONE,
  CONFLICT_THEN_LAST_THREAD,
  LAST_THREAD_THEN_CONFLICT;

  public boolean isEnabled() {
    return !this.equals(NONE);
  }
}
