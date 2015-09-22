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
package org.sosy_lab.solver.smtInterpol;

import org.sosy_lab.solver.api.ArrayFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FormulaType;
import org.sosy_lab.solver.api.FormulaType.ArrayFormulaType;
import org.sosy_lab.solver.basicimpl.FormulaCreator;

import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.logic.Theory;

class SmtInterpolFormulaCreator extends FormulaCreator<Term, Sort, SmtInterpolEnvironment> {

  SmtInterpolFormulaCreator(SmtInterpolEnvironment env) {
    super(env, env.getBooleanSort(), env.getIntegerSort(), env.getRealSort());
  }

  @Override
  public FormulaType<?> getFormulaType(final Term pFormula) {
    if (SmtInterpolUtil.isBoolean(pFormula)) {
      return FormulaType.BooleanType;
    } else if (SmtInterpolUtil.hasIntegerType(pFormula)) {
      return FormulaType.IntegerType;
    } else if (SmtInterpolUtil.hasRationalType(pFormula)) {
      return FormulaType.RationalType;
    } else if (SmtInterpolUtil.hasArrayType(pFormula)){
      Sort[] argumentSorts = pFormula.getSort().getArguments();
      assert argumentSorts.length == 2 : "Array sort has to have two arguments,"
          + " one for index type and one for element type!";

      return new FormulaType.ArrayFormulaType<>(
          getFormulaTypeOfSort(argumentSorts[0]),
          getFormulaTypeOfSort(argumentSorts[1]));
    }
    throw new IllegalArgumentException("Unknown formula type");
  }

  private FormulaType<?> getFormulaTypeOfSort(final Sort pSort) {
    final Theory theory = getEnv().getTheory();
    final Sort integer = theory.getNumericSort();
    final Sort rational = theory.getRealSort();
    final Sort bool = theory.getBooleanSort();

    if (pSort == integer) {
      return FormulaType.IntegerType;
    } else if (pSort == rational) {
      return FormulaType.RationalType;
    } else if (pSort == bool) {
      return FormulaType.BooleanType;
    } else {
      throw new IllegalArgumentException("Unknown formula type");
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Formula> FormulaType<T> getFormulaType(T pFormula) {
    if (pFormula instanceof ArrayFormula<?,?>) {
      FormulaType<T> arrayIndexType = getArrayFormulaIndexType(
          (ArrayFormula<T, T>) pFormula);
      FormulaType<T> arrayElementType = getArrayFormulaElementType(
          (ArrayFormula<T, T>) pFormula);
      return (FormulaType<T>)new ArrayFormulaType<>(arrayIndexType,
          arrayElementType);
    }

    return super.getFormulaType(pFormula);
  }

  @Override
  public Term makeVariable(Sort type, String varName) {
    SmtInterpolEnvironment env = getEnv();
    env.declareFun(varName, new Sort[]{}, type);
    return env.term(varName);
  }

  @Override
  public Sort getBitvectorType(int pBitwidth) {
    throw new UnsupportedOperationException("Bitvector theory is not supported by SmtInterpol");
  }

  @Override
  public Sort getFloatingPointType(FormulaType.FloatingPointType type) {
    throw new UnsupportedOperationException("FloatingPoint theory is not supported by SmtInterpol");
  }

  @Override
  public Sort getArrayType(Sort pIndexType, Sort pElementType) {
    return getEnv().getTheory().getSort("Array", pIndexType, pElementType);
  }
}
