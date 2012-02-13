/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import static org.sosy_lab.cpachecker.util.predicates.smtInterpol.SmtInterpolUtil.*;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;

import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.FunctionSymbol;
import de.uni_freiburg.informatik.ultimate.logic.Logics;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;

/**
 * Implementation of SmtInterpolFormulaManager for formulas with the theories of
 * real or integer linear arithmetic.
 */
class ArithmeticSmtInterpolFormulaManager extends SmtInterpolFormulaManager {

  private final static boolean useIntegers = true; // TODO set value with option?
  private final static String SMT_INTERPOL_INT = "Int";
  private final static String SMT_INTERPOL_REAL = "Real";

  // UF encoding of some unsupported operations
  // TODO there are some bitVektor-functions in smtinterpol.theory, can we use them?
  // TODO why use "_" in functionNames?
  private final String bitwiseAndUfDecl = "_&_";
  private final String bitwiseOrUfDecl = "_|_";
  private final String bitwiseXorUfDecl = "_^_";
  private final String bitwiseNotUfDecl = "_~_";
  private final String leftShiftUfDecl = "_<<_";
  private final String rightShiftUfDecl = "_>>_";
  private final String multUfDecl = "_*_";
  private final String divUfDecl = "_/_";
  private final String modUfDecl = "_%_";

  ArithmeticSmtInterpolFormulaManager(Configuration config, LogManager logger) throws InvalidConfigurationException {
    super(config, logger, useIntegers ? SMT_INTERPOL_INT : SMT_INTERPOL_REAL);

    final Sort sortType;
    if (useIntegers) {
      script.setLogic(Logics.QF_UFLIA.toString());
      sortType = script.getTheory().getSort(SMT_INTERPOL_INT);
    } else {
      script.setLogic(Logics.QF_UFLRA.toString());
      sortType = script.getTheory().getSort(SMT_INTERPOL_REAL);
    }
    super.sort = sortType.getName();

    final Sort[] sortArray1 = { sortType };
    final Sort[] sortArray2 = { sortType, sortType };

    try {
      script.declareFun(bitwiseAndUfDecl, sortArray2, sortType);
      script.declareFun(bitwiseOrUfDecl, sortArray2, sortType);
      script.declareFun(bitwiseXorUfDecl, sortArray2, sortType);
      script.declareFun(bitwiseNotUfDecl, sortArray1, sortType);
      script.declareFun(leftShiftUfDecl, sortArray2, sortType);
      script.declareFun(rightShiftUfDecl, sortArray2, sortType);
      script.declareFun(multUfDecl, sortArray2, sortType);
      script.declareFun(divUfDecl, sortArray2, sortType);
      script.declareFun(modUfDecl, sortArray2, sortType);
    } catch (SMTLIBException e) {
      e.printStackTrace();
    }
  }

  @Override
  Script getEnvironment() {
    Script script = super.getEnvironment();

    if (useIntegers) {
      if (script.getTheory().getLogic() != Logics.QF_UFLIA) // TODO other logic?
        script.setLogic(Logics.QF_UFLIA.toString());
    } else {
      if (script.getTheory().getLogic() != Logics.QF_UFLRA) // TODO other logic?
        script.setLogic(Logics.QF_UFLRA.toString());
    }

    return script;
  }

  // ----------------- Numeric formulas -----------------

  @Override
  public Formula makeNegate(Formula f) {
    try {
      return encapsulate(script.term("*", script.numeral("-1"), getTerm(f)));
    } catch (SMTLIBException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Formula makeNumber(int i) {
    return makeNumber(Integer.toString(i));
  }

  @Override
  public Formula makeNumber(String i) { // TODO test: only Integers or more?
    try {
      return encapsulate(script.numeral(i));
    } catch (SMTLIBException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Formula makePlus(Formula f1, Formula f2) {
    try {
      return encapsulate(script.term("+", getTerm(f1), getTerm(f2)));
    } catch (SMTLIBException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Formula makeMinus(Formula f1, Formula f2) {
    try {
      return encapsulate(script.term("-", getTerm(f1), getTerm(f2)));
    } catch (SMTLIBException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Formula makeDivide(Formula f1, Formula f2) {
    assert script.getTheory().getLogic() != Logics.QF_UFLIA :
      "divisions not possible in integer-logic.";

    Term t1 = getTerm(f1);
    Term t2 = getTerm(f2);
    Term result = null;
    if (isNumber(t2)) {
      try {
        result = script.term("/", t1, t2);
      } catch (SMTLIBException e) {
        e.printStackTrace();
      }
    } else {
      result = buildUF(divUfDecl, t1, t2);
    }
    return encapsulate(result);
  }

  @Override
  public Formula makeModulo(Formula f1, Formula f2) {
    try {
      return encapsulate(script.term(modUfDecl, getTerm(f1), getTerm(f2)));
    } catch (SMTLIBException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Formula makeMultiply(Formula f1, Formula f2) {
    Term t1 = getTerm(f1);
    Term t2 = getTerm(f2);

    Term result = null;
    if (isNumber(t1) || isNumber(t2)) { // TODO: both not numeral?
      try {
        result = script.term("*", t1, t2);
      } catch (SMTLIBException e) {
        e.printStackTrace();
      }
    } else {
      result = buildUF(multUfDecl, t1, t2);
    }

    return encapsulate(result);
  }

  // ----------------- Numeric relations -----------------

  @Override
  public Formula makeEqual(Formula f1, Formula f2) {
    return this.makeEquivalence(f1, f2); // TODO working?
  }

  @Override
  public Formula makeGt(Formula f1, Formula f2) {
    try {
      return encapsulate(script.term(">", getTerm(f1), getTerm(f2)));
    } catch (SMTLIBException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Formula makeGeq(Formula f1, Formula f2) {
    try {
      return encapsulate(script.term(">=", getTerm(f1), getTerm(f2)));
    } catch (SMTLIBException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Formula makeLt(Formula f1, Formula f2) {
    try {
      return encapsulate(script.term("<", getTerm(f1), getTerm(f2)));
    } catch (SMTLIBException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Formula makeLeq(Formula f1, Formula f2) {
    try {
      return encapsulate(script.term("<=", getTerm(f1), getTerm(f2)));
    } catch (SMTLIBException e) {
      e.printStackTrace();
      return null;
    }
  }

  // ----------------- Bit-manipulation functions -----------------

  @Override
  public Formula makeBitwiseNot(Formula f) {
    return encapsulate(buildUF(bitwiseNotUfDecl, getTerm(f)));
  }

  @Override
  public Formula makeBitwiseAnd(Formula f1, Formula f2) {
    return makeUIFforBinaryOperator(f1, f2, bitwiseAndUfDecl);
  }

  @Override
  public Formula makeBitwiseOr(Formula f1, Formula f2) {
    return makeUIFforBinaryOperator(f1, f2, bitwiseOrUfDecl);
  }

  @Override
  public Formula makeBitwiseXor(Formula f1, Formula f2) {
    return makeUIFforBinaryOperator(f1, f2, bitwiseXorUfDecl);
  }

  @Override
  public Formula makeShiftLeft(Formula f1, Formula f2) {
    return makeUIFforBinaryOperator(f1, f2, leftShiftUfDecl);
  }

  @Override
  public Formula makeShiftRight(Formula f1, Formula f2) {
    return makeUIFforBinaryOperator(f1, f2, rightShiftUfDecl);
  }

  //----------------- Uninterpreted functions -----------------

  private Formula makeUIFforBinaryOperator(Formula f1, Formula f2, String uifDecl) {
    return encapsulate(buildUF(uifDecl, getTerm(f1), getTerm(f2)));
  }

  // ----------------- Complex formula manipulation -----------------

  // returns a formula with some "static learning" about some bitwise
  // operations, so that they are (a bit) "less uninterpreted"
  // Currently it add's the following formulas for each number literal n that
  // appears in the formula: "(n & 0 == 0) and (0 & n == 0)"
  // But only if an bitwise "and" occurs in the formula.
  @Override
  public Formula getBitwiseAxioms(Formula f) {
    Deque<Formula> toProcess = new ArrayDeque<Formula>();
    Set<Formula> seen = new HashSet<Formula>();
    Set<Formula> allLiterals = new HashSet<Formula>();

    boolean andFound = false;

    toProcess.add(f);
    while (!toProcess.isEmpty()) {
      final Formula tt = toProcess.pollLast();
      final Term t = getTerm(tt);

      if (isNumber(t)) {
        allLiterals.add(tt);
      }
      if (isUIF(script, t)) {
        FunctionSymbol funcSym = ((ApplicationTerm) t).getFunction();
        andFound = bitwiseAndUfDecl.equals(funcSym.getName());
      }
      int arity = getArity(t);
      for (int i = 0; i < arity; ++i) {
        Formula c = encapsulate(getArg(t, i));
        if (seen.add(c)) {
          // was not already contained in seen
          toProcess.add(c);
        }
      }
    }

    try {
      Term result = script.getTheory().TRUE;
      if (andFound) {
        Term z = script.numeral("0");
        for (Formula nn : allLiterals) {
          Term n = getTerm(nn);
          Term u1 = buildUF(bitwiseAndUfDecl, n, z);
          Term u2 = buildUF(bitwiseAndUfDecl, z, n);
          Term e1;
          e1 = script.term("=", u1, z);
          Term e2 = script.term("=", u2, z);
          Term a = script.term("and", e1, e2);

          result = script.term("and", result, a);
        }
      }
      return encapsulate(result);

    } catch (SMTLIBException e) {
      e.printStackTrace();
      return null;
    }
  }
}