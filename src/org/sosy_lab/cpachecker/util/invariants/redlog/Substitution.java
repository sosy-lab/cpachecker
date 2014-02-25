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
package org.sosy_lab.cpachecker.util.invariants.redlog;

import java.util.HashMap;

import org.sosy_lab.cpachecker.util.invariants.Rational;

public class Substitution {

  private HashMap<String, Rational> map;

  public Substitution() {}

  public Substitution(String[] vars, int[] values) {
    map = new HashMap<>();

    int n = (vars.length < values.length ? vars.length : values.length);
    String v;
    int a;
    Rational r = null;
    for (int i = 0; i < n; i++) {
      v = vars[i];
      a = values[i];
      try {
        r = new Rational(a, 1);
      } catch (Exception e) {}
      map.put(v, r);
    }
  }

  public Rational get(String v) {
    Rational r = null;
    try {
      r = map.get(v);
      if (r==null) {
        throw new UndefinedSubstitutionException();
      }
    } catch (Exception e) {
    }
    return r;
  }

  public Rational getDummy(String v) {
    // Return the value plugged in for variable v.
    // For now, just a black box.
    // If there is no value for v, we should throw an exception.
    Rational r = null;
    try {
      r = new Rational(1, 1);
    } catch (Exception e) {
    }
    return r;
  }

}
