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

import static org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApi.*;

import java.math.BigDecimal;

import org.sosy_lab.cpachecker.util.UniqueIdGenerator;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;

class Z3IntegerFormulaManager extends Z3NumeralFormulaManager<IntegerFormula, IntegerFormula> {

  Z3IntegerFormulaManager(
          Z3FormulaCreator pCreator,
          Z3FunctionFormulaManager pFunctionManager) {
    super(pCreator, pFunctionManager);
  }

  @Override
  public FormulaType<IntegerFormula> getFormulaType() {
    return FormulaType.IntegerType;
  }

  @Override
  protected long getNumeralType() {
    return getFormulaCreator().getIntegerType();
  }

  @Override
  protected Long makeNumberImpl(double pNumber) {
    return makeNumberImpl((long)pNumber);
  }

  @Override
  protected Long makeNumberImpl(BigDecimal pNumber) {
    return decimalAsInteger(pNumber);
  }

  private final UniqueIdGenerator freshVarID = new UniqueIdGenerator();
  private static final String CONGRUENCE_VAR_TEMPLATE = "__CONGRUENCE_%d";

  /**
   * Manually implement the constraint
   * {@code a = b (mod pModulo)}
   * by adding a constraint
   * {@code a = b + K * pModulo},
   * which can be solved for some unknown {@code K}.
   */
  @Override
  protected Long modularCongruence(Long pNumber1, Long pNumber2, long pModulo) {
    if (pModulo > 0) {
      long modulo = mk_int64(z3context, pModulo, getNumeralType());
      inc_ref(z3context, modulo);

      long unknownCoeff =
          makeVariableImpl(String.format(CONGRUENCE_VAR_TEMPLATE, freshVarID.getFreshId()));
      inc_ref(z3context, unknownCoeff);

      long rhs = add(pNumber2, multiply(modulo, unknownCoeff));
      inc_ref(z3context, rhs);

      long out = equal(pNumber1, rhs);
      dec_ref(z3context, modulo);
      dec_ref(z3context, unknownCoeff);
      dec_ref(z3context, rhs);
      return out;
    }

    return mk_true(z3context);
  }
}
