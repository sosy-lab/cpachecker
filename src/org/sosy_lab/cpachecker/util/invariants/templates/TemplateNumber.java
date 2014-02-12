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
package org.sosy_lab.cpachecker.util.invariants.templates;

import org.sosy_lab.cpachecker.util.invariants.Rational;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

public class TemplateNumber extends TemplateNumericValue {

  private Rational rat;

  public TemplateNumber(FormulaType<?> type, int n) {
    super(type);
    rat = new Rational(n, 1);
  }

  /*
   * Here we enforce the rule that our numbers are rationals, not floats.
   */
  public TemplateNumber(FormulaType<?> type, String s) {
    super(type);
    try {
      Integer i = Integer.valueOf(s);
      rat = new Rational(i, 1);
    } catch (Exception e) {
      System.err.println("Attempted to use float "+s+".\nOnly rational coefficients are allowed.");
      System.exit(1);
    }
  }

  public Rational rationalValue() {
    return rat;
  }

  public TemplateNumber(FormulaType<?> type, Rational r) {
    super(type);
    rat = r;
  }

  @Override
  public TemplateNumber copy() {
    return withFormulaType(getFormulaType());
  }

  @Override
  public TemplateNumber withFormulaType(FormulaType<?> pNewType) {
    TemplateNumber n = new TemplateNumber(pNewType, rat.copy());
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
    return new TemplateNumber(n1.getFormulaType(), r);
  }

  public static TemplateNumber add(TemplateNumber n1, TemplateNumber n2) {
    Rational r = n1.rat.plus(n2.rat);
    return new TemplateNumber(n1.getFormulaType(), r);
  }

  public TemplateNumber divideBy(TemplateNumber n) {
    return new TemplateNumber(getFormulaType(), rat.div(n.rat));
  }

  public TemplateNumber makeReciprocal() {
    return new TemplateNumber(getFormulaType(), rat.makeReciprocal());
  }

  public static TemplateNumber makeUnity(FormulaType<?> type) {
    return new TemplateNumber(type, Rational.makeUnity());
  }

  public static TemplateNumber makeZero(FormulaType<?> type) {
    return new TemplateNumber(type, Rational.makeZero());
  }

  public boolean equals(TemplateNumber n) {
    return rat.equals(n.rat);
  }

  @Override
  public Formula translate(FormulaManagerView fmgr) {
    return fmgr.makeNumber(getFormulaType(), rat.makeInteger());
    //return fmgr.makeNumber(rat.toString());
  }

  @Override
  public String toString() {
    return rat.toString();
  }


}
