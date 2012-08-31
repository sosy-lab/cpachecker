/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.mathsat;

import static org.sosy_lab.cpachecker.util.predicates.mathsat.NativeApi.*;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaList;

/**
 * Implementation of MathsatFormulaManager for formulas with the theories of
 * real or integer linear arithmetic.
 */
public class ArithmeticMathsatFormulaManager extends MathsatFormulaManager {

  private final boolean useIntegers;

  // UF encoding of some unsupported operations
  private final long bitwiseAndUfDecl;
  private final long bitwiseOrUfDecl;
  private final long bitwiseXorUfDecl;
  private final long bitwiseNotUfDecl;
  private final long leftShiftUfDecl;
  private final long rightShiftUfDecl;
  private final long multUfDecl;
  private final long divUfDecl;
  private final long modUfDecl;

  public ArithmeticMathsatFormulaManager(Configuration config, LogManager logger, boolean pUseIntegers) throws InvalidConfigurationException {
    super(config, logger, pUseIntegers ? MSAT_INT : MSAT_REAL);

    useIntegers = pUseIntegers;

    final int msatVarType = pUseIntegers ? MSAT_INT : MSAT_REAL;
    final int[] msatVarType1 = {msatVarType};
    final int[] msatVarType2 = {msatVarType, msatVarType};

    bitwiseAndUfDecl = msat_declare_uif(msatEnv, "_&_", msatVarType, msatVarType2);
    bitwiseOrUfDecl = msat_declare_uif(msatEnv, "_|_", msatVarType, msatVarType2);
    bitwiseXorUfDecl = msat_declare_uif(msatEnv, "_^_", msatVarType, msatVarType2);
    bitwiseNotUfDecl = msat_declare_uif(msatEnv, "_~_", msatVarType, msatVarType1);
    leftShiftUfDecl = msat_declare_uif(msatEnv, "_<<_", msatVarType, msatVarType2);
    rightShiftUfDecl = msat_declare_uif(msatEnv, "_>>_", msatVarType, msatVarType2);
    multUfDecl = msat_declare_uif(msatEnv, "_*_", msatVarType, msatVarType2);
    divUfDecl = msat_declare_uif(msatEnv, "_/_", msatVarType, msatVarType2);
    modUfDecl = msat_declare_uif(msatEnv, "_%_", msatVarType, msatVarType2);
  }

  @Override
  long createEnvironment(boolean pShared, boolean pGhostFilter) {
    long env = super.createEnvironment(pShared, pGhostFilter);

    if (useIntegers) {
      msat_add_theory(env, MSAT_LIA);
      int ok = msat_set_option(env, "split_eq", "false");
      assert(ok == 0);
    } else {
      msat_add_theory(env, MSAT_LRA);
    }

    return env;
  }

  @Override
  long interpreteBitvector(long pBv) {
    throw new UnsupportedOperationException("Bitvector not expected");
  }

  // ----------------- Numeric formulas -----------------

  @Override
  public Formula makeNegate(Formula f) {
    return encapsulate(msat_make_negate(msatEnv, getTerm(f)));
  }

  @Override
  public Formula makeNumber(int i) {
    return makeNumber(Integer.toString(i));
  }

  @Override
  public Formula makeNumber(String i) {
    return encapsulate(msat_make_number(msatEnv, i));
  }

  @Override
  public Formula makePlus(Formula f1, Formula f2) {
    return encapsulate(msat_make_plus(msatEnv, getTerm(f1), getTerm(f2)));
  }

  @Override
  public Formula makeMinus(Formula f1, Formula f2) {
    return encapsulate(msat_make_minus(msatEnv, getTerm(f1), getTerm(f2)));
  }

  @Override
  public Formula makeDivide(Formula f1, Formula f2) {
    long t1 = getTerm(f1);
    long t2 = getTerm(f2);

    long result;
    if (msat_term_is_number(t2) != 0) {
      // invert t2 and multiply with it
      String n = msat_term_repr(t2);
      if (n.startsWith("(")) {
        n = n.substring(1, n.length()-1);
      }
      String[] frac = n.split("/");
      if (frac.length == 1) {
        n = "1/" + n;
      } else {
        assert(frac.length == 2);
        n = frac[1] + "/" + frac[0];
      }
      t2 = msat_make_number(msatEnv, n);
      result = msat_make_times(msatEnv, t2, t1);
    } else {
      result = buildMsatUF(divUfDecl, new long[]{t1, t2});
    }
    return encapsulate(result);
  }

  @Override
  public Formula makeModulo(Formula f1, Formula f2) {
    return makeUIFforBinaryOperator(f1, f2, modUfDecl);
  }

  @Override
  public Formula makeMultiply(Formula f1, Formula f2) {
    long t1 = getTerm(f1);
    long t2 = getTerm(f2);

    long result;
    if (msat_term_is_number(t1) != 0) {
      result = msat_make_times(msatEnv, t1, t2);
    } else if (msat_term_is_number(t2) != 0) {
      result = msat_make_times(msatEnv, t2, t1);
    } else {
      result = buildMsatUF(multUfDecl, new long[]{t1, t2});
    }

    return encapsulate(result);
  }

  // ----------------- Numeric relations -----------------

  @Override
  public Formula makeEqual(Formula f1, Formula f2) {
    return encapsulate(msat_make_equal(msatEnv, getTerm(f1), getTerm(f2)));
  }

  @Override
  public Formula makeGt(Formula f1, Formula f2) {
    return encapsulate(msat_make_gt(msatEnv, getTerm(f1), getTerm(f2)));
  }

  @Override
  public Formula makeGeq(Formula f1, Formula f2) {
    return encapsulate(msat_make_geq(msatEnv, getTerm(f1), getTerm(f2)));
  }

  @Override
  public Formula makeLt(Formula f1, Formula f2) {
    return encapsulate(msat_make_lt(msatEnv, getTerm(f1), getTerm(f2)));
  }

  @Override
  public Formula makeLeq(Formula f1, Formula f2) {
    return encapsulate(msat_make_leq(msatEnv, getTerm(f1), getTerm(f2)));
  }

  // ----------------- Bit-manipulation functions -----------------

  @Override
  public Formula makeBitwiseNot(Formula f) {
    long[] args = {getTerm(f)};

    return encapsulate(buildMsatUF(bitwiseNotUfDecl, args));
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


  // ----------------- Uninterpreted functions -----------------

  private Formula makeUIFforBinaryOperator(Formula f1, Formula f2, long uifDecl) {
    long[] args = {getTerm(f1), getTerm(f2)};

    return encapsulate(buildMsatUF(uifDecl, args));
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
      final long t = getTerm(tt);

      if (msat_term_is_number(t) != 0) {
        allLiterals.add(tt);
      }
      if (msat_term_is_uif(t) != 0) {
        String r = msat_term_repr(t);
        if (r.startsWith("_&_")) {
          andFound = true;
        }
      }
      int arity = msat_term_arity(t);
      for (int i = 0; i < arity; ++i) {
        Formula c = encapsulate(msat_term_get_arg(t, i));
        if (seen.add(c)) {
          // was not already contained in seen
          toProcess.add(c);
        }
      }
    }

    long result = msat_make_true(msatEnv);
    if (andFound) {
      long z = msat_make_number(msatEnv, "0");
      for (Formula nn : allLiterals) {
        long n = getTerm(nn);
        long u1 = buildMsatUF(bitwiseAndUfDecl, new long[]{n, z});
        long u2 = buildMsatUF(bitwiseAndUfDecl, new long[]{z, n});
        long e1 = msat_make_equal(msatEnv, u1, z);
        long e2 = msat_make_equal(msatEnv, u2, z);
        long a = msat_make_and(msatEnv, e1, e2);
        result = msat_make_and(msatEnv, result, a);
      }
    }
    return encapsulate(result);
  }

  @Override
  public FormulaList parseList(String pS) throws IllegalArgumentException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String dumpFormulaList(FormulaList pFlist) {
    // TODO Auto-generated method stub
    return null;
  }

}
