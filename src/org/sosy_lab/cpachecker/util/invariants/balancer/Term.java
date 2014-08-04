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

public class Term {

  private Monomial monom = new Monomial();
  private Rational coeff = Rational.makeZero();

  public Term() {
    monom = new Monomial();
    coeff = Rational.makeZero();
  }

  public Term(Rational c, Monomial m) {
    coeff = c;
    monom = m;
  }

  public Term(int n) {
    makeConstant(new Rational(n, 1));
  }

  public Term(Rational r) {
    makeConstant(r);
  }

  public Term(Variable v) {
    monom = new Monomial(v);
    coeff = Rational.makeUnity();
  }

  public Term copy() {
    return new Term(coeff.copy(), monom.copy());
  }

  public static Term divide(Term t, Monomial m) {
    Term u = new Term();
    u.setMonomial(m);
    u.setCoeff(Rational.makeUnity());
    return divide(t, u);
  }

  /*
   * Create the result of dividing t by u.
   */
  public static Term divide(Term t, Term u) {
    Rational ct = t.getCoeff();
    Rational cu = u.getCoeff();
    Monomial mt = t.getMonomial();
    Monomial mu = u.getMonomial();
    Rational cv = ct.div(cu);
    Monomial mv = Monomial.divide(mt, mu);
    Term v = new Term();
    v.setCoeff(cv);
    v.setMonomial(mv);
    return v;
  }

  /*
   * Make this Term to be the constant specified by the passed Rational r.
   */
  private void makeConstant(Rational r) {
    monom = new Monomial();
    coeff = r;
  }

  public static Term multiply(Term t1, Term t2) {
    Rational c1 = t1.coeff;
    Rational c2 = t2.coeff;
    Monomial m1 = t1.monom;
    Monomial m2 = t2.monom;
    Term t = new Term();
    Rational c3 = c1.times(c2);
    Monomial m3 = Monomial.multiply(m1, m2);
    t.setCoeff(c3);
    t.setMonomial(m3);
    return t;
  }

  public static Term makeNegative(Term t) {
    Term u = new Term();
    u.setCoeff(t.getCoeff().makeNegative());
    u.setMonomial(t.getMonomial());
    return u;
  }

  public static Term makeZero() {
    Term t = new Term();
    t.setCoeff(Rational.makeZero());
    return t;
  }

  public static Term makeUnity() {
    Term t = new Term();
    t.setCoeff(Rational.makeUnity());
    return t;
  }

  public boolean isZero() {
    return coeff.isZero();
  }

  public boolean isUnity() {
    return this.isConstant() && coeff.isUnity();
  }

  public boolean isPositive() {
    return this.isConstant() && coeff.isPositive();
  }

  public boolean isConstant() {
    return monom.isConstant();
  }

  public boolean isLinear() {
    return monom.isLinear();
  }

  /*
   * If this term is linear, then return its one variable; else return null.
   */
  public Variable getLinearVariable() {
    return monom.getLinearVariable();
  }

  public void setCoeff(Rational r) {
    coeff = r;
  }

  public Rational getCoeff() {
    return coeff;
  }

  public void setMonomial(Monomial m) {
    monom = m;
  }

  public Monomial getMonomial() {
    return monom;
  }

  public int getDegree() {
    return monom.getDegree();
  }

  public void setPower(Variable v, int n) {
    monom.setPower(v, n);
  }

  public int getPower(Variable v) {
    return monom.getPower(v);
  }

  @Override
  public String toString() {
    // If 0, just return that.
    if (coeff.isZero()) {
      return "0";
    }
    // Write the monomial.
    // By default, we'll sort the monomials alphabetically.
    // We can make this an option later, if it really matters.
    boolean sortAlpha = true;
    String s = monom.toString(sortAlpha);
    // If there are no variables, then just write the coefficient.
    if (s.equals("1")) {
      return coeff.toString();
    }
    // Otherwise there was at least one variable.
    // If coeff is 1, then just return monomial.
    if (coeff.isUnity()) {
      return s;
    }
    // If coeff is -1, just prefix with a minus sign.
    if (coeff.makeNegative().isUnity()) {
      return "-"+s;
    }
    // Else we need to actually multiply by the coefficient.
    return coeff.toString()+"*"+s;
  }

}
