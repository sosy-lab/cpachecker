/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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

public class Rational {

  private int num;
  private int denom;

  public Rational(int n, int d) throws DivisionByZeroException {
    num = n;
    if (d == 0) {
      throw new DivisionByZeroException();
    }
    denom = d;
  }

  @Override
  public String toString() {
    String a = Integer.toString(num);
    String b = Integer.toString(denom);
    String s = a + "/" + b;
    return s;
  }

  public String toStringNice() {
    // Writes as a simple integer, when integral.
    // When not, normalizes the signs before calling the simple
    // toString method.
    String s = null;
    if (isIntegral()) {
      s = makeInteger().toString();
    } else {
      normalizeSigns();
      s = toString();
    }
    return s;
  }

  public void normalizeSigns() {
    // Normalizes the signs of num and denom.
    // (Plus a bit more: see last clause!)
    // If both are negative, makes both positive.
    // If exactly one is negative, makes num negative and denom
    // positive.
    // If num is zero, makes denom = 1.
    if (num==0) {
      denom = 1;
    } else if (denom<0) {
      num = -num;
      denom = -denom;
    }
  }

  public boolean isIntegral() {
    return (num % denom == 0);
  }

  public Integer makeInteger() {
    Integer z = null;
    if (isIntegral()) {
      z = new Integer(num/denom);
    }
    return z;
  }

  public Rational operate(String op, Rational other) throws
    DivisionByZeroException {
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

  public Rational times(Rational other) throws
    DivisionByZeroException {
    int n = this.num * other.num;
    int d = this.denom * other.denom;
    return new Rational(n,d);
  }

  public Rational div(Rational other) throws DivisionByZeroException
    {
    int a = other.num;
    if (a == 0) {
      throw new DivisionByZeroException();
    }
    int b = other.denom;
    int n = this.num * b;
    int d = this.denom * a;
    return new Rational(n,d);
  }

  public Rational plus(Rational other) throws
    DivisionByZeroException {
    int a = this.num;
    int b = this.denom;
    int c = other.num;
    int d = other.denom;
    int p = a*d + b*c;
    int q = b*d;
    // Should we reduce to lowest terms?
    // How about in times and div?
    // Maybe just do it in the constructor?
    return new Rational(p,q);
  }

  public Rational minus(Rational other) throws
    DivisionByZeroException {
    int a = this.num;
    int b = this.denom;
    int c = other.num;
    int d = other.denom;
    int p = a*d - b*c;
    int q = b*d;
    return new Rational(p,q);
  }

}
