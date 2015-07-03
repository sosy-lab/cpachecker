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
package org.sosy_lab.cpachecker.util.predicates.smtInterpol;

import static org.sosy_lab.cpachecker.util.predicates.smtInterpol.SmtInterpolUtil.isNumber;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractNumeralFormulaManager;

import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;


abstract class SmtInterpolNumeralFormulaManager
        <ParamFormulaType extends NumeralFormula, ResultFormulaType extends NumeralFormula>
        extends AbstractNumeralFormulaManager<Term, Sort, SmtInterpolEnvironment, ParamFormulaType, ResultFormulaType> {

  private final SmtInterpolEnvironment env;

  SmtInterpolNumeralFormulaManager(
          SmtInterpolFormulaCreator pCreator,
          SmtInterpolFunctionFormulaManager pFunctionManager) {
    super(pCreator, pFunctionManager);
    env = pCreator.getEnv();
  }

  @Override
  public Term negate(Term pNumber) {
    return env.term("*", env.numeral("-1"), pNumber);
  }

  @Override
  public Term add(Term pNumber1, Term pNumber2) {
    return env.term("+", pNumber1, pNumber2);
  }

  @Override
  public Term subtract(Term pNumber1, Term pNumber2) {
    return env.term("-", pNumber1, pNumber2);
  }

  @Override
  public Term divide(Term pNumber1, Term pNumber2) {
    Term result;
    if (isNumber(pNumber2)) {
      Sort intSort = pNumber1.getTheory().getNumericSort();
      Sort realSort = pNumber1.getTheory().getRealSort();
      if (intSort.equals(pNumber1.getSort()) && intSort.equals(pNumber2.getSort())) {
        Term div = env.term("div", pNumber1, pNumber2);
        // C99 truncates towards 0
        result = env.term("mod", pNumber1, pNumber2);
        result = env.term("ite",
            greaterOrEquals(pNumber1, env.numeral(BigInteger.ZERO)),
            div,
            add(div, env.numeral(BigInteger.ONE)));
      } else {
        assert intSort.equals(pNumber1.getSort()) || realSort.equals(pNumber1.getSort());
        assert intSort.equals(pNumber2.getSort()) || realSort.equals(pNumber2.getSort());
        result = env.term("/", pNumber1, pNumber2);
      }
    } else {
      result = super.divide(pNumber1, pNumber2);
    }

    return result;
  }

  @Override
  public Term multiply(Term pNumber1, Term pNumber2) {
    Term result;
    if (isNumber(pNumber1) || isNumber(pNumber2)) {
      result = env.term("*", pNumber1, pNumber2);
    } else {
      result = super.multiply(pNumber1, pNumber2);
    }

    return result;
  }

  @Override
  protected Term modulo(Term pNumber1, Term pNumber2) {
    Term result;
    Sort intSort = pNumber1.getTheory().getNumericSort();
    if (isNumber(pNumber2)
        && intSort.equals(pNumber1.getSort())
        && intSort.equals(pNumber2.getSort())) {
      Term mod = env.term("mod", pNumber1, pNumber2);
      // C99 truncates towards 0 on division
      result = env.term("ite",
          greaterOrEquals(pNumber1, env.numeral(BigInteger.ZERO)),
          mod,
          subtract(mod, pNumber2));
    } else {
      result = super.modulo(pNumber1, pNumber2);
    }

    return result;
  }

  @Override
  protected Term modularCongruence(Term pNumber1, Term pNumber2, long pModulo) {
    // if x >= 0: ((_ divisible n) x)   <==>   (= x (* n (div x n)))
    // if x <  0: ((_ divisible n) x)   <==>   (= x (* n (div x n)))
    Sort intSort = pNumber1.getTheory().getNumericSort();
    if (pModulo > 0
        && intSort.equals(pNumber1.getSort())
        && intSort.equals(pNumber2.getSort())) {
      Term n = env.numeral(BigInteger.valueOf(pModulo));
      Term x = subtract(pNumber1, pNumber2);
      return env.term("=", x, env.term("*", n, env.term("div", x, n)));
    }
    return env.getTheory().mTrue;
  }

  @Override
  public Term equal(Term pNumber1, Term pNumber2) {
    return env.term("=", pNumber1, pNumber2);
  }

  @Override
  public Term greaterThan(Term pNumber1, Term pNumber2) {
    return env.term(">", pNumber1, pNumber2);
  }

  @Override
  public Term greaterOrEquals(Term pNumber1, Term pNumber2) {
    return env.term(">=", pNumber1, pNumber2);
  }

  @Override
  public Term lessThan(Term pNumber1, Term pNumber2) {
    return env.term("<", pNumber1, pNumber2);
  }

  @Override
  public Term lessOrEquals(Term pNumber1, Term pNumber2) {
    return env.term("<=", pNumber1, pNumber2);
  }
}
