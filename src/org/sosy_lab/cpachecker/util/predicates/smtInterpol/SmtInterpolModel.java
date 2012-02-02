/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.Model.AssignableTerm;
import org.sosy_lab.cpachecker.util.predicates.Model.Function;
import org.sosy_lab.cpachecker.util.predicates.Model.TermType;
import org.sosy_lab.cpachecker.util.predicates.Model.Variable;

import com.google.common.collect.ImmutableMap;

import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.Valuation;

public class SmtInterpolModel {

  private static TermType toSmtInterpolType(Sort sort) {

    if ("Bool".equals(sort.getName())) {
      return TermType.Boolean;
    } else if ("Int".equals(sort.getName())) {
      return TermType.Integer;
    } else if ("Real".equals(sort.getName())) {
      return TermType.Real;

      // TODO TermType.Uninterpreted; TermType.Bitvector;

    } else {
      throw new IllegalArgumentException("Given parameter cannot be converted to a TermType!");
    }
  }

  private static Variable toVariable(Term t) {
    if (!SmtInterpolUtil.isVariable(t)) {
      throw new IllegalArgumentException("Given term is no variable! (" + t.toString() + ")");
    }

    ApplicationTerm appTerm = (ApplicationTerm)t;
    String lName = appTerm.getFunction().getName();
    TermType lType = toSmtInterpolType(appTerm.getSort());

    Pair<String, Integer> lSplitName = ArithmeticSmtInterpolFormulaManager.parseName(lName);
    return new Variable(lSplitName.getFirst(), lSplitName.getSecond(), lType);
  }


  private static Function toFunction(Term t) {
    if (SmtInterpolUtil.isVariable(t)) {
      throw new IllegalArgumentException("Given term is no function! (" + t.toString() + ")");
    }

    ApplicationTerm appTerm = (ApplicationTerm)t;
    String lName = appTerm.getFunction().getName();
    TermType lType = toSmtInterpolType(appTerm.getSort());

    int lArity = SmtInterpolUtil.getArity(appTerm);

    // TODO we assume only constants (reals) as parameters for now
    Object[] lArguments = new Object[lArity];

    for (int lArgumentIndex = 0; lArgumentIndex < lArity; lArgumentIndex++) {
      Term lArgument = SmtInterpolUtil.getArg(appTerm, lArgumentIndex);

      String lTermRepresentation = lArgument.toString();

      Object lValue;

      try {
        lValue = Double.valueOf(lTermRepresentation);
      }
      catch (NumberFormatException e) {
        // TODO this part is copied from mathsat, can we use it for smtInterpol, too?
        // lets try special case for mathsat
        String[] lNumbers = lTermRepresentation.split("/");

        if (lNumbers.length != 2) {
          throw new NumberFormatException("Unknown number format: " + lTermRepresentation);
        }

        double lNumerator = Double.valueOf(lNumbers[0]);
        double lDenominator = Double.valueOf(lNumbers[1]);

        lValue = lNumerator/lDenominator;
      }
      lArguments[lArgumentIndex] = lValue;
    }

    return new Function(lName, lType, lArguments);
  }


  private static AssignableTerm toAssignable(Term t) {

    assert t instanceof ApplicationTerm : "This is no ApplicationTerm: " + t.toString();

    if (SmtInterpolUtil.isVariable(t)) {
      return toVariable(t);
    } else {
      return toFunction(t);
    }
  }

  static Model createSmtInterpolModel(Script script, Term[] keys) throws SMTLIBException {
    ImmutableMap.Builder<AssignableTerm, Object> model = ImmutableMap.builder();

    Valuation val = script.getValue(keys);

    Term modelFormula = script.term("true");

    for (Term lKeyTerm : keys) {
      Term lValueTerm = val.get(lKeyTerm);

      Term equivalence = script.term("=", lKeyTerm, lValueTerm);
      modelFormula = script.term("and", modelFormula, equivalence);

      AssignableTerm lAssignable = toAssignable(lKeyTerm);

      // TODO maybe we have to convert to SMTLIB format and
      // then read in values in a controlled way, e.g., size of bitvector
      // TODO we are assuming numbers as values
      if (!(SmtInterpolUtil.isNumber(lValueTerm)
            || SmtInterpolUtil.isBoolean(script, lValueTerm))) {
        throw new IllegalArgumentException("Mathsat term is not a number!");
      }

      String lTermRepresentation = lValueTerm.toString();

      Object lValue;

      switch (lAssignable.getType()) {
      case Boolean:
        lValue = Boolean.valueOf(lTermRepresentation);
        break;
      case Real:
        try {
          lValue = Double.valueOf(lTermRepresentation);
        }
        catch (NumberFormatException e) {
          // lets try special case for mathsat
          String[] lNumbers = lTermRepresentation.split("/");

          if (lNumbers.length != 2) {
            throw new NumberFormatException("Unknown number format: " + lTermRepresentation);
          }

          double lNumerator = Double.valueOf(lNumbers[0]);
          double lDenominator = Double.valueOf(lNumbers[1]);

          lValue = lNumerator/lDenominator;
        }

        break;

      case Integer:
        lValue = Long.valueOf(lTermRepresentation);
        break;

//      case Bitvector:
//        lValue = fmgr.interpreteBitvector(lValueTerm);
//        break;

      default:
        throw new IllegalArgumentException("SmtInterpol term with unhandled type " + lAssignable.getType());
      }

      model.put(lAssignable, lValue);
    }

    return new Model(model.build(), new SmtInterpolFormula(modelFormula, script));
  }

}
