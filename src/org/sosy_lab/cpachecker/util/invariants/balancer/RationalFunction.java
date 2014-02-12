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

import org.sosy_lab.cpachecker.util.invariants.Rational;

public class RationalFunction {

  private Polynomial num;
  private Polynomial denom;

  public RationalFunction(Polynomial n, Polynomial d) {
    num = n;
    denom = d;
    simplify();
  }

  public RationalFunction(Polynomial n) {
    num = n;
    denom = new Polynomial(1);
  }

  public RationalFunction(int n) {
    makeConstant(new Rational(n, 1));
  }

  public RationalFunction(String x) {
    Variable v = new Variable(x);
    num = new Polynomial(v);
    denom = new Polynomial(1);
  }

  public RationalFunction(Variable v) {
    num = new Polynomial(v);
    denom = new Polynomial(1);
  }

  public RationalFunction(Rational r) {
    makeConstant(r);
  }

  public RationalFunction copy() {
    return new RationalFunction(num.copy(), denom.copy());
  }

  public Polynomial getNumerator() {
    return num;
  }

  public Polynomial getDenominator() {
    return denom;
  }

  public static RationalFunction buildVar(String x) {
    Variable v = new Variable(x);
    RationalFunction f = new RationalFunction(v);
    return f;
  }

  private void makeConstant(Rational r) {
    num = new Polynomial(r);
    denom = new Polynomial(1);
  }

  public static RationalFunction makeZero() {
    return new RationalFunction(Polynomial.makeZero(), Polynomial.makeUnity());
  }

  public static RationalFunction makeUnity() {
    return new RationalFunction(Polynomial.makeUnity(), Polynomial.makeUnity());
  }

  public static RationalFunction add(RationalFunction f1, RationalFunction f2) {
    Polynomial n1 = f1.num;
    Polynomial d1 = f1.denom;
    Polynomial n2 = f2.num;
    Polynomial d2 = f2.denom;
    // So we want to compute a/b = n1/d1 + n2/d2.
    // Compute numerator a = n1*d2 + n2*d1.
    Polynomial n1d2 = Polynomial.multiply(n1, d2);
    Polynomial n2d1 = Polynomial.multiply(n2, d1);
    Polynomial a = Polynomial.add(n1d2, n2d1);
    // Compute denominator b = d1*d2.
    Polynomial b = Polynomial.multiply(d1, d2);
    RationalFunction f = new RationalFunction(a, b);
    f.simplify();
    return f;
  }

  public static RationalFunction subtract(RationalFunction f1, RationalFunction f2) {
    Polynomial n1 = f1.num;
    Polynomial d1 = f1.denom;
    Polynomial n2 = f2.num;
    Polynomial d2 = f2.denom;
    // So we want to compute a/b = n1/d1 - n2/d2.
    // Compute numerator a = n1*d2 - n2*d1.
    Polynomial n1d2 = Polynomial.multiply(n1, d2);
    Polynomial n2d1 = Polynomial.multiply(n2, d1);
    Polynomial a = Polynomial.subtract(n1d2, n2d1);
    // Compute denominator b = d1*d2.
    Polynomial b = Polynomial.multiply(d1, d2);
    RationalFunction f = new RationalFunction(a, b);
    f.simplify();
    return f;
  }

  public static RationalFunction multiply(RationalFunction f1, RationalFunction f2) {
    Polynomial n1 = f1.num;
    Polynomial d1 = f1.denom;
    Polynomial n2 = f2.num;
    Polynomial d2 = f2.denom;
    // So we want to compute a/b = (n1*n2)/(d1*d2).
    // Compute numerator a = n1*n2.
    Polynomial a = Polynomial.multiply(n1, n2);
    // Compute denominator b = d1*d2.
    Polynomial b = Polynomial.multiply(d1, d2);
    RationalFunction f = new RationalFunction(a, b);
    f.simplify();
    return f;
  }

  public static RationalFunction divide(RationalFunction f1, RationalFunction f2) {
    Polynomial n1 = f1.num;
    Polynomial d1 = f1.denom;
    Polynomial n2 = f2.num;
    Polynomial d2 = f2.denom;
    // So we want to compute a/b = (n1*d2)/(d1*n2).
    // Compute numerator a = n1*d2.
    Polynomial a = Polynomial.multiply(n1, d2);
    // Compute denominator b = d1*n2.
    Polynomial b = Polynomial.multiply(d1, n2);
    RationalFunction f = new RationalFunction(a, b);
    f.simplify();
    return f;
  }

  public static RationalFunction makeReciprocal(RationalFunction f) {
    return new RationalFunction(f.denom, f.num);
  }

  public static RationalFunction makeNegative(RationalFunction f) {
    Polynomial negnum = Polynomial.makeNegative(f.num);
    return new RationalFunction(negnum, f.denom);
  }

  /*
   * Does not alter the passed rational function; creates a new one.
   */
  public static RationalFunction applySubstitution(Substitution subs, RationalFunction f) {
    Polynomial n = Polynomial.applySubstitution(subs, f.num);
    Polynomial d = Polynomial.applySubstitution(subs, f.denom);
    RationalFunction g = new RationalFunction(n, d);
    g.simplify();
    return g;
  }

  public boolean isZero() {
    return num.isZero();
  }

  public boolean isUnity() {
    return num.equals(denom);
  }

  public boolean isPositive() {
    return isConstant() && num.isPositive();
  }

  public boolean isConstant() {
    return num.isConstant() && denom.isConstant();
  }

  public boolean isPolynomial() {
    return denom.isUnity();
  }

  public boolean isRationalConstantMultipleOf(RationalFunction that) {
    Polynomial p = this.num.cancelRationalContent();
    Polynomial q = this.denom.cancelRationalContent();
    RationalFunction a = new RationalFunction(p, q);
    Polynomial r = that.num.cancelRationalContent();
    Polynomial s = that.denom.cancelRationalContent();
    RationalFunction b = new RationalFunction(r, s);
    if (subtract(a, b).isZero()) {
      return true;
    } else if (add(a, b).isZero()) {
      return true;
    } else {
      return false;
    }
  }

  /*
   * Return the max of the number of terms of the num and denom.
   */
  public int getTermHeight() {
    return Math.max(num.getNumTerms(), denom.getNumTerms());
  }

  /*
   * If this function is constant, returns the value; else returns null.
   */
  public Rational getConstant() {
    Rational c = null;
    if (isConstant()) {
      Rational a = num.getConstant();
      Rational b = denom.getConstant();
      c = a.div(b);
    }
    return c;
  }

  @Override
  public boolean equals(Object o) {
    boolean ans = false;
    if (o instanceof RationalFunction) {
      String s1 = toString();
      String s2 = o.toString();
      ans = s1.equals(s2);
    }
    return ans;
  }

  /**
   * HashSet only looks to the equals method if the hashCodes of the
   * two objects are the same.
   */
  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public String toString() {
    //simplify();
    String s = num.toString();
    // Only bother to write denominator if we are not a polynomial, and we are not zero.
    if (!isPolynomial() && !isZero()) {
      //s += " / "+denom.toString();
      s = "("+s+") / ("+denom.toString()+")";
    }
    return s;
  }

  public void simplify() {

    // Check whether num is constant multiple of denom.
    Rational q = num.rationalConstantQuotientOver(denom);
    if (q != null) {
      // In this case, this rational function is simply equal to q, everywhere it is defined.
      num = new Polynomial(q);
      denom = new Polynomial(1);
    }

    // Make coefficients integers, if we are not a polynomial.
    if (!isPolynomial()) {
      Rational a = num.getRationalContent();
      Rational b = denom.getRationalContent();
      num = num.cancelRationalContent();
      denom = denom.cancelRationalContent();
      Rational c = a.div(b);
      int n = c.getNumerator();
      int d = c.getDenominator();
      num = Polynomial.multiply(new Polynomial(n), num);
      denom = Polynomial.multiply(new Polynomial(d), denom);
    }

    // Cancel common monomial content of num and denom.
    Monomial mn = num.getMonomialContent();
    Monomial md = denom.getMonomialContent();
    Monomial mc = Monomial.gcd(mn, md);
    if (!mc.isConstant()) {
      num.factorOut(mc);
      denom.factorOut(mc);
    }

    // Cancel common integer content.
    Integer cnI = num.getIntegerContent();
    Integer cdI = denom.getIntegerContent();
    int cn = cnI.intValue();
    int cd = cdI.intValue();
    if (cn >= 2 && cd >= 2) {
      Integer c = Polynomial.gcd(cnI, cdI);
      if (c.intValue() > 1) {
        Rational d = new Rational(1, c);
        Polynomial p = new Polynomial(d);
        num = Polynomial.multiply(p, num);
        denom = Polynomial.multiply(p, denom);
      }
    }

    // Divide through by a constant denominator different from 1.
    if (denom.isConstant() && !denom.isUnity()) {
      Rational c = denom.getConstant();
      Polynomial cinv = new Polynomial(c.makeReciprocal());
      num = Polynomial.multiply(cinv, num);
      denom = new Polynomial(1);
    }

    // If num is 0, set denom to 1.
    if (num.isZero()) {
      denom = Polynomial.makeUnity();
    }
    // If not, then check whether num = denom, and in that case set both to 1.
    else if (num.equals(denom)) {
      num = Polynomial.makeUnity();
      denom = Polynomial.makeUnity();
    }
    // If not, then check whether -1*num = denom, and in that case set num = -1, denom = 1.
    else if (Polynomial.multiply(new Polynomial(-1), num).equals(denom)) {
      num = new Polynomial(-1);
      denom = Polynomial.makeUnity();
    }

  }


}
