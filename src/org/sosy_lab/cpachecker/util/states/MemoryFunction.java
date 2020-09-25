// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import java.util.HashMap;
import java.util.Map;

public enum SLMemoryFunction {

  MALLOC("malloc"),
  LDVMALLOC("ldv_malloc"),
  REALLOC("realloc"),
  CALLOC("calloc"),
  FREE("free"),
  ALLOCA("alloca"),
  NONE("none");

  private final String fctName;

  SLMemoryFunction(String pFctName) {
    fctName = pFctName;
  }

  public String getFctName() {
    return fctName;
  }

  private static final Map<String, SLMemoryFunction> lookup = new HashMap<>();
  static {
    for (SLMemoryFunction fct : SLMemoryFunction.values()) {
      lookup.put(fct.getFctName(), fct);
    }
  }

  public static SLMemoryFunction get(String pName) {
    return lookup.containsKey(pName) ? lookup.get(pName) : NONE;
  }
}
