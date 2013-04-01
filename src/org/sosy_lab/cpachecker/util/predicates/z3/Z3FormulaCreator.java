/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractFormulaCreator;

public class Z3FormulaCreator extends AbstractFormulaCreator<Long, Long, Long> {

  public Z3FormulaCreator(
      long pEnv,
      long pBoolType,
      long pNumberType,
      AbstractFormulaCreator.CreateBitType<Long> pBittype) {
    super(pEnv, pBoolType, pNumberType, pBittype);
  }

  @Override
  public Long makeVariable(Long type, String varName) {
    long context = getEnv();
    long symbol = Z3NativeApi.mk_string_symbol(context, varName);
    return Z3NativeApi.mk_const(context, symbol, type);
  }

  @Override
  public Long extractInfo(Formula pT) {
    return Z3FormulaManager.getZ3Expr(pT);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Formula> T encapsulate(Class<T> pClazz, Long pTerm) {
    Z3Formula f;
    if (pClazz == BitvectorFormula.class) {
      f = new Z3BitvectorFormula(getEnv(), pTerm);
    } else if (pClazz == RationalFormula.class) {
      f = new Z3RationalFormula(getEnv(), pTerm);
    } else if (pClazz == BooleanFormula.class) {
      f = new Z3BooleanFormula(getEnv(), pTerm);
    } else {
      throw new IllegalArgumentException("invalid interface type");
    }
    return (T)f;
  }
}
