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
package org.sosy_lab.solver.mathsat5;

import static org.sosy_lab.solver.mathsat5.Mathsat5NativeApi.*;

import java.math.BigDecimal;

import org.sosy_lab.solver.api.FormulaType;
import org.sosy_lab.solver.api.NumeralFormula.IntegerFormula;


class Mathsat5IntegerFormulaManager extends Mathsat5NumeralFormulaManager<IntegerFormula, IntegerFormula> {

  public Mathsat5IntegerFormulaManager(
          Mathsat5FormulaCreator pCreator,
          Mathsat5FunctionFormulaManager functionManager,
          boolean useNonLinearArithmetic) {
    super(pCreator, functionManager, useNonLinearArithmetic);
  }

  @Override
  protected long getNumeralType() {
    return getFormulaCreator().getIntegerType();
  }

  @Override
  public FormulaType<IntegerFormula> getFormulaType() {
    return FormulaType.IntegerType;
  }

  @Override
  protected Long makeNumberImpl(double pNumber) {
    return makeNumberImpl((long)pNumber);
  }

  @Override
  protected Long makeNumberImpl(BigDecimal pNumber) {
    return decimalAsInteger(pNumber);
  }

  @Override
  public Long linearDivide(Long pNumber1, Long pNumber2) {
    assert isNumeral(pNumber2);
    long mathsatEnv = getFormulaCreator().getEnv();
    long t1 = pNumber1;
    long t2 = pNumber2;

      // invert t2 and multiply with it
      String n = msat_term_repr(t2);
      if (n.startsWith("(")) {
        n = n.substring(1, n.length() - 1);
      }
      String[] frac = n.split("/");
      if (frac.length == 1) {
        // cannot multiply with term 1/n because the result will have type rat instead of int
        return super.linearDivide(pNumber1, pNumber2);
      } else {
        assert (frac.length == 2);
        n = frac[1] + "/" + frac[0];
      }
      t2 = msat_make_number(mathsatEnv, n);
      return msat_make_times(mathsatEnv, t2, t1);
  }

  @Override
  protected Long modularCongruence(Long pNumber1, Long pNumber2, long pModulo) {
    if (pModulo > 0) {
      return msat_make_int_modular_congruence(getFormulaCreator().getEnv(),
          pModulo, pNumber1, pNumber2);
    }
    return msat_make_true(getFormulaCreator().getEnv());
  }
}
