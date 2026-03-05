// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * CPA for tracking the state of mutexes in concurrent C programs. Supports both POSIX pthread
 * mutexes ({@code pthread_mutex_lock}, {@code pthread_mutex_unlock}, etc.) and C11 threading
 * mutexes ({@code mtx_lock}, {@code mtx_unlock}, etc.).
 */
package org.sosy_lab.cpachecker.cpa.mutex;
