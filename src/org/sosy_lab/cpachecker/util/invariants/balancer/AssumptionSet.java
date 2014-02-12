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
package org.sosy_lab.cpachecker.util.invariants.balancer;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import org.sosy_lab.cpachecker.util.invariants.balancer.Assumption.AssumptionRelation;
import org.sosy_lab.cpachecker.util.invariants.balancer.Assumption.AssumptionType;


public class AssumptionSet implements Iterable<Assumption> {

  private Vector<Assumption> aset;

  public AssumptionSet() {
    aset = new Vector<>();
  }

  public AssumptionSet(Collection<Assumption> ca) {
    aset = new Vector<>();
    for (Assumption a : ca) {
      add(a);
    }
  }

  public AssumptionSet(AssumptionSet as) {
    aset = new Vector<>();
    for (Assumption a : as) {
      // We can use the vector method, since 'as' should already contain no obvious logical redundancies.
      aset.add(a);
    }
  }

  public AssumptionSet copy() {
    Vector<Assumption> v = new Vector<>(aset.size());
    for (Assumption a : aset) {
      v.add(a);
    }
    AssumptionSet as = new AssumptionSet();
    as.aset = v;
    return as;
  }

  @Override
  public Iterator<Assumption> iterator() {
    return aset.iterator();
  }

  public int size() {
    return aset.size();
  }

  @Override
  public String toString() {
    return aset.toString();
  }

  /*
   * Write the conjunction of all the assumptions, and existentially quantify all the parameters,
   * using Redlog syntax.
   */
  public String writeQEformula() {
    String phi = "";
    for (Assumption a : aset) {
      phi += " and "+a.toString();
    }
    if (phi.length() > 0) {
      phi = phi.substring(5);
    }
    phi = "rlex("+phi+")";
    return phi;
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
   * truth value of the statement P = "The set is not obviously self-contradictory."
   * In fact, a return value of false definitely means that P is false, whereas a return
   * value of true does not guarantee that P be true, since algebraic relations are not investigated.
   * For example, true will be returned even on the set { p2 + -p1 > 0, p1 > 0, p2 < 0 }. Thus, the
   * return value is only meant to serve as an "early warning" that the set contains a contradiction.
   */
  public boolean add(Assumption a) {
    Assumption b, c;
    for (int i = 0; i < aset.size(); i++) {
      b = aset.get(i);
      c = a.strengthen(b);
      if (c != null) {
        aset.set(i, c);
        return c.getAssumptionType() != AssumptionType.FALSE;
      }
    }
    aset.add(a);
    return a.getAssumptionType() != AssumptionType.FALSE;
  }

  /*
   * Diagnostic purposes.
   */
  public boolean add(Assumption a, boolean writeArgs) {
    Assumption b, c;
    for (int i = 0; i < aset.size(); i++) {
      b = aset.get(i);
      c = a.strengthen(b);
    }
    boolean bl = add(a);
    return bl;
  }

  /*
   * Return value is the conjunction of the return values of the individual add commands.
   * Thus, if /any/ add resulted in 'false' (meaning the set became contradictory), then
   * we will return false.
   */
  public boolean addAll(Collection<Assumption> ca) {
    boolean result = true;
    for (Assumption a : ca) {
      result = result & add(a);
    }
    return result;
  }

  public boolean addAll(AssumptionSet a) {
    return addAll(a.aset);
  }

  /*
   * Return the union of this set with the passed one, but do not modify
   * either this set or the passed one.
   */
  public AssumptionSet union(AssumptionSet a) {
    AssumptionSet b = this.copy();
    b.addAll(a);
    return b;
  }

  public boolean contains(Assumption a) {
    return aset.contains(a);
  }

  public boolean isSubsetOf(AssumptionSet that) {
    boolean subset = true;
    for (Assumption a : aset) {
      if (!that.contains(a)) {
        subset = false;
        break;
      }
    }
    return subset;
  }

  public boolean equals(AssumptionSet that) {
    return this.isSubsetOf(that) && that.isSubsetOf(this);
  }

  /*
   * We match the passed assumption a against all those in the set,
   * returning the /strongest/ relation we find.
   *
   * We can find: C, I, W, R, S, D.
   *
   * If there's a C, we return that.
   * If there's no C, but there is an I, then we return that.
   * If there's neither C nor I, but there is a W, R, or S, then we return that.
   * Else, there were only D, and we return D.
   *
   * Note that if the current set is neither redundant (containing b and c such that
   * b W c or b S c) nor immediately contradictory (containing b and c such that b C c),
   * then we will never get more than one of W, R, and S.
   *
   * For if we get both a W b and a S c, then c W b, and the set is redundant.
   * If we get both a R b and a W c, then either b W c or b C c.
   * If we get both a R b and a S c, then c W b.
   *
   */
  public AssumptionRelation matchAgainst(Assumption a) {
    AssumptionRelation max = AssumptionRelation.DOESNOTCOMPARETO;
    for (Assumption b : aset) {
      AssumptionRelation rel = a.matchAgainst(b);
      if (rel.getNum() > max.getNum()) {
        max = rel;
      }
    }
    return max;
  }

  /*
   * Say the strongest assumption we have on f, if any.
   * Returns 'true' if we have no assumptions about f.
   */
  public AssumptionType query(RationalFunction f) {
    AssumptionType at = AssumptionType.TRUE;
    Assumption a = new Assumption(f, at);
    for (Assumption b : aset) {
      Assumption c = b.strengthen(a);
      if (c != null) {
        RationalFunction g = b.getRationalFunction();
        // Since b strengthens a, g must be a constant multiple of f.
        // If that multiple is negative, then we must flip the assumption type.
        if (g.isZero()) {
          // This should happen only if f was zero.
          return AssumptionType.ZERO;
        }
        RationalFunction q = RationalFunction.divide(f, g);
        if (!q.isConstant()) {
          // This should not happen. Just in case,
          return AssumptionType.TRUE;
        }
        at = b.getAssumptionType();
        if (!q.isPositive()) {
          // If q is negative, then flip the assumption type.
          at = at.flip();
        }
      }
    }
    return at;
  }

}
