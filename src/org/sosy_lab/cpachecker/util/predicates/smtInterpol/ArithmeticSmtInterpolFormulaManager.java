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
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.predicates.FormulaOperator;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.smtInterpol.SmtInterpolEnvironment.Type;

import com.google.common.collect.Sets;

import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.FunctionSymbol;
import de.uni_freiburg.informatik.ultimate.logic.Logics;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;

/**
 * Implementation of SmtInterpolFormulaManager for formulas with the theories of
 * real or integer linear arithmetic.
 */
public class ArithmeticSmtInterpolFormulaManager extends SmtInterpolFormulaManager {

  private final boolean useIntegers;

  // UF encoding of some unsupported operations
  // TODO there are some bitVektor-functions in smtinterpol.theory, can we use them?
  // TODO why use "_" in functionNames?
  private final String bitwiseAndUfDecl = "_&_";
  private final String bitwiseOrUfDecl = "_OR_";
  private final String bitwiseXorUfDecl = "_^_";
  private final String bitwiseNotUfDecl = "_~_";
  private final String leftShiftUfDecl = "_<<_";
  private final String rightShiftUfDecl = "_>>_";
  private final String multUfDecl = "_*_";
  private final String divUfDecl = "_/_";
  private final String modUfDecl = "_%_";

  public ArithmeticSmtInterpolFormulaManager(Configuration config, LogManager logger, boolean pUseIntegers)
      throws InvalidConfigurationException {
    super(config, logger, pUseIntegers ? Type.INT : Type.REAL);
    useIntegers = pUseIntegers;
    initBasics(env);
  }

  @Override
  SmtInterpolEnvironment createEnvironment() {
    SmtInterpolEnvironment newEnv = super.createEnvironment();
    return newEnv;
  }

  /** set logic and declare some useful functions */
  private void initBasics(SmtInterpolEnvironment e) {
    final Sort sortType;
    if (useIntegers) {
      e.setLogic(Logics.QF_UFLIA);
      sortType = e.sort(Type.INT);
    } else {
      e.setLogic(Logics.QF_UFLRA);
      sortType = e.sort(Type.REAL);
    }

    final Sort[] sortArray1 = { sortType };
    final Sort[] sortArray2 = { sortType, sortType };

    e.declareFun(bitwiseAndUfDecl, sortArray2, sortType);
    e.declareFun(bitwiseOrUfDecl, sortArray2, sortType);
    e.declareFun(bitwiseXorUfDecl, sortArray2, sortType);
    e.declareFun(bitwiseNotUfDecl, sortArray1, sortType);
    e.declareFun(leftShiftUfDecl, sortArray2, sortType);
    e.declareFun(rightShiftUfDecl, sortArray2, sortType);
    e.declareFun(multUfDecl, sortArray2, sortType);
    e.declareFun(divUfDecl, sortArray2, sortType);
    e.declareFun(modUfDecl, sortArray2, sortType);
  }

  // ----------------- Numeric formulas -----------------

  @Override
  public Formula makeNegate(Formula f) {
    return encapsulate(env.term("*", env.numeral("-1"), getTerm(f)));
  }

  @Override
  public Formula makeNumber(int i) {
    return makeNumber(Integer.toString(i));
  }

  @Override
  public Formula makeNumber(String i) {
    return encapsulate(env.decimal(i));
  }

  @Override
  public Formula makePlus(Formula f1, Formula f2) {
    return encapsulate(env.term("+", getTerm(f1), getTerm(f2)));
  }

  @Override
  public Formula makeMinus(Formula f1, Formula f2) {
    return encapsulate(env.term("-", getTerm(f1), getTerm(f2)));
  }

  @Override
  public Formula makeDivide(Formula f1, Formula f2) {
    assert !useIntegers : "divisions not possible in integer-logic.";

    Term t1 = getTerm(f1);
    Term t2 = getTerm(f2);
    Term result = null;
    if (isNumber(t2)) {
      result = env.term("/", t1, t2);
    } else {
      result = env.term(divUfDecl, t1, t2);
    }
    return encapsulate(result);
  }

  @Override
  public Formula makeModulo(Formula f1, Formula f2) {
    return encapsulate(env.term(modUfDecl, getTerm(f1), getTerm(f2)));
  }

  @Override
  public Formula makeMultiply(Formula f1, Formula f2) {
    Term t1 = getTerm(f1);
    Term t2 = getTerm(f2);

    Term result = null;
    if (isNumber(t1) || isNumber(t2)) { // TODO: both not numeral?
      result = env.term("*", t1, t2);
    } else {
      result = env.term(multUfDecl, t1, t2);
    }

    return encapsulate(result);
  }

  // ----------------- Numeric relations -----------------

  @Override
  public Formula makeEqual(Formula f1, Formula f2) {
    return encapsulate(env.term("=", getTerm(f1), getTerm(f2)));
  }

  @Override
  public Formula makeGt(Formula f1, Formula f2) {
    return encapsulate(env.term(">", getTerm(f1), getTerm(f2)));
  }

  @Override
  public Formula makeGeq(Formula f1, Formula f2) {
    return encapsulate(env.term(">=", getTerm(f1), getTerm(f2)));
  }

  @Override
  public Formula makeLt(Formula f1, Formula f2) {
    return encapsulate(env.term("<", getTerm(f1), getTerm(f2)));
  }

  @Override
  public Formula makeLeq(Formula f1, Formula f2) {
    return encapsulate(env.term("<=", getTerm(f1), getTerm(f2)));
  }

  // ----------------- Bit-manipulation functions -----------------

  @Override
  public Formula makeBitwiseNot(Formula f) {
    return encapsulate(env.term(bitwiseNotUfDecl, getTerm(f)));
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
    return encapsulate(env.term(uifDecl, getTerm(f1), getTerm(f2)));
  }

  // ----------------- Complex formula manipulation -----------------

  // returns a formula with some "static learning" about some bitwise
  // operations, so that they are (a bit) "less uninterpreted"
  // Currently it add's the following formulas for each number literal n that
  // appears in the formula: "(n & 0 == 0) and (0 & n == 0)"
  // But only if an bitwise "and" occurs in the formula.
  @Override
  public Formula getBitwiseAxioms(Formula f) {
    Deque<Term> toProcess = new ArrayDeque<Term>();
    Set<Term> seen = Sets.newHashSet();
    Set<Term> allLiterals = Sets.newHashSet();

    boolean andFound = false;

    toProcess.add(getTerm(f));
    while (!toProcess.isEmpty()) {
      final Term t = toProcess.pollLast();

      if (isNumber(t)) {
        allLiterals.add(t);
      }
      if (uifs.contains(t)) {
        FunctionSymbol funcSym = ((ApplicationTerm) t).getFunction();
        andFound = bitwiseAndUfDecl.equals(funcSym.getName());
      }
      for (Term c : getArgs(t)) {
        if (seen.add(c)) {
          // was not already contained in seen
          toProcess.add(c);
        }
      }
    }

    Term result = getTrueTerm();
    if (andFound) {
      Term z = env.numeral("0");
      for (Term n : allLiterals) {
        Term u1 = env.term(bitwiseAndUfDecl, n, z);
        Term u2 = env.term(bitwiseAndUfDecl, z, n);
        Term e1;
        e1 = env.term("=", u1, z);
        Term e2 = env.term("=", u2, z);
        Term a = env.term("and", e1, e2);

        result = env.term("and", result, a);
      }
    }
    return encapsulate(result);
  }

  @Override
  public Formula[] getArguments(Formula f) {
    Term t = getTerm(f);
    assert t instanceof ApplicationTerm;
    Term[] params = ((ApplicationTerm) t).getParameters();
    Formula[] formulas = new Formula[params.length];
    for (int i = 0; i < params.length; i++) {
      formulas[i] = encapsulate(params[i]);
    }
    return formulas;
  }

  @Override
  public FormulaOperator getOperator(Formula f) {
    Term t = getTerm(f);
    assert t instanceof ApplicationTerm;
    ((ApplicationTerm) t).getParameters();
    ;
    String funcN = ((ApplicationTerm) t).getFunction().getName();
    if (funcN.equals("not")) { return FormulaOperator.NOT; }
    if (funcN.equals("and")) { return FormulaOperator.AND; }
    if (funcN.equals("or")) { return FormulaOperator.OR; }
    if (funcN.equals("=")) { return FormulaOperator.EQUIV; }
    if (funcN.equals("ite")) { return FormulaOperator.ITE; }
    if (isAnd(t)) { return FormulaOperator.ATOM; }
    return null;
  }

  @Override
  public Map<String, Formula> parseFormulas(String pS) throws IllegalArgumentException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String dumpFormulas(Map<String, Formula> pFormulas) {
    throw new UnsupportedOperationException();
  }
}