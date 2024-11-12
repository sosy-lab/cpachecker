// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads;

public enum PthreadObjectType {
  PTHREAD_T("pthread_t"),
  PTHREAD_MUTEX_T("pthread_mutex_t");

  public final String name;

  PthreadObjectType(String pName) {
    name = pName;
  }
}
