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

public enum Sign implements Serializable {
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

  private static final ImmutableMap<Integer, Sign> VALUE_MAP;

  static {
    ImmutableMap.Builder<Integer, Sign> builder = ImmutableMap.builder();
    for (Sign s : Sign.values()) {
      builder.put(s.numVal, s);
    }
    VALUE_MAP = builder.buildOrThrow();
  }

  Sign(int numVal) {
    this.numVal = numVal;
  }

  public boolean isAll() {
    return this == ALL;
  }

  public boolean isEmpty() {
    return this == EMPTY;
  }

  public Sign combineWith(Sign sign) {
    // combine bit values
    return VALUE_MAP.get(sign.numVal | numVal);
  }

  public boolean covers(Sign sign) {
    return (sign.numVal | numVal) == numVal;
  }

  public boolean intersects(Sign sign) {
    return (sign.numVal & numVal) != 0;
  }

  public static Sign min(Sign sign0, Sign sign1) {
    if (sign0.isSubsetOf(sign1)) {
      return sign0;
    }
    return sign1;
  }

  public boolean isSubsetOf(Sign sign) {
    if (sign.isAll()) {
      return true;
    }
    // Check if this is a subset using atomic signs
    return sign.split().containsAll(split());
  }

  public ImmutableSet<Sign> split() { // TODO performance
    ImmutableSet.Builder<Sign> builder = ImmutableSet.builder();
    for (Sign s : ImmutableList.of(PLUS, MINUS, ZERO)) {
      if ((s.numVal & numVal) != 0) {
        builder.add(s);
      }
    }
    return builder.build();
  }
}
