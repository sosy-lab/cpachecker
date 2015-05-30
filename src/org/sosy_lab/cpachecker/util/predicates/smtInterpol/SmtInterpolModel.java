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
package org.sosy_lab.cpachecker.util.predicates.smtInterpol;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.core.counterexample.Model.Variable;
import org.sosy_lab.cpachecker.util.predicates.AssignableTerm;
import org.sosy_lab.cpachecker.core.counterexample.Model.Function;
import org.sosy_lab.cpachecker.util.predicates.TermType;

import com.google.common.base.Verify;

import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.Term;

class SmtInterpolModel {

  private static TermType getType(Term t) {
    if (SmtInterpolUtil.isBoolean(t)) {
      return TermType.Boolean;
    } else if (SmtInterpolUtil.hasIntegerType(t)) {
      return TermType.Integer;
    } else if (SmtInterpolUtil.hasRationalType(t)) {
      return TermType.Real;
    }

    throw new IllegalArgumentException("Given sort cannot be converted to a TermType: " + t.getSort());
  }

  private static AssignableTerm toVariable(Term t) {
    if (!SmtInterpolUtil.isVariable(t)) {
      throw new IllegalArgumentException("Given term is no variable! (" + t.toString() + ")");
    }

    ApplicationTerm appTerm = (ApplicationTerm)t;
    String lName = appTerm.getFunction().getName();
    TermType lType = getType(appTerm);
    return new Variable(lName, lType);
  }


  private static Function toFunction(Term t, de.uni_freiburg.informatik.ultimate.logic.Model values) {
    if (SmtInterpolUtil.isVariable(t)) {
      throw new IllegalArgumentException("Given term is no function! (" + t.toString() + ")");
    }

    ApplicationTerm appTerm = (ApplicationTerm)t;
    String lName = appTerm.getFunction().getName();
    TermType lType = getType(appTerm);

    int lArity = SmtInterpolUtil.getArity(appTerm);

    Object[] lArguments = new Object[lArity];

    for (int lArgumentIndex = 0; lArgumentIndex < lArity; lArgumentIndex++) {
      Term lArgument = SmtInterpolUtil.getArg(appTerm, lArgumentIndex);
      lArgument = values.evaluate(lArgument);
      lArguments[lArgumentIndex] = getValue(lArgument);
    }

    return new Function(lName, lType, lArguments);
  }


  private static AssignableTerm toAssignable(Term t, de.uni_freiburg.informatik.ultimate.logic.Model values) {

    assert t instanceof ApplicationTerm : "This is no ApplicationTerm: " + t.toString();

    if (SmtInterpolUtil.isVariable(t)) {
      return toVariable(t);
    } else {
      return toFunction(t, values);
    }
  }

  private static Object getValue(Term value) {
    if (SmtInterpolUtil.isTrue(value)) {
      return true;
    } else if (SmtInterpolUtil.isFalse(value)) {
      return false;
    } else if (SmtInterpolUtil.isNumber(value)) {
      return SmtInterpolUtil.toNumber(value);
    }

    throw new IllegalArgumentException("SmtInterpol model term with expected value " + value);
  }

  static Model createSmtInterpolModel(SmtInterpolEnvironment env, Collection<Term> assertedFormulas) {
    de.uni_freiburg.informatik.ultimate.logic.Model values = env.getModel();

    Map<AssignableTerm, Object> model = new LinkedHashMap<>();
    for (Term lKeyTerm : SmtInterpolUtil.getVarsAndUIFs(assertedFormulas)) {
      Term lValueTerm = values.evaluate(lKeyTerm);

      AssignableTerm lAssignable = toAssignable(lKeyTerm, values);
      Object lValue = getValue(lValueTerm);

      // Duplicate entries may occur if "uf(a)" and "uf(b)" occur in the formulas
      // and "a" and "b" have the same value, because "a" and "b" will both be resolved,
      // leading to two entries for "uf(1)" (if value is 1).
      Object existingValue = model.get(lAssignable);
      Verify.verify(existingValue == null || lValue.equals(existingValue),
          "Duplicate values for model entry %s: %s and %s", lAssignable, existingValue, lValue
          );
      model.put(lAssignable, lValue);
    }

    return new Model(model);
  }
}