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
package org.sosy_lab.cpachecker.util.invariants;

import org.sosy_lab.cpachecker.util.invariants.balancer.Polynomial;


public class Rational {

  private final int num;
  private final int denom;
  private final boolean infty;

  /*
   * Rather than throwing exceptions, creating the burden of always
   * catching them, we simply have a boolean field 'infty' which we
   * set to 'true' if the denominator is zero, and to 'false' otherwise.
   *
   * In the 'true' case, we also set the numerator to 0. This makes it so
   * that dividing by an infinite rational gives infinity (see 'div' method).
   *
   * In the 'false' case, we put the rational number into a "normal form",
   * in which the denominator is made positive.
   *
   */
  public Rational(int n, int d) {
    if (d == 0) {
      infty = true;
      num = 0;
      denom = 0;
    } else {
      infty = false;
      if (d < 0) {
        num = -n;
        denom = -d;
      } else {
        num = n;
        denom = d;
      }
    }
  }

  public Rational copy() {
    return new Rational(num, denom);
  }

  public Rational makeNegative() {
    return new Rational(-num, denom);
  }

  public Rational makeReciprocal() {
    return new Rational(denom, num);
  }

  public static Rational makeUnity() {
    return new Rational(1, 1);
  }

  public static Rational makeZero() {
    return new Rational(0, 1);
  }

  public boolean equals(Rational r) {
    boolean ans = false;
    if (!isInfinite() && !r.isInfinite()) {
      ans = this.div(r).isUnity();
    }
    return ans;
  }

  public boolean isInfinite() {
    return infty;
  }

  public boolean isIntegral() {
    return !isInfinite() && num % denom == 0;
  }

  public boolean isZero() {
    return !isInfinite() && num == 0;
  }

  public boolean isUnity() {
    return !isInfinite() && num == denom;
  }

  public boolean isPositive() {
    return !isInfinite() && ((denom > 0 && num > 0) || (denom < 0 && num < 0));
  }

  /*
   * Return the "height" of the quotient, namely the maximum of the absolute values
   * of its numerator and denominator.
   */
  public int getHeight() {
    int a = Math.abs(num);
    int b = Math.abs(denom);
    return Math.max(a, b);
  }

  public int getNumerator() {
    return num;
  }

  public int getDenominator() {
    return denom;
  }

  /*
   * Create an Integer with the same value as this Rational, in the case that
   * this Rational is integral. Otherwise, return null.
   */
  public Integer makeInteger() {
    Integer z = null;
    if (isIntegral()) {
      z = Integer.valueOf(num/denom);
    }
    return z;
  }

  /*
   * Write as an integer, when integral; else, as a quotient.
   * In particular, if infinite, will appear as "0/0".
   */
  @Override
  public String toString() {
    String s = null;
    if (isIntegral()) {
      s = makeInteger().toString();
    } else {
      String a = Integer.toString(num);
      String b = Integer.toString(denom);
      s = a + "/" + b;
    }
    return s;
  }

  public Rational operate(String op, Rational other) {
    Rational r = null;
    if (op.equals("+")) {
      r = plus(other);
    } else if (op.equals("-")) {
      r = minus(other);
    } else if (op.equals("*")) {
      r = times(other);
    } else if (op.equals("/")) {
      r = div(other);
    }
    return r;
  }

  /*
   * Return a new Rational, equal to the result of cancelling
   * the gcd of num and denom.
   */
  public Rational leastTerms() {
    int g = Polynomial.gcd(num, denom);
    int n = num/g;
    int d = denom/g;
    return new Rational(n, d);
  }

  public Rational times(Rational other) {
    int n = this.num * other.num;
    int d = this.denom * other.denom;
    return new Rational(n, d).leastTerms();
  }

  /*
   * Create and return a new Rational, equal to this divided by other.
   */
  public Rational div(Rational other) {
    int a = other.num;
    int b = other.denom;
    int n = this.num * b;
    int d = this.denom * a;
    return new Rational(n, d).leastTerms();
  }

  public Rational plus(Rational other) {
    int a = this.num;
    int b = this.denom;
    int c = other.num;
    int d = other.denom;
    int p = a*d + b*c;
    int q = b*d;
    return new Rational(p, q).leastTerms();
  }

  public Rational minus(Rational other) {
    int a = this.num;
    int b = this.denom;
    int c = other.num;
    int d = other.denom;
    int p = a*d - b*c;
    int q = b*d;
    return new Rational(p, q).leastTerms();
  }

}
