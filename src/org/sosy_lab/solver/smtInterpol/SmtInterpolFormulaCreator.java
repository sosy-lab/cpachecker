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

import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;

class SmtInterpolFormulaCreator
    extends FormulaCreator<Term, Sort, SmtInterpolEnvironment> {

  private final Sort booleanSort;
  private final Sort integerSort;
  private final Sort realSort;

  SmtInterpolFormulaCreator(final SmtInterpolEnvironment env) {
    super(env, env.getBooleanSort(), env.getIntegerSort(), env.getRealSort());
    booleanSort = env.getBooleanSort();
    integerSort = env.getIntegerSort();
    realSort = env.getRealSort();
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
    if (pSort == integerSort) {
      return FormulaType.IntegerType;
    } else if (pSort == realSort) {
      return FormulaType.RationalType;
    } else if (pSort == booleanSort) {
      return FormulaType.BooleanType;
    } else {
      throw new IllegalArgumentException("Unknown formula type");
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Formula> FormulaType<T> getFormulaType(final T pFormula) {
    if (pFormula instanceof ArrayFormula<?, ?>) {
      final FormulaType<?> arrayIndexType = getArrayFormulaIndexType(
          (ArrayFormula<?, ?>) pFormula);
      final FormulaType<?> arrayElementType = getArrayFormulaElementType(
          (ArrayFormula<?, ?>) pFormula);
      return (FormulaType<T>)new ArrayFormulaType<>(arrayIndexType,
          arrayElementType);
    }

    return super.getFormulaType(pFormula);
  }

  @Override
  public Term makeVariable(final Sort type, final String varName) {
    SmtInterpolEnvironment env = getEnv();
    env.declareFun(varName, new Sort[]{}, type);
    return env.term(varName);
  }

  @Override
  public Sort getBitvectorType(final int pBitwidth) {
    throw new UnsupportedOperationException("Bitvector theory is not supported "
        + "by SmtInterpol");
  }

  @Override
  public Sort getFloatingPointType(final FormulaType.FloatingPointType type) {
    throw new UnsupportedOperationException("FloatingPoint theory is not "
        + "supported by SmtInterpol");
  }

  @Override
  public Sort getArrayType(final Sort pIndexType, final Sort pElementType) {
    return getEnv().getTheory().getSort("Array", pIndexType, pElementType);
  }
}
