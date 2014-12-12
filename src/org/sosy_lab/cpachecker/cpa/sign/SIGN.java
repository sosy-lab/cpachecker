/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.sign;

import java.io.Serializable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;

public enum SIGN implements Serializable{
  // ALL = 111, PLUS = 100, MINUS = 010, ...
  EMPTY(0), PLUS(1), MINUS(2), ZERO(4), PLUSMINUS(3), PLUS0(5), MINUS0(6), ALL(7);

  private int numVal;

  private static final ImmutableMap<Integer, SIGN> VALUE_MAP;

  static {
    Builder<Integer, SIGN> builder = ImmutableMap.builder();
    for (SIGN s : SIGN.values()) {
      builder.put(s.numVal, s);
    }
    VALUE_MAP = builder.build();
  }

  private SIGN(int numVal) {
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
    return VALUE_MAP.get(Integer.valueOf(sign.numVal | numVal));
  }

  public boolean covers(SIGN sign) {
    if ((sign.numVal | this.numVal)  == this.numVal) { return true; }
    return false;
  }

  public boolean intersects(SIGN sign) {
    if ((sign.numVal & this.numVal) != 0) { return true; }
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
    return sign.split().containsAll(this.split());
  }

  public ImmutableSet<SIGN> split() { // TODO performance
    ImmutableSet.Builder<SIGN> builder = ImmutableSet.builder();
    for (SIGN s : ImmutableList.of(PLUS,MINUS,ZERO)) {
      if ((s.numVal & numVal) > 0) {
        builder.add(s);
      }
    }
    return builder.build();
  }
}