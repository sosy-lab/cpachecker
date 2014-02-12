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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.sosy_lab.cpachecker.util.invariants.Rational;

public class Polynomial {

  private List<Term> terms = new Vector<>();

  public Polynomial() {}

  public Polynomial(List<Term> tlist) {
    terms = new Vector<>();
    for (Term t : tlist) {
      if (!t.isZero()) {
        terms.add(t);
      }
    }
  }

  public Polynomial(Term t) {
    terms = new Vector<>();
    terms.add(t);
  }

  public Polynomial(int n) {
    makeConstant(new Rational(n, 1));
  }

  public Polynomial(Rational r) {
    makeConstant(r);
  }

  public Polynomial(Variable v) {
    Term t = new Term(v);
    terms = new Vector<>();
    terms.add(t);
  }

  public Polynomial copy() {
    List<Term> ts = new Vector<>(terms.size());
    for (Term t : terms) {
      ts.add(t.copy());
    }
    return new Polynomial(ts);
  }

  private void makeConstant(Rational r) {
    Term t = new Term(r);
    terms = new Vector<>();
    terms.add(t);
  }

  public boolean equals(Polynomial that) {
    Map<String, Rational> m1 = this.getMonomToCoeffMap();
    Map<String, Rational> m2 = that.getMonomToCoeffMap();
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

  public Map<String, Rational> getMonomToCoeffMap() {
    collect();
    Map<String, Rational> map = new HashMap<>();
    for (Term t : terms) {
      String m = t.getMonomial().toString(true);
      Rational c = t.getCoeff();
      map.put(m, c);
    }
    return map;
  }

  public static Polynomial add(Polynomial f1, Polynomial f2) {
    List<Term> tlist = new Vector<>(f1.terms);
    tlist.addAll(f2.terms);
    tlist = collect(tlist);
    return new Polynomial(tlist);
  }

  public static Polynomial subtract(Polynomial f1, Polynomial f2) {
    List<Term> tlist = new Vector<>(f1.terms);
    Polynomial mf2 = Polynomial.makeNegative(f2);
    tlist.addAll(mf2.terms);
    tlist = collect(tlist);
    return new Polynomial(tlist);
  }

  public static Polynomial multiply(Polynomial f1, Polynomial f2) {
    List<Term> tlist = new Vector<>();
    for (Term a : f1.terms) {
      for (Term b : f2.terms) {
        Term c = Term.multiply(a, b);
        tlist.add(c);
      }
    }
    tlist = collect(tlist);
    return new Polynomial(tlist);
  }

  public static Polynomial power(Polynomial f, int e) {
    if (e < 0) {
      return null;
    } else if (e == 0) {
      return Polynomial.makeUnity();
    } else if (e == 1) {
      return f.copy();
    } else if (e <= 100) {
      Polynomial g = Polynomial.power(f, e-1);
      return Polynomial.multiply(f, g);
    } else {
      System.err.println("Tried to raise polynomial to power higher than 100.");
      return null;
    }
  }

  public static Polynomial makeNegative(Polynomial f) {
    List<Term> tlist = new Vector<>(f.terms.size());
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

  /*
   * Apply the substitution to the polynomial, replacing all occurrences of the substituted variable
   * by the polynomial substituted for it. Do not alter the passed polynomial; create a new one.
   */
  public static Polynomial applySubstitution(Substitution subs, Polynomial f) {
    Variable v = subs.getVar();
    Polynomial g = subs.getRHS();
    Polynomial h = Polynomial.makeZero();
    // We will substitute g for v in f, creating h.

    // For each term t in f, consider the power e to which v occurs in t.
    Polynomial p, q;
    for (Term t : f.terms) {
      int e = t.getPower(v);
      if (e == 0) {
        // If v does not occur in t, then simply add t to h.
        q = new Polynomial(t.copy());
        h = Polynomial.add(h, q);
      } else {
        // Else we must eliminate v from t, and then multiply t by g to the e power.
        // Eliminate v.
        t = t.copy();
        t.setPower(v, 0);
        q = new Polynomial(t);
        // Raise g to a higher power if necessary.
        if (e == 1) {
          p = g;
        } else {
          p = Polynomial.power(g, e);
        }
        // Multiply, and add the result to h.
        p = Polynomial.multiply(p, q);
        h = Polynomial.add(h, p);
      }
    }
    // Collect h, and return.
    h.collect();
    return h;
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
    Map<String, Term> gatheredTerms = new HashMap<>();
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
    List<Term> collected = new Vector<>(gatheredTerms.values());
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

  public boolean isPositive() {
    // If we have no terms, then we are zero.
    if (terms.size() == 0) {
      return false;
    }
    // Otherwise, first make sure like terms are collected.
    terms = collect(terms);
    // Now if we have more than one term, they cannot both be
    // constant, so we cannot be a positive constant.
    if (terms.size() > 1) {
      return false;
    }
    // Otherwise we have exactly one term. Check whether it is a positive constant.
    Term t = terms.get(0);
    return t.isPositive();
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
    List<Monomial> mlist = new Vector<>();
    for (Term t : terms) {
      //if (!t.isConstant()) {
      //  mlist.add(t.getMonomial());
      //}
      mlist.add(t.getMonomial());
    }
    return mlist;
  }

  /*
   * Compute the max of the total degrees of the terms.
   */
  public int getDegree() {
    collect();
    int d = 0, e;
    for (Term t : terms) {
      e = t.getDegree();
      if (e > d) {
        d = e;
      }
    }
    return d;
  }

  public int getNumTerms() {
    collect();
    return terms.size();
  }

  public Monomial getMonomialContent() {
    return Monomial.gcd(getMonomials());
  }

  /*
   * If this polynomial is 0, return 0;
   * else if all coefficients are negative, return -1;
   * else return 1.
   */
  public int getUnitContent() {
    if (isZero()) {
      return 0;
    } else {
      int u = -1;
      for (Term t : terms) {
        if (t.getCoeff().isPositive()) {
          u = 1; break;
        }
      }
      return u;
    }
  }

  /*
   * Let an ... a0 be the coefficients of the polynomial.
   * Let ai = pi/qi.
   * Let l = lcm(qi).
   * Let g = gcd(l*ai).
   * Let bi = l*ai/g.
   * Then polynomial equals g/l * (bn ... b0).
   * We return g/l.
   * Under this definition, it follows that if you multiply a polynomial by a rational number m,
   * you multiply its rational content by m too.
   * Therefore suppose f and g are polynomials, m a rational number, and we have g = m*f.
   * Let c be the rational content of f. Then the rational content of g is m*c.
   * Let h be f divided by c. Then g/mc = f/c = h.
   * Therefore if one polynomial is a rational multiple of another, you can detect this fact by
   * dividing each one by its own rational content, and checking whether the results are either
   * equal, or are additive inverses.
   */
  public Rational getRationalContent() {
    // Get the coefficients.
    List<Rational> coeffs = getCoeffs();
    // Get their denominators.
    List<Integer> denoms = new Vector<>(coeffs.size());
    for (Rational r : coeffs) {
      denoms.add(r.getDenominator());
    }
    // Get the least common multiple of the denominators.
    int d = gcd(denoms);
    int p = intProduct(denoms);
    int l = p/d;
    // Get the integer content of (l times this polynomial).
    Polynomial lTimesThis = multiply(new Polynomial(l), this);
    int g = lTimesThis.getIntegerContent();
    // Return the rational content g/l.
    Rational c = new Rational(g, l);
    return c;
  }

  /*
   * Return a new polynomial; namely, the one you get by dividing the present polynomial
   * by its own rational content.
   * Does NOT alter the present polynomial.
   */
  public Polynomial cancelRationalContent() {
    Rational c = getRationalContent();
    if (c.isZero()) {
      return copy();
    }
    Rational r = c.makeReciprocal();
    Polynomial p = multiply(new Polynomial(r), this);
    return p;
  }

  /*
   * If this is a rational constant multiple of that, then return this rational constant.
   * Else return null.
   */
  public Rational rationalConstantQuotientOver(Polynomial that) {
    Rational a = this.getRationalContent();
    Rational b = that.getRationalContent();
    Polynomial p = this.cancelRationalContent();
    Polynomial q = that.cancelRationalContent();
    if (subtract(p, q).isZero()) {
      return a.div(b);
    } else if (add(p, q).isZero()) {
      return a.div(b).makeNegative();
    } else {
      return null;
    }
  }

  public boolean isRationalConstantMultipleOf(Polynomial that) {
    Polynomial p = this.cancelRationalContent();
    Polynomial q = that.cancelRationalContent();
    if (subtract(p, q).isZero()) {
      return true;
    } else if (add(p, q).isZero()) {
      return true;
    } else {
      return false;
    }
  }

  public List<Rational> getCoeffs() {
    collect();
    List<Rational> coeffs = new Vector<>(terms.size());
    for (Term t : terms) {
      coeffs.add(t.getCoeff());
    }
    return coeffs;
  }

  /*
   * If all coefficients are integral, return their positive gcd.
   * Else return 1.
   */
  public Integer getIntegerContent() {
    collect();
    List<Integer> coeffs = new Vector<>(terms.size());
    // Build the list of integer coefficients, or quit immediately if one of the
    // coeffs is not integral.
    // Discard 0's.
    Rational c;
    for (Term t : terms) {
      c = t.getCoeff();
      if (c.isIntegral()) {
        if (!c.isZero()) {
          coeffs.add(t.getCoeff().makeInteger());
        }
      } else {
        return Integer.valueOf(1);
      }
    }
    // If all the coeffs were 0, then return 1.
    if (coeffs.size() == 0) {
      return Integer.valueOf(1);
    }
    // Replace each coeff by its absolute value.
    List<Integer> absCoeffs = new Vector<>(coeffs.size());
    int n;
    for (Integer nI : coeffs) {
      n = nI.intValue();
      if (n < 0) {
        n = (-1)*n;
      }
      absCoeffs.add(Integer.valueOf(n));
    }
    // Now get the gcd.
    Integer d = gcd(absCoeffs);
    return d;
  }

  /*
   * Attempt to divide each monomial by m, returning true iff this succeeds,
   * and altering the terms of this polynomial also iff it succeeds.
   */
  public boolean factorOut(Monomial m) {
    boolean success = true;
    List<Term> results = new Vector<>(terms.size());
    for (Term t : terms) {
      if (m.divides(t)) {
        Term u = Term.divide(t, m);
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

  public static Integer intProduct(List<Integer> list) {
    int p = 1;
    for (Integer f : list) {
      p *= f;
    }
    return Integer.valueOf(p);
  }

  public static Integer gcd(Integer... ma) {
    List<Integer> ml = new Vector<>(ma.length);
    for (Integer m : ma) {
      ml.add(m);
    }
    return gcd(ml);
  }

  public static Integer gcd(List<Integer> mlist) {
    int N = mlist.size();
    // If list is length zero, return the Integer 1, a dummy response.
    if (N == 0) {
      return Integer.valueOf(1);
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
      Integer d = gcd(a, b);
      return d;
    }
    // If exactly 2:
    else {
      Integer a = mlist.get(0);
      Integer b = mlist.get(1);
      return euclideanAlgorithm(a, b);
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
    return Integer.valueOf(b);
  }

  /*
   * Like linearIsolateAll, but only isolates the first linear term.
   */
  public Substitution linearIsolateFirst() {
    // First make sure like terms are collected.
    collect();
    // Now get a copy of the first linear term, and copies of all other terms.
    Term lin = null;
    List<Term> others = new Vector<>(terms.size() - 1);
    for (Term t : terms) {
      if (lin == null && t.isLinear()) {
        lin = t.copy();
      } else {
        others.add(t.copy());
      }
    }
    // If we didn't find one, then quit.
    if (lin == null) {
      return null;
    }
    // In this case we found a linear term.
    // First form the polynomial containing all terms other than lin.
    Polynomial rhs = new Polynomial(others);
    // Now multiply it by the negative reciprocal of lin's coeff.
    Rational c = lin.getCoeff().makeNegative().makeReciprocal();
    rhs = Polynomial.multiply(new Polynomial(c), rhs);
    // Grab lin's variable.
    Variable v = lin.getLinearVariable();
    // Return the result.
    Substitution s = new Substitution(v, rhs);
    return s;
  }

}
