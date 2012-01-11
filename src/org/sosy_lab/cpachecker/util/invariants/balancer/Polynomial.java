/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.sosy_lab.cpachecker.util.invariants.Rational;

public class Polynomial {

  private List<Term> terms = new Vector<Term>();

  public Polynomial() {}

  public Polynomial(List<Term> tlist) {
    terms = new Vector<Term>();
    for (Term t : tlist) {
      if (!t.isZero()) {
        terms.add(t);
      }
    }
  }

  public Polynomial(Term t) {
    terms = new Vector<Term>();
    terms.add(t);
  }

  public Polynomial(int n) {
    makeConstant(new Rational(n,1));
  }

  public Polynomial(Rational r) {
    makeConstant(r);
  }

  public Polynomial(Variable v) {
    Term t = new Term(v);
    terms = new Vector<Term>();
    terms.add(t);
  }

  private void makeConstant(Rational r) {
    Term t = new Term(r);
    terms = new Vector<Term>();
    terms.add(t);
  }

  public boolean equals(Polynomial that) {
    Map<String,Rational> m1 = this.getMonomToCoeffMap();
    Map<String,Rational> m2 = that.getMonomToCoeffMap();
    boolean answer = true;
    for (String s : m1.keySet()) {
      if (!m2.containsKey(s)) {
        answer = false;
        break;
      } else {
        Rational c1 = m1.get(s);
        Rational c2 = m2.get(s);
        if (!c1.equals(c2)) {
          answer = false;
          break;
        }
      }
    }
    return answer;
  }

  public Map<String,Rational> getMonomToCoeffMap() {
    collect();
    Map<String,Rational> map = new HashMap<String,Rational>();
    for (Term t : terms) {
      String m = t.getMonomial().toString(true);
      Rational c = t.getCoeff();
      map.put(m, c);
    }
    return map;
  }

  public static Polynomial add(Polynomial f1, Polynomial f2) {
    List<Term> tlist = new Vector<Term>(f1.terms);
    tlist.addAll(f2.terms);
    tlist = collect(tlist);
    return new Polynomial(tlist);
  }

  public static Polynomial subtract(Polynomial f1, Polynomial f2) {
    List<Term> tlist = new Vector<Term>(f1.terms);
    Polynomial mf2 = Polynomial.makeNegative(f2);
    tlist.addAll(mf2.terms);
    tlist = collect(tlist);
    return new Polynomial(tlist);
  }

  public static Polynomial multiply(Polynomial f1, Polynomial f2) {
    List<Term> tlist = new Vector<Term>();
    for (Term a : f1.terms) {
      for (Term b : f2.terms) {
        Term c = Term.multiply(a, b);
        tlist.add(c);
      }
    }
    tlist = collect(tlist);
    return new Polynomial(tlist);
  }

  public static Polynomial makeNegative(Polynomial f) {
    List<Term> tlist = new Vector<Term>(f.terms.size());
    for (Term t : f.terms) {
      Term u = Term.makeNegative(t);
      tlist.add(u);
    }
    return new Polynomial(tlist);
  }

  public static Polynomial makeZero() {
    return new Polynomial(Term.makeZero());
  }

  public static Polynomial makeUnity() {
    return new Polynomial(Term.makeUnity());
  }

  public void collect() {
    terms = collect(terms);
  }

  /*
   * Collect like terms.
   */
  public static List<Term> collect(List<Term> tlist) {
    // Make a map from monomial strings, written alphabetically,
    // to a Term whose coeff is the cumulative coefficient for that monomial.
    Map<String,Term> gatheredTerms = new HashMap<String,Term>();
    boolean sortAlpha = true;
    for (Term t : tlist) {
      String monom = t.getMonomial().toString(sortAlpha);
      if (gatheredTerms.containsKey(monom)) {
        // We have encountered this monomial before.
        // Get current coefficient.
        Rational current = gatheredTerms.get(monom).getCoeff();
        // Get rational to be added on -- the "augend".
        Rational augend = t.getCoeff();
        // Add.
        Rational sum = current.plus(augend);
        // Set new coefficient.
        gatheredTerms.get(monom).setCoeff(sum);
      } else {
        // This is the first case of this monomial.
        // Create a new Term whose coeff equals that of t.
        Term u = new Term();
        u.setCoeff(t.getCoeff());
        u.setMonomial(t.getMonomial());
        gatheredTerms.put(monom, u);
      }
    }
    // Now create a list of terms based on the map and return it.
    List<Term> collected = new Vector<Term>(gatheredTerms.values());
    return collected;
  }

  public boolean isZero() {
    // If we have no terms, then we are zero.
    if (terms.size() == 0) {
      return true;
    }
    // Otherwise, first make sure like terms are collected.
    terms = collect(terms);
    // Now if we have more than one term, they cannot both be
    // constant, so we cannot be zero.
    if (terms.size() > 1) {
      return false;
    }
    // Otherwise we have exactly one term. Check whether it is zero.
    Term t = terms.get(0);
    return t.isZero();
  }

  public boolean isUnity() {
    // If we have no terms, then we are zero.
    if (terms.size() == 0) {
      return false;
    }
    // Otherwise, first make sure like terms are collected.
    terms = collect(terms);
    // Now if we have more than one term, they cannot both be
    // constant, so we cannot be unity.
    if (terms.size() > 1) {
      return false;
    }
    // Otherwise we have exactly one term. Check whether it is unity.
    Term t = terms.get(0);
    return t.isUnity();
  }

  public boolean isConstant() {
    // If we have no terms, then we are zero, hence constant.
    if (terms.size() == 0) {
      return true;
    }
    // Otherwise, first make sure like terms are collected.
    terms = collect(terms);
    // Now if we have more than one term, they cannot both be constant.
    if (terms.size() > 1) {
      return false;
    }
    // Otherwise we have exactly one term. Check whether it is constant.
    Term t = terms.get(0);
    return t.isConstant();
  }

  public Rational getConstant() {
    Rational r = null;
    if (isConstant()) {
      r = terms.get(0).getCoeff();
    }
    return r;
  }

  public List<Monomial> getMonomials() {
    collect();
    List<Monomial> mlist = new Vector<Monomial>();
    for (Term t : terms) {
      //if (!t.isConstant()) {
      //  mlist.add(t.getMonomial());
      //}
      mlist.add(t.getMonomial());
    }
    return mlist;
  }

  public Monomial getMonomialContent() {
    return Monomial.gcd(getMonomials());
  }

  /*
   * Attempt to divide each monomial by m, returning true iff this succeeds,
   * and altering the terms of this polynomial also iff it succeeds.
   */
  public boolean factorOut(Monomial m) {
    boolean success = true;
    List<Term> results = new Vector<Term>(terms.size());
    for (Term t : terms) {
      if (m.divides(t)) {
        Term u = Term.divide(t,m);
        results.add(u);
      } else {
        success = false;
        break;
      }
    }
    if (success) {
      terms = results;
    }
    return success;
  }

  @Override
  public String toString() {
    // If there are no terms, then just return 0.
    if (terms.size() == 0) {
      return "0";
    }
    // Otherwise write all the terms and add them up.
    String s = "";
    for (Term t : terms) {
      s += " + "+t.toString();
    }
    // There is one more " + " at the beginning than we need.
    s = s.substring(3);
    return s;
  }

  public static Integer gcd(Integer... ma) {
    List<Integer> ml = new Vector<Integer>(ma.length);
    for (Integer m : ma) {
      ml.add(m);
    }
    return gcd(ml);
  }

  public static Integer gcd(List<Integer> mlist) {
    int N = mlist.size();
    // If list is length zero, return the Integer 1, a dummy response.
    if (N == 0) {
      return new Integer(1);
    }
    // If exactly 1, then it is its own gcd.
    else if (N == 1) {
      return mlist.get(0);
    }
    // If more than 2, then "divide and conquer".
    else if (N > 2) {
      int L = N/2;
      Integer a = gcd(mlist.subList(0, L));
      Integer b = gcd(mlist.subList(L, N));
      Integer d = gcd(a,b);
      return d;
    }
    // If exactly 2:
    else {
      Integer a = mlist.get(0);
      Integer b = mlist.get(1);
      return euclideanAlgorithm(a,b);
    }
  }

  public static Integer euclideanAlgorithm(Integer aI, Integer bI) {
    int b = aI.intValue();
    int r = bI.intValue();
    if (b*b < r*r) {
      int t = b; b = r; r = t;
    }
    int a;
    while (r != 0) {
      a = b;
      b = r;
      r = a%b;
    }
    return new Integer(b);
  }

}
