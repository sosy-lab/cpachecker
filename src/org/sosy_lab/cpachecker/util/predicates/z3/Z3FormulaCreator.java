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
package org.sosy_lab.cpachecker.util.predicates.z3;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApi.*;
import static org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApiConstants.Z3_BV_SORT;

import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractFormulaCreator;

import com.google.common.base.Preconditions;

public class Z3FormulaCreator extends AbstractFormulaCreator<Long, Long, Long> {

  private final Z3SmtLogger smtLogger;

  public Z3FormulaCreator(
      long pEnv,
      long pBoolType,
      long pIntegerType,
      long pRealType,
      Z3SmtLogger smtLogger) {
    super(pEnv, pBoolType, pIntegerType, pRealType);

    this.smtLogger = smtLogger;
  }

  @Override
  public Long makeVariable(Long type, String varName) {
    long z3context = getEnv();
    long symbol = mk_string_symbol(z3context, varName);
    long var = mk_const(z3context, symbol, type);

    smtLogger.logVarDeclaration(var, type);

    return var;
  }

  @Override
  public Long extractInfo(Formula pT) {
    return Z3FormulaManager.getZ3Expr(pT);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Formula> FormulaType<T> getFormulaType(T pFormula) {
    if (pFormula instanceof BitvectorFormula) {
      long term = extractInfo(pFormula);
      long z3context = getEnv();
      long sort = get_sort(z3context, term);
      Preconditions.checkArgument(get_sort_kind(z3context, sort) == Z3_BV_SORT);
      return (FormulaType<T>) FormulaType.getBitvectorTypeWithSize(
          get_bv_sort_size(z3context, sort));
    }
    return super.getFormulaType(pFormula);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Formula> T encapsulate(Class<T> pClazz, Long pTerm) {
    Z3Formula f;
    if (pClazz == BitvectorFormula.class) {
      f = new Z3BitvectorFormula(getEnv(), pTerm);
    } else if (pClazz == IntegerFormula.class) {
      f = new Z3IntegerFormula(getEnv(), pTerm);
    } else if (pClazz == RationalFormula.class) {
      f = new Z3RationalFormula(getEnv(), pTerm);
    } else if (pClazz == BooleanFormula.class) {
      f = new Z3BooleanFormula(getEnv(), pTerm);
    } else {
      throw new IllegalArgumentException("invalid interface type");
    }
    return (T) f;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Formula> T encapsulate(FormulaType<T> pType, Long pTerm) {
    if (pType.isBooleanType()) {
      return (T)new Z3BooleanFormula(getEnv(), pTerm);
    } else if (pType.isIntegerType()) {
      return (T)new Z3IntegerFormula(getEnv(), pTerm);
    } else if (pType.isRationalType()) {
      return (T)new Z3RationalFormula(getEnv(), pTerm);
    } else if (pType.isBitvectorType()) {
      return (T)new Z3BitvectorFormula(getEnv(), pTerm);
    }
    throw new IllegalArgumentException("Cannot create formulas of type " + pType + " in Z3");
  }

  @Override
  public Long getBittype(int pBitwidth) {
    checkArgument(pBitwidth > 0, "Cannot use bitvector type with size %s", pBitwidth);
    long bvSort = mk_bv_sort(getEnv(), pBitwidth);
    inc_ref(getEnv(), sort_to_ast(getEnv(), bvSort));
    return bvSort;  }
}
