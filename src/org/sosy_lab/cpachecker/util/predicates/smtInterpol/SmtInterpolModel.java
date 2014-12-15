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

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.core.counterexample.Model.AssignableTerm;
import org.sosy_lab.cpachecker.core.counterexample.Model.Constant;
import org.sosy_lab.cpachecker.core.counterexample.Model.Function;
import org.sosy_lab.cpachecker.core.counterexample.Model.TermType;
import org.sosy_lab.cpachecker.core.counterexample.Model.Variable;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Verify;

import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;

class SmtInterpolModel {

  private static TermType toSmtInterpolType(Sort sort) {

    switch (sort.getName()) {
      case "Bool":
        return TermType.Boolean;
      case "Int":
        return TermType.Integer;
      case "Real":
        return TermType.Real;
      default:
        throw new IllegalArgumentException("Given sort cannot be converted to a TermType: " + sort);
    }
  }

  private static AssignableTerm toVariable(Term t) {
    if (!SmtInterpolUtil.isVariable(t)) {
      throw new IllegalArgumentException("Given term is no variable! (" + t.toString() + ")");
    }

    ApplicationTerm appTerm = (ApplicationTerm)t;
    String lName = appTerm.getFunction().getName();
    TermType lType = toSmtInterpolType(appTerm.getSort());

    Pair<String, Integer> lSplitName = FormulaManagerView.parseName(lName);
    if (lSplitName.getSecond() != null) {
      return new Variable(lSplitName.getFirst(), lSplitName.getSecond(), lType);
    } else {
      return new Constant(lSplitName.getFirst(), lType);
    }
  }


  private static Function toFunction(Term t, de.uni_freiburg.informatik.ultimate.logic.Model values) {
    if (SmtInterpolUtil.isVariable(t)) {
      throw new IllegalArgumentException("Given term is no function! (" + t.toString() + ")");
    }

    ApplicationTerm appTerm = (ApplicationTerm)t;
    String lName = appTerm.getFunction().getName();
    TermType lType = toSmtInterpolType(appTerm.getSort());

    int lArity = SmtInterpolUtil.getArity(appTerm);

    Object[] lArguments = new Object[lArity];

    for (int lArgumentIndex = 0; lArgumentIndex < lArity; lArgumentIndex++) {
      Term lArgument = SmtInterpolUtil.getArg(appTerm, lArgumentIndex);
      lArgument = values.evaluate(lArgument);

      Object lValue;
      if (SmtInterpolUtil.isNumber(lArgument)) {
        lValue = SmtInterpolUtil.toNumber(lArgument);
      } else {
        lValue = SmtInterpolUnsafeFormulaManager.dequote(lArgument.toString());
      }
      lArguments[lArgumentIndex] = lValue;
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

  static Model createSmtInterpolModel(SmtInterpolEnvironment env, Collection<Term> assertedFormulas) {
    de.uni_freiburg.informatik.ultimate.logic.Model values = env.getModel();

    Map<AssignableTerm, Object> model = new LinkedHashMap<>();
    for (Term lKeyTerm : SmtInterpolUtil.getVarsAndUIFs(assertedFormulas)) {
      Term lValueTerm = values.evaluate(lKeyTerm);

      AssignableTerm lAssignable = toAssignable(lKeyTerm, values);

      // TODO maybe we have to convert to SMTLIB format and
      // then read in values in a controlled way, e.g., size of bitvector
      // TODO we are assuming numbers as values
      if (!(SmtInterpolUtil.isNumber(lValueTerm)
            || SmtInterpolUtil.isBoolean(lValueTerm))) {
        // TODO is there a bug in SmtInterpol??
        // with new version from 2012.04.09 there can be ApplicationTerms in the model
        // we put the Term into the model
        model.put(lAssignable, SmtInterpolUnsafeFormulaManager.dequote(lValueTerm.toStringDirect()));
      } else {

      String lTermRepresentation = lValueTerm.toString();

      Object lValue;

      switch (lAssignable.getType()) {
      case Boolean:
        lValue = Boolean.valueOf(lTermRepresentation);
        break;

      case Real:
        lValue = SmtInterpolUtil.toNumber(lValueTerm);
        break;

      case Integer:
        lValue = SmtInterpolUtil.toNumber(lValueTerm);
        break;

//      case Bitvector:
//        lValue = fmgr.interpreteBitvector(lValueTerm);
//        break;

      default:
        throw new IllegalArgumentException("SmtInterpol term with unhandled type " + lAssignable.getType());
      }

      // Duplicate entries may occur if "uf(a)" and "uf(b)" occur in the formulas
      // and "a" and "b" have the same value, because "a" and "b" will both be resolved,
      // leading to two entries for "uf(1)" (if value is 1).
      Object existingValue = model.get(lAssignable);
      Verify.verify(existingValue == null || lValue.equals(existingValue),
          "Duplicate values for model entry %s: %s and %s", lAssignable, existingValue, lValue
          );
      model.put(lAssignable, lValue);
    }
    }

    return new Model(model);
  }

}
