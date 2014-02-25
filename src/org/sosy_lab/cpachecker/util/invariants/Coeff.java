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

import java.util.List;
import java.util.Map;

import org.sosy_lab.cpachecker.util.invariants.balancer.Polynomial;
import org.sosy_lab.cpachecker.util.invariants.balancer.RationalFunction;
import org.sosy_lab.cpachecker.util.invariants.balancer.Variable;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateNumber;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateSum;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateTerm;
import org.sosy_lab.cpachecker.util.invariants.templates.VariableWriteMode;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;

public class Coeff {

  private final TemplateSum value;
  private VariableWriteMode vwm = VariableWriteMode.REDLOG;

  public Coeff(TemplateSum s) {
    value = s;
  }

  public Coeff(TemplateSum s, VariableWriteMode vwm) {
    value = s;
    this.vwm = vwm;
  }

  public RationalFunction makeRationalFunction(Map<String, Variable> paramVars) {
    Polynomial num = value.makePolynomial(paramVars);
    Polynomial denom = new Polynomial(1);
    RationalFunction f = new RationalFunction(num, denom);
    f.simplify();
    return f;
  }

  /*
   * For this constructor, the string must represent an integer.
   */
  public Coeff(FormulaType<?> type, String s) {
    TemplateNumber n = new TemplateNumber(type, s);
    TemplateTerm t = new TemplateTerm(type);
    t.setCoefficient(n);
    value = t;
  }

  /*
  public String getValue() {
    return a;
  }
  */

  @Override
  public String toString() {
    return value.toString(vwm);
  }

  /*
  public void setValue(String b) {
    a = b;
  }
  */

  public Coeff negative() {
    TemplateSum s = value.copy();
    s.negate();
    return new Coeff(s);
  }

  /*
  public static Vector<Coeff> makeCoeffList(String[] C) {
    Vector<Coeff> coeffs = new Vector<>();
    for (int i = 0; i < C.length; i++) {
      coeffs.add( new Coeff(C[i] ));
    }
    return coeffs;
  }
  */

  public static String coeffsToString(List<Coeff> list) {
    // From a List of Coeffs, create a string listing them all,
    // separated by spaces.
    String s = "";
    for (Coeff c : list) {
      s += " "+c.toString();
    }
    return s;
  }

}