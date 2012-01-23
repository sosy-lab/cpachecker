/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.invariants.balancer;

import java.util.Collection;
import java.util.Vector;

import org.sosy_lab.cpachecker.util.invariants.balancer.Assumption.AssumptionType;


public class AssumptionSet {

  private final Vector<Assumption> aset;;

  public AssumptionSet() {
    aset = new Vector<Assumption>();
  }

  public AssumptionSet(Collection<Assumption> ca) {
    aset = new Vector<Assumption>();
    for (Assumption a : ca) {
      add(a);
    }
  }

  public int size() {
    return aset.size();
  }

  @Override
  public String toString() {
    return aset.toString();
  }

  /*
   * Add the assumption to the set, first checking whether it strengthens any existing one.
   *
   * The check for strengthening results in this collection acting like a set, and more.
   * For one thing, a duplicate will never be added. E.g. if b = (p1 < 0) is present and we
   * attempt to add a = (p1 < 0), then we will find that a "strengthens" b (actually leaving it
   * unchanged), and so we will add this common strengthening in place of b and return. This
   * results in no actual change to our set of assumptions.
   *
   * Moreover, if we try to add an a strictly weaker than something present, e.g. if again
   * b = (p1 < 0) is present and we attempt to add a = (p1 <= 0), then the result of strengthening
   * b by a will be simply b again, and this will be kept while a is discarded.
   *
   * As examination of the method Assumption.strengthen reveals, the upshot is that an
   * AssumptionSet will never have more than one Assumption on a given RationalFunction or its
   * additive inverse.
   *
   * Returns false if the assumption "false" got added; returns true otherwise.
   * In other words, the boolean return value of this method can be thought of as roughly the
   * truth value of the statement, P = "The set is still consistent." In fact, a return value of
   * false definitely means that P is false, whereas a return value of true does not guarantee
   * that P be true, since algebraic relations are not investigated. For example, true will be
   * returned even on the set { p2 + -p1 > 0, p1 > 0, p2 < 0 }. Thus, the return value is only meant
   * to serve as an "early warning" that the set contains a contradiction.
   */
  public boolean add(Assumption a) {
    Assumption b,c;
    for (int i = 0; i < aset.size(); i++) {
      b = aset.get(i);
      c = a.strengthen(b);
      if (c != null) {
        aset.set(i,c);
        return c.getAssumptionType() != AssumptionType.FALSE;
      }
    }
    aset.add(a);
    return a.getAssumptionType() != AssumptionType.FALSE;
  }

  public void addAll(Collection<Assumption> ca) {
    for (Assumption a : ca) {
      add(a);
    }
  }

}
