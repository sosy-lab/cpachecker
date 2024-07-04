// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

public enum FunctionType {
  // TODO decide which of these are actually relevant for us
  // TODO create barrier logic, see e.g. pthread-divine/barrier_2t.i
  BARRIER_INIT("pthread_barrier_init"),
  BARRIER_WAIT("pthread_barrier_wait"),
  CANCEL("pthread_cancel"),
  DETACH("pthread_detach"),
  EXIT("pthread_exit"),
  PTHREAD_CREATE("pthread_create"),
  PTHREAD_JOIN("pthread_join"),
  PTHREAD_MUTEX_LOCK("pthread_mutex_lock"),
  PTHREAD_MUTEX_UNLOCK("pthread_mutex_unlock");
  // TODO unsure about yield, mutex_destroy
  //  pthread_mutex_t amutex = PTHREAD_MUTEX_INITIALIZER; // also used instead of mutex init
  //  pthread_barrier stuff
  //  etc. probably a lot more things
  //  __VERIFIER_atomic_begin and _end will be relevant to identify too

  public final String name;

  FunctionType(String pName) {
    this.name = pName;
  }
}
