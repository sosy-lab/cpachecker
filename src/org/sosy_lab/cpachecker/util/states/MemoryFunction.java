// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.util.states;

import java.util.HashMap;
import java.util.Map;

public enum MemoryFunction {

  MALLOC("malloc"),
  LDVMALLOC("ldv_malloc"),
  REALLOC("realloc"),
  CALLOC("calloc"),
  FREE("free"),
  ALLOCA("alloca"),
  NONE("none");

  private final String fctName;

  MemoryFunction(String pFctName) {
    fctName = pFctName;
  }

  public String getFctName() {
    return fctName;
  }

  private static final Map<String, MemoryFunction> lookup = new HashMap<>();
  static {
    for (MemoryFunction fct : MemoryFunction.values()) {
      lookup.put(fct.getFctName(), fct);
    }
  }

  public static MemoryFunction get(String pName) {
    return lookup.containsKey(pName) ? lookup.get(pName) : NONE;
  }
}
