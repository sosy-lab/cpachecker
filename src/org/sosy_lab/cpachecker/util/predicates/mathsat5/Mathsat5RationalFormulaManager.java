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
package org.sosy_lab.cpachecker.util.predicates.mathsat5;

import static org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5NativeApi.*;

import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;

class Mathsat5RationalFormulaManager extends Mathsat5NumeralFormulaManager<NumeralFormula, RationalFormula> {

  public Mathsat5RationalFormulaManager(
          Mathsat5FormulaCreator pCreator,
          Mathsat5FunctionFormulaManager functionManager) {
    super(pCreator, functionManager, RationalFormula.class);
  }

  @Override
  protected long getNumeralType() {
    return getFormulaCreator().getRealType();
  }

  @Override
  public FormulaType<? extends NumeralFormula> getFormulaType() {
    return FormulaType.RationalType;
  }

  @Override
  public Long divide(Long pNumber1, Long pNumber2) {
    long mathsatEnv = getFormulaCreator().getEnv();
    long t1 = pNumber1;
    long t2 = pNumber2;

    long result;
    if (msat_term_is_number(mathsatEnv, t2)) {
      // invert t2 and multiply with it
      String n = msat_term_repr(t2);
      if (n.startsWith("(")) {
        n = n.substring(1, n.length() - 1);
      }
      String[] frac = n.split("/");
      if (frac.length == 1) {
        n = "1/" + n;
      } else {
        assert (frac.length == 2);
        n = frac[1] + "/" + frac[0];
      }
      t2 = msat_make_number(mathsatEnv, n);
      result = msat_make_times(mathsatEnv, t2, t1);
    } else {
      result = makeUf(divUfDecl, t1, t2);
    }

    return result;
  }
}
