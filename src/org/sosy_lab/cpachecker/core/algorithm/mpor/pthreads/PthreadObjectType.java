// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads;

import org.sosy_lab.cpachecker.cfa.types.c.CType;

public enum PthreadObjectType {
  PTHREAD_BARRIER_T("pthread_barrier_t"),
  PTHREAD_COND_INITIALIZER("PTHREAD_COND_INITIALIZER"),
  PTHREAD_COND_T("pthread_cond_t"),
  PTHREAD_KEY_T("pthread_key_t"),
  PTHREAD_MUTEX_INITIALIZER("PTHREAD_MUTEX_INITIALIZER"),
  PTHREAD_MUTEX_T("pthread_mutex_t"),
  PTHREAD_ONCE_T("pthread_once_t"),
  PTHREAD_RWLOCK_T("pthread_rwlock_t"),
  PTHREAD_T("pthread_t"),
  RETURN_VALUE(""),
  START_ROUTINE(""),
  START_ROUTINE_ARGUMENT("");

  public final String name;

  PthreadObjectType(String pName) {
    name = pName;
  }

  public boolean equalsType(CType pType) {
    // there seems no better way than comparing by string, unfortunately
    return this.name.equals(pType.toASTString("").strip());
  }
}
