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
package org.sosy_lab.cpachecker.util.invariants.templates;

import org.sosy_lab.cpachecker.util.invariants.Rational;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

public class TemplateNumber extends TemplateFormula {

  private Rational rat;

  public TemplateNumber(int n) {
    rat = new Rational(n,1);
  }

  /*
   * Here we enforce the rule that our numbers are rationals, not floats.
   */
  public TemplateNumber(String s) {
    try {
      Integer i = new Integer(s);
      rat = new Rational(i,1);
    } catch (Exception e) {
      System.err.println("Attempted to use float "+s+".\nOnly rational coefficients are allowed.");
      System.exit(1);
    }
  }

  public Rational rationalValue() {
    return rat;
  }

  public TemplateNumber(Rational r) {
    rat = r;
  }

  @Override
  public TemplateNumber copy() {
    TemplateNumber n = new TemplateNumber(rat.copy());
    return n;
  }

  public boolean isZero() {
    return rat.isZero();
  }

  @Override
  public void negate() {
    rat = rat.makeNegative();
  }

  public static TemplateNumber multiply(TemplateNumber n1, TemplateNumber n2) {
    Rational r = n1.rat.times(n2.rat);
    return new TemplateNumber(r);
  }

  public static TemplateNumber add(TemplateNumber n1, TemplateNumber n2) {
    Rational r = n1.rat.plus(n2.rat);
    return new TemplateNumber(r);
  }

  public TemplateNumber divideBy(TemplateNumber n) {
    return new TemplateNumber( rat.div(n.rat) );
  }

  public TemplateNumber makeReciprocal() {
    return new TemplateNumber( rat.makeReciprocal() );
  }

  public static TemplateNumber makeUnity() {
    return new TemplateNumber( Rational.makeUnity() );
  }

  public static TemplateNumber makeZero() {
    return new TemplateNumber( Rational.makeZero() );
  }

  public boolean equals(TemplateNumber n) {
    return rat.equals(n.rat);
  }

  @Override
  public Formula translate(FormulaManager fmgr) {
  	return fmgr.makeNumber(rat.toString());
  }

  @Override
  public String toString() {
    return rat.toString();
  }

}