// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * CPA that tracks the state of mutexes in concurrent C programs, supporting both POSIX pthread
 * mutexes and C11 threading mutexes. Communicates with the POR CPA via the {@code strengthen}
 * operator to learn which thread (PID) holds each lock.
 */
package org.sosy_lab.cpachecker.cpa.mutex;
