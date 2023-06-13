// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.sign;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;

public enum SIGN implements Serializable {
  // ALL = 111, PLUS = 100, MINUS = 010, ...
  EMPTY(0),
  PLUS(1),
  MINUS(2),
  ZERO(4),
  PLUSMINUS(3),
  PLUS0(5),
  MINUS0(6),
  ALL(7);

  private final int numVal;

  private static final ImmutableMap<Integer, SIGN> VALUE_MAP;

  static {
    ImmutableMap.Builder<Integer, SIGN> builder = ImmutableMap.builder();
    for (SIGN s : SIGN.values()) {
      builder.put(s.numVal, s);
    }
    VALUE_MAP = builder.buildOrThrow();
  }

  SIGN(int numVal) {
    this.numVal = numVal;
  }

  public boolean isAll() {
    return this == ALL;
  }

  public boolean isEmpty() {
    return this == EMPTY;
  }

  public SIGN combineWith(SIGN sign) {
    // combine bit values
    return VALUE_MAP.get(sign.numVal | numVal);
  }

  public boolean covers(SIGN sign) {
    if ((sign.numVal | numVal) == numVal) {
      return true;
    }
    return false;
  }

  public boolean intersects(SIGN sign) {
    if ((sign.numVal & numVal) != 0) {
      return true;
    }
    return false;
  }

  public static SIGN min(SIGN sign0, SIGN sign1) {
    if (sign0.isSubsetOf(sign1)) {
      return sign0;
    }
    return sign1;
  }

  public boolean isSubsetOf(SIGN sign) {
    if (sign.isAll()) {
      return true;
    }
    // Check if this is a subset using atomic signs
    return sign.split().containsAll(split());
  }

  public ImmutableSet<SIGN> split() { // TODO performance
    ImmutableSet.Builder<SIGN> builder = ImmutableSet.builder();
    for (SIGN s : ImmutableList.of(PLUS, MINUS, ZERO)) {
      if ((s.numVal & numVal) > 0) {
        builder.add(s);
      }
    }
    return builder.build();
  }
}
