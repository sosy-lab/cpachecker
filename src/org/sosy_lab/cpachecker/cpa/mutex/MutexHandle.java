// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.mutex;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The identity of a mutex, as the canonical key of the lvalue that denotes it. Several syntactic
 * forms denote the same mutex ({@code m}, {@code &m}, {@code (pthread_mutex_t *)&m}), so the key is
 * the canonicalized form computed by {@link MutexFunctions#extractMutexName}, not the expression
 * itself. This wrapper exists so that such a key cannot be confused with an ordinary variable name.
 */
public record MutexHandle(String key) {
  public MutexHandle {
    checkNotNull(key);
  }

  @Override
  public String toString() {
    return key;
  }
}
