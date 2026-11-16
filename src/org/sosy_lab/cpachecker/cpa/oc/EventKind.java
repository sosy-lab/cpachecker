// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.oc;

/**
 * Kind of an event collected during the ordering-consistency exploration. Atomic blocks are
 * represented as LOCK/UNLOCK of a dedicated pseudo-mutex.
 */
public enum EventKind {
  READ,
  WRITE,
  LOCK,
  UNLOCK,
  CREATE,
  JOIN,
  ERROR,
  /** A call that terminates the whole program (abort/exit). */
  ABORT,
  /** A path cut at the loop bound; the thread has not terminated there. */
  TRUNCATED,
  /**
   * Normal termination of a thread (its start routine returns, or pthread_exit); its guard is the
   * condition under which the thread finishes, and only such paths can be joined.
   */
  THREAD_EXIT,
}
