// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

public enum PthreadFunction {
  CREATE("pthread_create");
  // TODO more pthread functions

  public final String name;

  private PthreadFunction(String pName) {
    this.name = pName;
  }
}
