/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.mathsat5;

import static org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5NativeApi.*;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.Model.AssignableTerm;
import org.sosy_lab.cpachecker.util.predicates.Model.Function;
import org.sosy_lab.cpachecker.util.predicates.Model.TermType;
import org.sosy_lab.cpachecker.util.predicates.Model.Variable;
import org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5NativeApi.ModelIterator;

import com.google.common.collect.ImmutableMap;

public class Mathsat5Model {

  private static TermType toMathsatType(long e, long mType) {


    if (msat_is_bool_type(e, mType))
      return TermType.Boolean;
    else if (msat_is_integer_type(e, mType))
      return TermType.Integer;
    else if (msat_is_rational_type(e, mType))
      return TermType.Real;
    else if (msat_is_bv_type(e, mType))
      return TermType.Bitvector; // all other values are bitvectors of different sizes
    else
      throw new IllegalArgumentException("Given parameter is not a mathsat type!");
  }

  private static Variable toVariable(long env, long pVariableId) {
    if (!msat_term_is_constant(env, pVariableId)) {
      throw new IllegalArgumentException("Given mathsat id doesn't correspond to a variable! (" + msat_term_repr(pVariableId) + ")");
    }

    long lDeclarationId = msat_term_get_decl(pVariableId);
    String lName = msat_decl_get_name(lDeclarationId);
    TermType lType = toMathsatType(env, msat_decl_get_return_type(lDeclarationId));

    Pair<String, Integer> lSplitName = ArithmeticMathsat5FormulaManager.parseName(lName);
    return new Variable(lSplitName.getFirst(), lSplitName.getSecond(), lType);
  }


  private static Function toFunction(long env, long pFunctionId) {
    if (msat_term_is_constant(env, pFunctionId)) {
      throw new IllegalArgumentException("Given mathsat id is a variable! (" + msat_term_repr(pFunctionId) + ")");
    }

    long lDeclarationId = msat_term_get_decl(pFunctionId);
    String lName = msat_decl_get_name(lDeclarationId);
    TermType lType = toMathsatType(env, msat_decl_get_return_type(lDeclarationId));

    int lArity = msat_decl_get_arity(lDeclarationId);

    // TODO we assume only constants (reals) as parameters for now
    Object[] lArguments = new Object[lArity];

    for (int lArgumentIndex = 0; lArgumentIndex < lArity; lArgumentIndex++) {
      long lArgument = msat_term_get_arg(pFunctionId, lArgumentIndex);
      String lTermRepresentation = msat_term_repr(lArgument);

      Object lValue;

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

      lArguments[lArgumentIndex] = lValue;
    }

    return new Function(lName, lType, lArguments);
  }


  private static AssignableTerm toAssignable(long env, long pTermId) {
    if (!msat_term_is_constant(env, pTermId)) {
      return toFunction(env, pTermId);
    }
    else {
      return toVariable(env, pTermId);
    }
  }

  static Model createMathsatModel(long lMathsatEnvironmentID, Mathsat5FormulaManager fmgr) {
    ImmutableMap.Builder<AssignableTerm, Object> model = ImmutableMap.builder();
    long modelFormula = msat_make_true(lMathsatEnvironmentID);

    ModelIterator lModelIterator = msat_create_ModelIterator(lMathsatEnvironmentID);

    while (lModelIterator.hasNext()) {
      long[] lModelElement = lModelIterator.next();

      long lKeyTerm = lModelElement[0];
      long lValueTerm = lModelElement[1];


      long equivalence;

      if (msat_is_bool_type(lMathsatEnvironmentID, msat_term_get_type(lKeyTerm)) && msat_is_bool_type(lMathsatEnvironmentID, msat_term_get_type(lValueTerm)))
        equivalence = msat_make_iff(lMathsatEnvironmentID, lKeyTerm, lValueTerm);
      else
        equivalence = msat_make_equal(lMathsatEnvironmentID, lKeyTerm, lValueTerm);

      modelFormula = msat_make_and(lMathsatEnvironmentID, modelFormula, equivalence);

      AssignableTerm lAssignable = toAssignable(lMathsatEnvironmentID, lKeyTerm);

      // TODO maybe we have to convert to SMTLIB format and then read in values in a controlled way, e.g., size of bitvector
      // TODO we are assuming numbers as values
      if (!(msat_term_is_number(lMathsatEnvironmentID, lValueTerm)
            || msat_term_is_boolean_constant(lMathsatEnvironmentID, lValueTerm) || msat_term_is_false(lMathsatEnvironmentID, lValueTerm) || msat_term_is_true(lMathsatEnvironmentID, lValueTerm))) {
        throw new IllegalArgumentException("Mathsat term is not a number!");
      }

      String lTermRepresentation = msat_term_repr(lValueTerm);

      Object lValue;

      switch (lAssignable.getType()) {
      case Boolean:
        if (lTermRepresentation.equals("`true`")) {
          lValue = true;
        } else if (lTermRepresentation.equals("`false`")) {
          lValue = false;
        } else {
          throw new IllegalArgumentException("Mathsat unhandled boolean value " + lTermRepresentation);
        }
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

      case Bitvector:
        lValue = fmgr.interpreteBitvector(lValueTerm);
        break;

      default:
        throw new IllegalArgumentException("Mathsat term with unhandled type " + lAssignable.getType());
      }

      model.put(lAssignable, lValue);
    }

    lModelIterator.free();
    return new Model(model.build(), new Mathsat5Formula(lMathsatEnvironmentID, modelFormula));
  }

}
