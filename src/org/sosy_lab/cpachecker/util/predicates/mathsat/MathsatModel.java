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
package org.sosy_lab.cpachecker.util.predicates.mathsat;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.Model.AssignableTerm;
import org.sosy_lab.cpachecker.util.predicates.Model.Function;
import org.sosy_lab.cpachecker.util.predicates.Model.TermType;
import org.sosy_lab.cpachecker.util.predicates.Model.Variable;

import com.google.common.collect.ImmutableMap;

public class MathsatModel {

  private static TermType toMathsatType(int pTypeId) {

    switch (pTypeId) {
    case NativeApi.MSAT_BOOL:
      return TermType.Boolean;
    case NativeApi.MSAT_U:
      return TermType.Uninterpreted;
    case NativeApi.MSAT_INT:
      return TermType.Integer;
    case NativeApi.MSAT_REAL:
      return TermType.Real;
    default:
      if (pTypeId <= NativeApi.MSAT_BV) {
        throw new IllegalArgumentException("Given parameter is not a mathsat type!");
      } else {
        return TermType.Bitvector; // all other values are bitvectors of different sizes
      }
    }
  }

  private static Variable toVariable(long pVariableId) {
    if (NativeApi.msat_term_is_variable(pVariableId) == 0) {
      throw new IllegalArgumentException("Given mathsat id doesn't correspond to a variable! (" + NativeApi.msat_term_repr(pVariableId) + ")");
    }

    long lDeclarationId = NativeApi.msat_term_get_decl(pVariableId);
    String lName = NativeApi.msat_decl_get_name(lDeclarationId);
    TermType lType = toMathsatType(NativeApi.msat_decl_get_return_type(lDeclarationId));

    Pair<String, Integer> lSplitName = ArithmeticMathsatFormulaManager.parseName(lName);
    return new Variable(lSplitName.getFirst(), lSplitName.getSecond(), lType);
  }


  private static Function toFunction(long pFunctionId) {
    if (NativeApi.msat_term_is_variable(pFunctionId) != 0) {
      throw new IllegalArgumentException("Given mathsat id is a variable! (" + NativeApi.msat_term_repr(pFunctionId) + ")");
    }

    long lDeclarationId = NativeApi.msat_term_get_decl(pFunctionId);
    String lName = NativeApi.msat_decl_get_name(lDeclarationId);
    TermType lType = toMathsatType(NativeApi.msat_decl_get_return_type(lDeclarationId));

    int lArity = NativeApi.msat_decl_get_arity(lDeclarationId);

    // TODO we assume only constants (reals) as parameters for now
    Object[] lArguments = new Object[lArity];

    for (int lArgumentIndex = 0; lArgumentIndex < lArity; lArgumentIndex++) {
      long lArgument = NativeApi.msat_term_get_arg(pFunctionId, lArgumentIndex);

      String lTermRepresentation = NativeApi.msat_term_repr(lArgument);

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


  private static AssignableTerm toAssignable(long pTermId) {
    long lDeclarationId = NativeApi.msat_term_get_decl(pTermId);

    if (NativeApi.MSAT_ERROR_DECL(lDeclarationId)) {
      throw new IllegalArgumentException("No declaration available!");
    }

    if (NativeApi.msat_term_is_variable(pTermId) == 0) {
      return toFunction(pTermId);
    }
    else {
      return toVariable(pTermId);
    }
  }

  static Model createMathsatModel(final long sourceEnvironment,
      final MathsatFormulaManager fmgr, final boolean sharedEnvironments) {
    final long targetEnvironment = fmgr.msatEnv;
    ImmutableMap.Builder<AssignableTerm, Object> model = ImmutableMap.builder();
    long modelFormula = NativeApi.msat_make_true(targetEnvironment);

    long lModelIterator = NativeApi.msat_create_model_iterator(sourceEnvironment);

    if (NativeApi.MSAT_ERROR_MODEL_ITERATOR(lModelIterator)) {
      throw new RuntimeException("Erroneous model iterator! (" + lModelIterator + ")");
    }

    while (NativeApi.msat_model_iterator_has_next(lModelIterator) != 0) {
      long[] lModelElement = NativeApi.msat_model_iterator_next(lModelIterator);

      long lKeyTerm = lModelElement[0];
      long lValueTerm = lModelElement[1];

      if (!sharedEnvironments) {
        lKeyTerm = NativeApi.msat_make_copy_from(targetEnvironment, lKeyTerm, sourceEnvironment);
        lValueTerm = NativeApi.msat_make_copy_from(targetEnvironment, lValueTerm, sourceEnvironment);
      }

      long equivalence = NativeApi.msat_make_equal(targetEnvironment, lKeyTerm, lValueTerm);
      modelFormula = NativeApi.msat_make_and(targetEnvironment, modelFormula, equivalence);

      AssignableTerm lAssignable = toAssignable(lKeyTerm);

      // TODO maybe we have to convert to SMTLIB format and then read in values in a controlled way, e.g., size of bitvector
      // TODO we are assuming numbers as values
      if (!(NativeApi.msat_term_is_number(lValueTerm) != 0
            || NativeApi.msat_term_is_boolean_var(lValueTerm) != 0)) {
        throw new IllegalArgumentException("Mathsat term is not a number!");
      }

      String lTermRepresentation = NativeApi.msat_term_repr(lValueTerm);

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

      case Bitvector:
        lValue = fmgr.interpreteBitvector(lValueTerm);
        break;

      default:
        throw new IllegalArgumentException("Mathsat term with unhandled type " + lAssignable.getType());
      }

      model.put(lAssignable, lValue);
    }

    NativeApi.msat_destroy_model_iterator(lModelIterator);
    return new Model(model.build(), ArithmeticMathsatFormulaManager.encapsulate(modelFormula));
  }

}
