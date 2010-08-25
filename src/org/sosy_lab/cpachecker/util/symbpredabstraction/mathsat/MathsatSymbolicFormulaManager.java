/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.symbpredabstraction.PathFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.SSAMap;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.TheoremProver;

import com.google.common.base.Preconditions;

@Options(prefix="cpas.symbpredabs")
public class MathsatSymbolicFormulaManager implements SymbolicFormulaManager  {

  @Option(name="mathsat.useIntegers")
  private boolean useIntegers = false;

  @Option(name="mathsat.useDtc")
  private boolean useDtc = false;

  @Option
  private boolean useBitwiseAxioms = false;
  
  // the MathSAT environment in which all terms are created
  private final long msatEnv;

  // We need to distinguish assignments from tests. This is needed to
  // build a formula in SSA form later on, when we have to mapback
  // a counterexample, without adding too many extra variables. Therefore,
  // in the representation of "uninstantiated" symbolic formulas, we
  // use a new binary uninterpreted function ":=" to represent
  // assignments. When we instantiate the formula, we replace this UIF
  // with an equality, because now we have an SSA form
  private final long assignUfDecl;

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
  private final long stringLitUfDecl;

  // datatype to use for variables, when converting them to mathsat vars
  // can be either MSAT_REAL or MSAT_INT
  // Note that MSAT_INT does not mean that we support the full linear
  // integer arithmetic (LIA)! At the moment, interpolation doesn't work on
  // LIA, only difference logic or on LRA (i.e. on the rationals). However
  // by setting the vars to be MSAT_INT, the solver tries some heuristics
  // that might work (e.g. tightening of a < b into a <= b - 1, splitting
  // negated equalities, ...)
  private final int msatVarType;
  
  // the character for separating name and index of a value
  private static final String INDEX_SEPARATOR = "@";

  // various caches for speeding up expensive tasks
  //
  // cache for splitting arithmetic equalities in extractAtoms
  private final Map<Long, Boolean> arithCache = new HashMap<Long, Boolean>();

  // cache for uninstantiating terms (see uninstantiate() below)
  private final Map<Long, Long> uninstantiateCache = new HashMap<Long, Long>();

  // cache for replacing assignments
  private final Map<Long, Long> replaceAssignmentsCache = new HashMap<Long, Long>();

  protected final LogManager logger;
  private final MathsatAbstractionPrinter absPrinter;  
  
  public MathsatSymbolicFormulaManager(Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this, MathsatSymbolicFormulaManager.class);
    this.logger = logger;
    msatEnv = mathsat.api.msat_create_env();
    if (useIntegers) {
      msatVarType = mathsat.api.MSAT_INT;
    } else {
      msatVarType = mathsat.api.MSAT_REAL;
    }

    final int[] msatVarType1 = new int[]{msatVarType};
    final int[] msatVarType2 = {msatVarType, msatVarType};
    assignUfDecl = mathsat.api.msat_declare_uif(msatEnv, ":=",
        mathsat.api.MSAT_BOOL, 2, msatVarType2);

    bitwiseAndUfDecl = mathsat.api.msat_declare_uif(msatEnv, "_&_",
        msatVarType, 2, msatVarType2);
    bitwiseOrUfDecl = mathsat.api.msat_declare_uif(msatEnv, "_|_",
        msatVarType, 2, msatVarType2);
    bitwiseXorUfDecl = mathsat.api.msat_declare_uif(msatEnv, "_^_",
        msatVarType, 2, msatVarType2);
    bitwiseNotUfDecl = mathsat.api.msat_declare_uif(msatEnv, "_~_",
        msatVarType, 1, msatVarType1);
    leftShiftUfDecl = mathsat.api.msat_declare_uif(msatEnv, "_<<_",
        msatVarType, 2, msatVarType2);
    rightShiftUfDecl = mathsat.api.msat_declare_uif(msatEnv, "_>>_",
        msatVarType, 2, msatVarType2);
    multUfDecl = mathsat.api.msat_declare_uif(msatEnv, "_*_",
        msatVarType, 2, msatVarType2);
    divUfDecl = mathsat.api.msat_declare_uif(msatEnv, "_/_",
        msatVarType, 2, msatVarType2);
    modUfDecl = mathsat.api.msat_declare_uif(msatEnv, "_%_",
        msatVarType, 2, msatVarType2);

    stringLitUfDecl = mathsat.api.msat_declare_uif(msatEnv, "__string__",
        msatVarType, 1, msatVarType1);
    
    absPrinter = new MathsatAbstractionPrinter(msatEnv, "abs", logger);
  }

  long getMsatEnv() {
    return msatEnv;
  }

  long createEnvironment(boolean shared, boolean ghostFilter) {
    long env;
    if (shared) {
      env = mathsat.api.msat_create_shared_env(msatEnv);
    } else {
      env = mathsat.api.msat_create_env();
    }
    
    mathsat.api.msat_add_theory(env, mathsat.api.MSAT_UF);
    if (useIntegers) {
      mathsat.api.msat_add_theory(env, mathsat.api.MSAT_LIA);
      int ok = mathsat.api.msat_set_option(env, "split_eq", "false");
      assert(ok == 0);
    } else {
      mathsat.api.msat_add_theory(env, mathsat.api.MSAT_LRA);
    }
    if (useDtc) {
      mathsat.api.msat_set_theory_combination(env, mathsat.api.MSAT_COMB_DTC);
    }
    // disable static learning. For small problems, this is just overhead
    mathsat.api.msat_set_option(env, "sl", "0");
    
    if (ghostFilter) {
      mathsat.api.msat_set_option(env, "ghost_filter", "true");
    }
    
    return env;
  }
  
  private long[] getTerm(SymbolicFormula[] f) {
    int length = f.length;
    long[] result = new long[length];
    for (int i = 0; i < length; i++) {
      result[i] = ((MathsatSymbolicFormula)f[i]).getTerm();
    }
    return result;
  }
   
  
  // ----------------- Boolean formulas -----------------
  
  @Override
  public boolean isBoolean(SymbolicFormula f) {
    long t = ((MathsatSymbolicFormula)f).getTerm();
    
    return mathsat.api.msat_term_get_type(t) == mathsat.api.MSAT_BOOL;
  }
  
  @Override
  public SymbolicFormula makeTrue() {
    return new MathsatSymbolicFormula(mathsat.api.msat_make_true(msatEnv));
  }
  
  @Override
  public SymbolicFormula makeFalse() {
    return new MathsatSymbolicFormula(mathsat.api.msat_make_false(msatEnv));
  }

  @Override
  public SymbolicFormula makeNot(SymbolicFormula f) {
    MathsatSymbolicFormula m = (MathsatSymbolicFormula)f;

    long a = mathsat.api.msat_make_not(msatEnv, m.getTerm());
    return new MathsatSymbolicFormula(a);
  }

  @Override
  public SymbolicFormula makeAnd(SymbolicFormula f1, SymbolicFormula f2) {
    MathsatSymbolicFormula m1 = (MathsatSymbolicFormula)f1;
    MathsatSymbolicFormula m2 = (MathsatSymbolicFormula)f2;

    long a = mathsat.api.msat_make_and(msatEnv, m1.getTerm(), m2.getTerm());
    return new MathsatSymbolicFormula(a);
  }

  @Override
  public SymbolicFormula makeOr(SymbolicFormula f1, SymbolicFormula f2) {
    MathsatSymbolicFormula m1 = (MathsatSymbolicFormula)f1;
    MathsatSymbolicFormula m2 = (MathsatSymbolicFormula)f2;

    long a = mathsat.api.msat_make_or(msatEnv, m1.getTerm(), m2.getTerm());
    return new MathsatSymbolicFormula(a);
  }

  @Override
  public SymbolicFormula makeEquivalence(SymbolicFormula f1, SymbolicFormula f2) {
    MathsatSymbolicFormula m1 = (MathsatSymbolicFormula)f1;
    MathsatSymbolicFormula m2 = (MathsatSymbolicFormula)f2;

    long a = mathsat.api.msat_make_iff(msatEnv, m1.getTerm(), m2.getTerm());
    return new MathsatSymbolicFormula(a);
  }

  @Override
  public SymbolicFormula makeIfThenElse(SymbolicFormula condition, SymbolicFormula f1, SymbolicFormula f2) {
    MathsatSymbolicFormula mCondition = (MathsatSymbolicFormula)condition;
    MathsatSymbolicFormula m1 = (MathsatSymbolicFormula)f1;
    MathsatSymbolicFormula m2 = (MathsatSymbolicFormula)f2;

    long ite = mathsat.api.msat_make_ite(msatEnv, mCondition.getTerm(), m1.getTerm(), m2.getTerm());

    return new MathsatSymbolicFormula(ite);
  }
  
  
  // ----------------- Numeric formulas -----------------

  @Override
  public SymbolicFormula makeNegate(SymbolicFormula f) {
    MathsatSymbolicFormula m = (MathsatSymbolicFormula)f;
    return new MathsatSymbolicFormula(mathsat.api.msat_make_negate(msatEnv, m.getTerm()));
  }

  @Override
  public SymbolicFormula makeNumber(int i) {
    return new MathsatSymbolicFormula(mathsat.api.msat_make_number(msatEnv, Integer.valueOf(i).toString()));
  }
  
  @Override
  public SymbolicFormula makeNumber(String i) {
    return new MathsatSymbolicFormula(mathsat.api.msat_make_number(msatEnv, i));
  }
  
  @Override
  public SymbolicFormula makePlus(SymbolicFormula f1, SymbolicFormula f2) {
    MathsatSymbolicFormula m1 = (MathsatSymbolicFormula)f1;
    MathsatSymbolicFormula m2 = (MathsatSymbolicFormula)f2;
    return new MathsatSymbolicFormula(mathsat.api.msat_make_plus(msatEnv, m1.getTerm(), m2.getTerm()));
  }
  
  @Override
  public SymbolicFormula makeMinus(SymbolicFormula f1, SymbolicFormula f2) {
    MathsatSymbolicFormula m1 = (MathsatSymbolicFormula)f1;
    MathsatSymbolicFormula m2 = (MathsatSymbolicFormula)f2;
    return new MathsatSymbolicFormula(mathsat.api.msat_make_minus(msatEnv, m1.getTerm(), m2.getTerm()));
  }
  
  @Override
  public SymbolicFormula makeDivide(SymbolicFormula f1, SymbolicFormula f2) {
    long t1 = ((MathsatSymbolicFormula)f1).getTerm();
    long t2 = ((MathsatSymbolicFormula)f2).getTerm();
    
    long result;
    if (mathsat.api.msat_term_is_number(t2) != 0) {
      // invert t2 and multiply with it
      String n = mathsat.api.msat_term_repr(t2);
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
      t2 = mathsat.api.msat_make_number(msatEnv, n);
      if (mathsat.api.MSAT_ERROR_TERM(t2)) {
        result = t2;
      } else {
        result = mathsat.api.msat_make_times(msatEnv, t2, t1);
      }
    } else {
      result = mathsat.api.msat_make_uif(msatEnv, divUfDecl, new long[]{t1, t2});
    }
    return new MathsatSymbolicFormula(result);
  }
  
  @Override
  public SymbolicFormula makeModulo(SymbolicFormula f1, SymbolicFormula f2) {
    return makeUIFforBinaryOperator(f1, f2, modUfDecl);
  }
  
  @Override
  public SymbolicFormula makeMultiply(SymbolicFormula f1, SymbolicFormula f2) {
    long t1 = ((MathsatSymbolicFormula)f1).getTerm();
    long t2 = ((MathsatSymbolicFormula)f2).getTerm();
    
    long result;
    if (mathsat.api.msat_term_is_number(t1) != 0) {
      result = mathsat.api.msat_make_times(msatEnv, t1, t2);
    } else if (mathsat.api.msat_term_is_number(t2) != 0) {
      result = mathsat.api.msat_make_times(msatEnv, t2, t1);
    } else {
      result = mathsat.api.msat_make_uif(msatEnv, multUfDecl, new long[]{t1, t2});
    }
    
    return new MathsatSymbolicFormula(result);
  }
  
  // ----------------- Numeric relations -----------------
  
  @Override
  public SymbolicFormula makeEqual(SymbolicFormula f1, SymbolicFormula f2) {
    MathsatSymbolicFormula m1 = (MathsatSymbolicFormula)f1;
    MathsatSymbolicFormula m2 = (MathsatSymbolicFormula)f2;
    return new MathsatSymbolicFormula(mathsat.api.msat_make_equal(msatEnv, m1.getTerm(), m2.getTerm()));
  }
  
  @Override
  public SymbolicFormula makeGt(SymbolicFormula f1, SymbolicFormula f2) {
    MathsatSymbolicFormula m1 = (MathsatSymbolicFormula)f1;
    MathsatSymbolicFormula m2 = (MathsatSymbolicFormula)f2;
    return new MathsatSymbolicFormula(mathsat.api.msat_make_gt(msatEnv, m1.getTerm(), m2.getTerm()));
  }
  
  @Override
  public SymbolicFormula makeGeq(SymbolicFormula f1, SymbolicFormula f2) {
    MathsatSymbolicFormula m1 = (MathsatSymbolicFormula)f1;
    MathsatSymbolicFormula m2 = (MathsatSymbolicFormula)f2;
    return new MathsatSymbolicFormula(mathsat.api.msat_make_geq(msatEnv, m1.getTerm(), m2.getTerm()));
  }
  
  @Override
  public SymbolicFormula makeLt(SymbolicFormula f1, SymbolicFormula f2) {
    MathsatSymbolicFormula m1 = (MathsatSymbolicFormula)f1;
    MathsatSymbolicFormula m2 = (MathsatSymbolicFormula)f2;
    return new MathsatSymbolicFormula(mathsat.api.msat_make_lt(msatEnv, m1.getTerm(), m2.getTerm()));
  }
  
  @Override
  public SymbolicFormula makeLeq(SymbolicFormula f1, SymbolicFormula f2) {
    MathsatSymbolicFormula m1 = (MathsatSymbolicFormula)f1;
    MathsatSymbolicFormula m2 = (MathsatSymbolicFormula)f2;
    return new MathsatSymbolicFormula(mathsat.api.msat_make_leq(msatEnv, m1.getTerm(), m2.getTerm()));
  }
  
  // ----------------- Bit-manipulation functions -----------------

  @Override
  public SymbolicFormula makeBitwiseNot(SymbolicFormula f) {
    long t = ((MathsatSymbolicFormula)f).getTerm();
    long[] args = {t};
    
    return new MathsatSymbolicFormula(mathsat.api.msat_make_uif(msatEnv, bitwiseNotUfDecl, args));
  }

  @Override
  public SymbolicFormula makeBitwiseAnd(SymbolicFormula f1, SymbolicFormula f2) {
    return makeUIFforBinaryOperator(f1, f2, bitwiseAndUfDecl);
  }
  
  @Override
  public SymbolicFormula makeBitwiseOr(SymbolicFormula f1, SymbolicFormula f2) {
    return makeUIFforBinaryOperator(f1, f2, bitwiseOrUfDecl);
  }
  
  @Override
  public SymbolicFormula makeBitwiseXor(SymbolicFormula f1, SymbolicFormula f2) {
    return makeUIFforBinaryOperator(f1, f2, bitwiseXorUfDecl);
  }
  
  @Override
  public SymbolicFormula makeShiftLeft(SymbolicFormula f1, SymbolicFormula f2) {
    return makeUIFforBinaryOperator(f1, f2, leftShiftUfDecl);
  }
  
  @Override
  public SymbolicFormula makeShiftRight(SymbolicFormula f1, SymbolicFormula f2) {
    return makeUIFforBinaryOperator(f1, f2, rightShiftUfDecl);
  }
  
  // ----------------- Uninterpreted functions -----------------
 
  private long buildMsatUF(String name, long[] args) {
    int[] tp = new int[args.length];
    for (int i = 0; i < tp.length; ++i) tp[i] = msatVarType;
    long decl = mathsat.api.msat_declare_uif(msatEnv, name, msatVarType, tp.length, tp);
    if (mathsat.api.MSAT_ERROR_DECL(decl)) {
      return mathsat.api.MSAT_MAKE_ERROR_TERM();
    }
    return mathsat.api.msat_make_uif(msatEnv, decl, args);
  }
  
  @Override
  public SymbolicFormula makeUIF(String name, SymbolicFormula[] args) {
    return new MathsatSymbolicFormula(buildMsatUF(name, getTerm(args)));
  }

  private long buildMsatUF(String name, long[] args, int idx) {
    int[] tp = new int[args.length];
    for (int i = 0; i < tp.length; ++i) tp[i] = msatVarType;
    long decl = mathsat.api.msat_declare_uif(
        msatEnv, name + INDEX_SEPARATOR + idx, msatVarType, tp.length, tp);
    if (mathsat.api.MSAT_ERROR_DECL(decl)) {
      return mathsat.api.MSAT_MAKE_ERROR_TERM();
    }
    return mathsat.api.msat_make_uif(msatEnv, decl, args);
  }

  @Override
  public SymbolicFormula makeUIF(String name, SymbolicFormula[] args, int idx) {
    return new MathsatSymbolicFormula(buildMsatUF(name, getTerm(args), idx));
  }
  
  private SymbolicFormula makeUIFforBinaryOperator(SymbolicFormula f1, SymbolicFormula f2, long uifDecl) {
    long t1 = ((MathsatSymbolicFormula)f1).getTerm();
    long t2 = ((MathsatSymbolicFormula)f2).getTerm();
    long[] args = {t1, t2};
    
    return new MathsatSymbolicFormula(mathsat.api.msat_make_uif(msatEnv, uifDecl, args));
  
  }


  // ----------------- Other formulas -----------------
  
  @Override
  public boolean isErrorTerm(SymbolicFormula f) {
    MathsatSymbolicFormula m = (MathsatSymbolicFormula)f;
    return mathsat.api.MSAT_ERROR_TERM(m.getTerm());
  }
  
  @Override
  public SymbolicFormula makeString(int i) {
    long n = mathsat.api.msat_make_number(msatEnv, Integer.valueOf(i).toString());
    
    return new MathsatSymbolicFormula(mathsat.api.msat_make_uif(msatEnv,
        stringLitUfDecl, new long[]{n}));
  }

  private long buildMsatVariable(String var, int idx) {
    long decl = mathsat.api.msat_declare_variable(
        msatEnv, var + INDEX_SEPARATOR + idx, msatVarType);
    return mathsat.api.msat_make_variable(msatEnv, decl);
  }

  @Override
  public SymbolicFormula makeVariable(String var, int idx) {
    return new MathsatSymbolicFormula(buildMsatVariable(var, idx));
  }

  private Pair<String, Integer> parseVariable(String var) {
    String[] s = var.split(INDEX_SEPARATOR);
    if (s.length != 2) {
      throw new IllegalArgumentException("Not an instantiated variable: " + var);
    }
    
    return new Pair<String, Integer>(s[0], Integer.parseInt(s[1]));
  }
  
  @Override
  public SymbolicFormula makeAssignment(SymbolicFormula f1, SymbolicFormula f2) {
    long t1 = ((MathsatSymbolicFormula)f1).getTerm();
    long t2 = ((MathsatSymbolicFormula)f2).getTerm();
    return new MathsatSymbolicFormula(mathsat.api.msat_make_uif(msatEnv, assignUfDecl, new long[]{t1, t2}));
  }

  private boolean isAssignment(long term) {
    return mathsat.api.msat_term_get_decl(term) == assignUfDecl;
  }

  
  // ----------------- Complex formula manipulation -----------------
  
  @Override
  public SymbolicFormula createPredicateVariable(SymbolicFormula atom) {
    long tt = ((MathsatSymbolicFormula)atom).getTerm();
    assert(!mathsat.api.MSAT_ERROR_TERM(tt));

    String repr = mathsat.api.msat_term_is_atom(tt) != 0 ?
                    mathsat.api.msat_term_repr(tt) :
                      ("#" + mathsat.api.msat_term_id(tt));
    long d = mathsat.api.msat_declare_variable(msatEnv,
        "\"PRED" + repr + "\"",
        mathsat.api.MSAT_BOOL);
    long var = mathsat.api.msat_make_variable(msatEnv, d);
    assert(!mathsat.api.MSAT_ERROR_TERM(var));

    return new MathsatSymbolicFormula(var);
  }

  @Override
  public String dumpFormula(SymbolicFormula f) {
    MathsatSymbolicFormula m = (MathsatSymbolicFormula)f;

    return mathsat.api.msat_to_msat(msatEnv, m.getTerm());
  }

  @Override
  public void dumpAbstraction(SymbolicFormula curState, SymbolicFormula edgeFormula,
      SymbolicFormula predDef, List<SymbolicFormula> importantPreds) {
    
    absPrinter.printMsatFormat(curState, edgeFormula, predDef, importantPreds);
    absPrinter.printNusmvFormat(curState, edgeFormula, predDef, importantPreds);
    absPrinter.nextNum();
  }

  @Override
  public SymbolicFormula parseInfix(String s) {
    long f = mathsat.api.msat_from_string(msatEnv, s);
    Preconditions.checkArgument(!mathsat.api.MSAT_ERROR_TERM(f), "Could not parse formula as Mathsat formula.");

    return new MathsatSymbolicFormula(f);
  }

  private int getIndex(String name, long[] args, SSAMap ssa, boolean autoInstantiate) {
    SymbolicFormula[] a = new SymbolicFormula[args.length];
    for (int i = 0; i < a.length; ++i) {
      a[i] = new MathsatSymbolicFormula(args[i]);
    }
    int idx = ssa.getIndex(name, a);
    if (idx <= 0) {
      if (!autoInstantiate) {
        return -1;
      } else {
        logger.log(Level.ALL, "DEBUG_3",
            "WARNING: Auto-instantiating lval: ", name, "(", a, ")");
        idx = 1;
        ssa.setIndex(name, a, idx);
      }
    }
    return idx;
  }

  // ssa can be null. In this case, all the variables are instantiated
  // at index 1
  @Override
  public SymbolicFormula instantiate(SymbolicFormula f, SSAMap ssa) {
    Stack<Long> toProcess = new Stack<Long>();
    Map<Long, Long> cache = new HashMap<Long, Long>();

    long term = ((MathsatSymbolicFormula)f).getTerm();
    toProcess.push(term);
    while (!toProcess.empty()) {
      long t = toProcess.peek();
      if (cache.containsKey(t)) {
        toProcess.pop();
        continue;
      }
      if (mathsat.api.msat_term_is_variable(t) != 0) {
        toProcess.pop();
        String name = mathsat.api.msat_term_repr(t);
        int idx = (ssa != null ? ssa.getIndex(name) : 1);
        if (idx > 0) {
          // ok, the variable has an instance in the SSA, replace it
          long newt = buildMsatVariable(name, idx);
          assert(!mathsat.api.MSAT_ERROR_TERM(newt));
          cache.put(t, newt);
        } else {
          // the variable is not used in the SSA, keep it as is
          cache.put(t, t);
        }
      } else {
        boolean childrenDone = true;
        long[] newargs = new long[mathsat.api.msat_term_arity(t)];
        for (int i = 0; i < mathsat.api.msat_term_arity(t); ++i) {
          long c = mathsat.api.msat_term_get_arg(t, i);
          if (!cache.containsKey(c)) {
            toProcess.push(c);
            childrenDone = false;
          } else {
            newargs[i] = cache.get(c);
          }
        }
        if (childrenDone) {
          toProcess.pop();
          long newt;
          if (isAssignment(t)) {
            // now we replace our "fake" assignment with an equality
            assert(newargs.length == 2);
            newt = mathsat.api.msat_make_equal(
                msatEnv, newargs[0], newargs[1]);
          } else {
            String name = null;
            if (mathsat.api.msat_term_is_uif(t) != 0) {
              long d = mathsat.api.msat_term_get_decl(t);
              name = mathsat.api.msat_decl_get_name(d);
            }
            if (name != null && ufCanBeLvalue(name)) {
              int idx = (ssa != null ?
                  getIndex(name, newargs, ssa, false) : 1);
              if (idx > 0) {
                // ok, the variable has an instance in the SSA,
                // replace it

                newt = buildMsatUF(name, newargs, idx);

                assert(!mathsat.api.MSAT_ERROR_TERM(newt));
                cache.put(t, newt);
              } else {
                newt = mathsat.api.msat_replace_args(
                    msatEnv, t, newargs);
              }
            } else {
              newt = mathsat.api.msat_replace_args(
                  msatEnv, t, newargs);
            }
          }
          assert(!mathsat.api.MSAT_ERROR_TERM(newt));
          cache.put(t, newt);
        }
      }
    }

    assert(cache.containsKey(term));
    return new MathsatSymbolicFormula(cache.get(term));
  }

  private boolean ufCanBeLvalue(String name) {
    return name.startsWith(".{") || name.startsWith("->{");
  }

  /**
   * As a side effect, this method does the same thing as {@link #replaceAssignments(SymbolicFormula)}
   * to the formula.
   */
  @Override
  public PathFormula shift(SymbolicFormula f, SSAMap ssa) {
    Stack<Long> toProcess = new Stack<Long>();
    Map<Long, Long> cache = new HashMap<Long, Long>();

    SSAMap newssa = new SSAMap();

    long term = ((MathsatSymbolicFormula)f).getTerm();
    toProcess.push(term);
    while (!toProcess.empty()) {
      long t = toProcess.peek();
      if (cache.containsKey(t)) {
        toProcess.pop();
        continue;
      }
//    if (termIsAssignment(t)) {
//// treat assignments specially. When we shift, we always have to
//    // update the SSA index of the variable being assigned
//    long var = mathsat.api.msat_term_get_arg(t, 0);
//    if (!cache.containsKey(var)) {
//    String name = mathsat.api.msat_term_repr(var);

//    CPAMain.logManager.log(Level.ALL, "DEBUG_3", "SHIFTING ASSIGNMENT:",
//    new MathsatSymbolicFormula(t), " VAR: ", name);

//    // check whether this is an instantiated variable
//    String[] bits = name.split("@");
//    int idx = -1;
//    assert(bits.length == 2);
//    try {
//    idx = Integer.parseInt(bits[1]);
//    name = bits[0];
//    } catch (NumberFormatException e) {
//    assert(false);
//    }
//    int ssaidx = ssa.getIndex(name);
//    if (ssaidx > 0) {
//    if (idx == 1) {
//    System.out.println("ERROR!!!, Shifting: " +
//    mathsat.api.msat_term_repr(t) + ", var: " +
//    name + ", TERM: " +
//    mathsat.api.msat_term_repr(term));
//    System.out.flush();
//    }
//    assert(idx > 1); //TODO!!!
//    long newvar = buildMsatVariable(name, ssaidx + idx-1);
//    assert(!mathsat.api.MSAT_ERROR_TERM(newvar));
//    cache.put(var, newvar);
//    if (newssa.getIndex(name) < ssaidx + idx-1) {
//    newssa.setIndex(name, ssaidx + idx-1);
//    }
//    } else {
//    cache.put(var, var);
//    if (newssa.getIndex(name) < idx) {
//    newssa.setIndex(name, idx);
//    }
//    }

//    CPAMain.logManager.log(Level.ALL, "DEBUG_3", "SHIFTING ASSIGNMENT,",
//    "RESULT: ", //name, "@", newssa.getIndex(name));
//    mathsat.api.msat_term_repr(cache.get(var)));
//    }
//    }

      if (mathsat.api.msat_term_is_variable(t) != 0) {
        toProcess.pop();
        // check whether this is an instantiated variable
        Pair<String, Integer> var = parseVariable(mathsat.api.msat_term_repr(t));
        String name = var.getFirst();
        int idx = var.getSecond();

        if (idx > 0) {
          // ok, the variable is instantiated in the formula
          // retrieve the index in the SSA, and shift
          int ssaidx = ssa.getIndex(name);
          //assert(ssaidx > 0);
          if (ssaidx > 0) {
            long newt = buildMsatVariable(name, ssaidx + idx-1);
            assert(!mathsat.api.MSAT_ERROR_TERM(newt));
            cache.put(t, newt);
            if (newssa.getIndex(name) < ssaidx + idx-1) {
              newssa.setIndex(name, ssaidx + idx-1);
            }
          } else {
            cache.put(t, t);
            if (newssa.getIndex(name) < idx) {
              newssa.setIndex(name, idx);
            }
          }
        } else {
          // the variable is not instantiated, keep it as is
          cache.put(t, t);
        }
      } else {
        boolean childrenDone = true;
        long[] newargs = new long[mathsat.api.msat_term_arity(t)];
        for (int i = 0; i < mathsat.api.msat_term_arity(t); ++i) {
          long c = mathsat.api.msat_term_get_arg(t, i);
          if (!cache.containsKey(c)) {
            toProcess.push(c);
            childrenDone = false;
          } else {
            newargs[i] = cache.get(c);
          }
        }
        if (childrenDone) {
          toProcess.pop();
          long newt = mathsat.api.MSAT_MAKE_ERROR_TERM();
          if (isAssignment(t)) {
            newt = mathsat.api.msat_make_equal(
                msatEnv, newargs[0], newargs[1]);
          } else {
            String name = null;
            if (mathsat.api.msat_term_is_uif(t) != 0) {
              long d = mathsat.api.msat_term_get_decl(t);
              name = mathsat.api.msat_decl_get_name(d);
            }
            if (name != null && ufCanBeLvalue(name)) {
              // we have to shift this uif as well
              Pair<String, Integer> uif = parseVariable(name);
              name = uif.getFirst();
              int idx = uif.getSecond();
              
              if (idx > 0) {
                // ok, the UF is instantiated in the formula
                // retrieve the index in the SSA, and shift
                SymbolicFormula[] a =
                  new SymbolicFormula[newargs.length];
                for (int i = 0; i < a.length; ++i) {
                  a[i] = new MathsatSymbolicFormula(
                      newargs[i]);
                }

                int ssaidx = ssa.getIndex(name, a);
                if (ssaidx > 0) {
                  int newidx = ssaidx + idx-1;

                  newt = buildMsatUF(name, newargs, newidx);

                  assert(!mathsat.api.MSAT_ERROR_TERM(newt));
                  cache.put(t, newt);
                  if (newssa.getIndex(name, a) < newidx) {
                    newssa.setIndex(name, a, newidx);
                  }
                } else {
                  newt = mathsat.api.msat_replace_args(
                      msatEnv, t, newargs);
                  if (newssa.getIndex(name, a) < idx) {
                    newssa.setIndex(name, a, idx);
                  }
                }
              } else {
                // the UF is not instantiated, keep it as is
                newt = mathsat.api.msat_replace_args(
                    msatEnv, t, newargs);
              }
            } else { // "normal" non-variable term
              newt = mathsat.api.msat_replace_args(
                  msatEnv, t, newargs);
            }
          }
          assert(!mathsat.api.MSAT_ERROR_TERM(newt));

          cache.put(t, newt);
        }
      }
    }

    assert(cache.containsKey(term));
    return new PathFormula(
        new MathsatSymbolicFormula(cache.get(term)), newssa);
  }

  /**
   * Looks for uninterpreted functions in the formula and adds bitwise
   * axioms for them.
   */
  @Override
  public SymbolicFormula prepareFormula(SymbolicFormula f) {
    if (useBitwiseAxioms) {
      SymbolicFormula bitwiseAxioms = getBitwiseAxioms(f);
      if (!bitwiseAxioms.isTrue()) {
        f = makeAnd(f, bitwiseAxioms);

        logger.log(Level.ALL, "DEBUG_3", "ADDED BITWISE AXIOMS:", bitwiseAxioms);
      }
    }
    return f;
  }
  
  /**
   * Looks for uninterpreted functions in the formulas and adds bitwise
   * axioms for them to the last formula.
   */
  @Override
  public void prepareFormulas(List<SymbolicFormula> formulas) {
    if (useBitwiseAxioms) {
      SymbolicFormula bitwiseAxioms = makeTrue();
  
      for (SymbolicFormula fm : formulas) {
        SymbolicFormula a = getBitwiseAxioms(fm);
        if (!a.isTrue()) {
          bitwiseAxioms = makeAnd(bitwiseAxioms, a);  
        }
      }
  
      if (!bitwiseAxioms.isTrue()) {
        logger.log(Level.ALL, "DEBUG_3", "ADDING BITWISE AXIOMS TO THE",
            "LAST GROUP: ", bitwiseAxioms);
        formulas.set(formulas.size()-1, makeAnd(formulas.get(formulas.size()-1), bitwiseAxioms));
      }
    }
  }
  
  // returns a formula with some "static learning" about some bitwise
  // operations, so that they are (a bit) "less uninterpreted"
  // Currently it add's the following formulas for each number literal n that
  // appears in the formula: "(n & 0 == 0) and (0 & n == 0)"
  // But only if an bitwise "and" occurs in the formula.
  @Deprecated
  public MathsatSymbolicFormula getBitwiseAxioms(SymbolicFormula f) {
    Deque<Long> toProcess = new ArrayDeque<Long>();
    Set<Long> seen = new HashSet<Long>();
    Set<Long> allLiterals = new HashSet<Long>();

    boolean andFound = false;

    toProcess.add(((MathsatSymbolicFormula)f).getTerm());
    while (!toProcess.isEmpty()) {
      long t = toProcess.pollLast();

      if (mathsat.api.msat_term_is_number(t) != 0) {
        allLiterals.add(t);
      }
      if (mathsat.api.msat_term_is_uif(t) != 0) {
        String r = mathsat.api.msat_term_repr(t);
        if (r.startsWith("_&_")) {
          andFound = true;
        }
      }
      for (int i = 0; i < mathsat.api.msat_term_arity(t); ++i) {
        long c = mathsat.api.msat_term_get_arg(t, i);
        if (seen.add(c)) {
          // was not already contained in seen
          toProcess.add(c);
        }
      }
    }

    long result = mathsat.api.msat_make_true(msatEnv);
    if (andFound) {
      long z = mathsat.api.msat_make_number(msatEnv, "0");
      for (long n : allLiterals) {
        long u1 = mathsat.api.msat_make_uif(msatEnv, bitwiseAndUfDecl, new long[]{n, z});
        long u2 = mathsat.api.msat_make_uif(msatEnv, bitwiseAndUfDecl, new long[]{z, n});
        long e1 = mathsat.api.msat_make_equal(msatEnv, u1, z);
        long e2 = mathsat.api.msat_make_equal(msatEnv, u2, z);
        long a = mathsat.api.msat_make_and(msatEnv, e1, e2);
        result = mathsat.api.msat_make_and(msatEnv, result, a);
      }
    }
    return new MathsatSymbolicFormula(result);
  }

  private long uninstantiate(long term) {
    Map<Long, Long> cache = uninstantiateCache;
    Stack<Long> toProcess = new Stack<Long>();
    toProcess.push(term);
    while (!toProcess.empty()) {
      long t = toProcess.peek();
      if (cache.containsKey(t)) {
        toProcess.pop();
        continue;
      }
      if (mathsat.api.msat_term_is_variable(t) != 0) {
        String name = parseVariable(mathsat.api.msat_term_repr(t)).getFirst();
        
        long d = mathsat.api.msat_declare_variable(msatEnv, name,
            msatVarType);
        long newt = mathsat.api.msat_make_variable(msatEnv, d);
        cache.put(t, newt);
      } else {
        long[] children = new long[mathsat.api.msat_term_arity(t)];
        boolean childrenDone = true;
        for (int i = 0; i < children.length; ++i) {
          long c = mathsat.api.msat_term_get_arg(t, i);
          if (cache.containsKey(c)) {
            children[i] = cache.get(c);
          } else {
            childrenDone = false;
            toProcess.push(c);
          }
        }
        if (childrenDone) {
          toProcess.pop();
          String name = null;
          if (mathsat.api.msat_term_is_uif(t) != 0) {
            long d = mathsat.api.msat_term_get_decl(t);
            name = mathsat.api.msat_decl_get_name(d);
          }
          if (name != null && ufCanBeLvalue(name)) {
            name = parseVariable(name).getFirst(); 
            
            int[] tp = new int[children.length];
            for (int i = 0; i < tp.length; ++i) {
              tp[i] = msatVarType;
            }
            long d = mathsat.api.msat_declare_uif(msatEnv, name,
                msatVarType, tp.length, tp);
            long newt = mathsat.api.msat_make_uif(
                msatEnv, d, children);
            cache.put(t, newt);
          } else {
            cache.put(t, mathsat.api.msat_replace_args(
                msatEnv, t, children));
          }
        }
      }
    }
    assert(cache.containsKey(term));
    return cache.get(term);
  }

  /**
   * Given an "instantiated" formula, returns the corresponding formula in
   * which all the variables are "generic" ones. This is the inverse of the
   * instantiate() method above
   */
  @Override
  public MathsatSymbolicFormula uninstantiate(SymbolicFormula f) {
    return new MathsatSymbolicFormula(uninstantiate(((MathsatSymbolicFormula)f).getTerm()));
  }

  @Override
  public SymbolicFormula[] getInstantiatedAt(SymbolicFormula[] args,
      SSAMap ssa, Map<SymbolicFormula, SymbolicFormula> cache) {
    Stack<Long> toProcess = new Stack<Long>();
    SymbolicFormula[] ret = new SymbolicFormula[args.length];
    for (SymbolicFormula f : args) {
        toProcess.push(((MathsatSymbolicFormula)f).getTerm());
    }
  
    while (!toProcess.empty()) {
        long t = toProcess.peek();
        SymbolicFormula tt = new MathsatSymbolicFormula(t);
        if (cache.containsKey(tt)) {
            toProcess.pop();
            continue;
        }
        if (mathsat.api.msat_term_is_variable(t) != 0) {
            toProcess.pop();
            String name = mathsat.api.msat_term_repr(t);
            assert(ssa.getIndex(name) > 0);
            cache.put(tt, instantiate(
                    new MathsatSymbolicFormula(t), ssa));
        } else if (mathsat.api.msat_term_is_uif(t) != 0) {
            long d = mathsat.api.msat_term_get_decl(t);
            String name = mathsat.api.msat_decl_get_name(d);
            if (ufCanBeLvalue(name)) {
                SymbolicFormula[] cc =
                    new SymbolicFormula[mathsat.api.msat_term_arity(t)];
                boolean childrenDone = true;
                for (int i = 0; i < cc.length; ++i) {
                    long c = mathsat.api.msat_term_get_arg(t, i);
                    SymbolicFormula f = new MathsatSymbolicFormula(c);
                    if (cache.containsKey(f)) {
                        cc[i] = cache.get(f);
                    } else {
                        toProcess.push(c);
                        childrenDone = false;
                    }
                }
                if (childrenDone) {
                    toProcess.pop();
                    if (ssa.getIndex(name, cc) < 0) {
                        ssa.setIndex(name, cc, 1);
                    }
                    cache.put(tt, instantiate(tt, ssa));
                }
            } else {
                toProcess.pop();
                cache.put(tt, tt);
            }
        } else {
            toProcess.pop();
            cache.put(tt, tt);
        }
    }
    for (int i = 0; i < ret.length; ++i) {
        assert(cache.containsKey(args[i]));
        ret[i] = cache.get(args[i]);
    }
    return ret;
  }

  @Override
  public Collection<SymbolicFormula> extractAtoms(SymbolicFormula f,
      boolean splitArithEqualities, boolean conjunctionsOnly) {
    Set<Long> cache = new HashSet<Long>();
    List<SymbolicFormula> atoms = new ArrayList<SymbolicFormula>();

    Stack<Long> toProcess = new Stack<Long>();
    toProcess.push(((MathsatSymbolicFormula)f).getTerm());

    while (!toProcess.empty()) {
      long term = toProcess.pop();
      assert(!cache.contains(term));
      cache.add(term);

      if (mathsat.api.msat_term_is_true(term) != 0 ||
          mathsat.api.msat_term_is_false(term) != 0) {
        continue;
      }

      if (mathsat.api.msat_term_is_atom(term) != 0) {
        term = uninstantiate(term);
        if (splitArithEqualities &&
            mathsat.api.msat_term_is_equal(term) != 0 &&
            isPurelyArithmetic(term, arithCache)) {
          long a1 = mathsat.api.msat_term_get_arg(term, 0);
          long a2 = mathsat.api.msat_term_get_arg(term, 1);
          long t1 = mathsat.api.msat_make_leq(msatEnv, a1, a2);
          //long t2 = mathsat.api.msat_make_leq(msatEnv, a2, a1);
          cache.add(t1);
          //cache.add(t2);
          atoms.add(new MathsatSymbolicFormula(t1));
          //atoms.add(t2);
          atoms.add(new MathsatSymbolicFormula(term));
        } else {
          atoms.add(new MathsatSymbolicFormula(term));
        }
      } else if (conjunctionsOnly) {
        if (mathsat.api.msat_term_is_not(term) != 0 ||
            mathsat.api.msat_term_is_and(term) != 0) {
          // ok, go into this formula
          for (int i = 0; i < mathsat.api.msat_term_arity(term); ++i){
            long c = mathsat.api.msat_term_get_arg(term, i);
            if (!cache.contains(c)) {
              toProcess.push(c);
            }
          }
        } else {
          // otherwise, treat this as atomic
          term = uninstantiate(term);
          atoms.add(new MathsatSymbolicFormula(term));
        }
      } else {
        for (int i = 0; i < mathsat.api.msat_term_arity(term); ++i){
          long c = mathsat.api.msat_term_get_arg(term, i);
          if (!cache.contains(c)) {
            toProcess.push(c);
          }
        }
      }
    }

    return atoms;
  }

  // returns true if the given term is a pure arithmetic term
  private boolean isPurelyArithmetic(long term,
      Map<Long, Boolean> arithCache) {
    if (arithCache.containsKey(term)) {
      return arithCache.get(term);
    } else if (mathsat.api.msat_term_is_uif(term) != 0) {
      arithCache.put(term, false);
      return false;
    } else {
      int a = mathsat.api.msat_term_arity(term);
      boolean yes = true;
      for (int i = 0; i < a; ++i) {
        yes |= isPurelyArithmetic(
            mathsat.api.msat_term_get_arg(term, i), arithCache);
      }
      arithCache.put(term, yes);
      return yes;
    }
  }

  /**
   * The path formulas created by this class have an uninterpreted function :=
   * where an assignment should be. This method replaces all those appearances
   * by equalities (which is a valid representation of an assignment for a SSA
   * formula).
   */
  @Override
  public MathsatSymbolicFormula replaceAssignments(SymbolicFormula f) {
    Stack<Long> toProcess = new Stack<Long>();
    Map<Long, Long> cache = replaceAssignmentsCache;

    long term = ((MathsatSymbolicFormula)f).getTerm();
    toProcess.push(term);
    while (!toProcess.empty()) {
      long t = toProcess.peek();
      if (cache.containsKey(t)) {
        toProcess.pop();
        continue;
      }
      if (mathsat.api.msat_term_arity(t) == 0) {
        cache.put(t, t);
      } else {
        long[] newargs = new long[mathsat.api.msat_term_arity(t)];
        boolean childrenDone = true;
        for (int i = 0; i < newargs.length; ++i) {
          long c = mathsat.api.msat_term_get_arg(t, i);
          if (!cache.containsKey(c)) {
            childrenDone = false;
            toProcess.push(c);
          } else {
            newargs[i] = cache.get(c);
          }
        }
        if (childrenDone) {
          toProcess.pop();
          long newt = mathsat.api.MSAT_MAKE_ERROR_TERM();
          if (isAssignment(t)) {
            newt = mathsat.api.msat_make_equal(
                msatEnv, newargs[0], newargs[1]);
          } else {
            newt = mathsat.api.msat_replace_args(
                msatEnv, t, newargs);
          }
          assert(!mathsat.api.MSAT_ERROR_TERM(newt));
          cache.put(t, newt);
        }
      }
    }
    assert(cache.containsKey(term));
    return new MathsatSymbolicFormula(cache.get(term));
  }

  /**
   * returns an SSA map for the instantiated formula f
   */
  @Override
  public SSAMap extractSSA(SymbolicFormula f) {
    SSAMap ssa = new SSAMap();
    Stack<Long> toProcess = new Stack<Long>();
    Set<Long> cache = new HashSet<Long>();

    toProcess.push(((MathsatSymbolicFormula)f).getTerm());
    while (!toProcess.empty()) {
      long t = toProcess.pop();
      if (cache.contains(t)) {
        continue;
      }
      cache.add(t);
      if (mathsat.api.msat_term_is_variable(t) != 0) {
        Pair<String, Integer> var = parseVariable(mathsat.api.msat_term_repr(t));
        String name = var.getFirst();
        int idx = var.getSecond();
        if (idx > ssa.getIndex(name)) {
          ssa.setIndex(name, idx);
        }
      } else {
        for (int i = 0; i < mathsat.api.msat_term_arity(t); ++i) {
          toProcess.push(mathsat.api.msat_term_get_arg(t, i));
        }
      }
    }

    return ssa;
  }

  @Override
  public void collectVarNames(SymbolicFormula term, Set<String> vars,
                              Set<Pair<String, SymbolicFormula[]>> lvals) {

    Deque<Long> toProcess = new ArrayDeque<Long>();
    toProcess.push(((MathsatSymbolicFormula)term).getTerm());
    // TODO - this assumes the term is small! There is no memoizing yet!!
    while (!toProcess.isEmpty()) {
        long t = toProcess.pop();
        if (mathsat.api.msat_term_is_variable(t) != 0) {
            vars.add(mathsat.api.msat_term_repr(t));
        } else {
            for (int i = 0; i < mathsat.api.msat_term_arity(t); ++i) {
                toProcess.push(mathsat.api.msat_term_get_arg(t, i));
            }
            if (mathsat.api.msat_term_is_uif(t) != 0) {
                long d = mathsat.api.msat_term_get_decl(t);
                String name = mathsat.api.msat_decl_get_name(d);
                if (ufCanBeLvalue(name)) {
                    int n = mathsat.api.msat_term_arity(t);
                    SymbolicFormula[] a = new SymbolicFormula[n];
                    for (int i = 0; i < n; ++i) {
                        a[i] = new MathsatSymbolicFormula(
                                mathsat.api.msat_term_get_arg(t, i));
                    }
                    lvals.add(new Pair<String, SymbolicFormula[]>(name, a));
                }
            }
        }
    }
  }
  
  @Override
  public SymbolicFormulaManager.AllSatCallback getAllSatCallback(FormulaManager mgr, AbstractFormulaManager amgr) {
    return new AllSatCallback(mgr, amgr, logger);
  }
  
  /**
   * callback used to build the predicate abstraction of a formula
   * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
   */
  private static class AllSatCallback implements TheoremProver.AllSatCallback, SymbolicFormulaManager.AllSatCallback {
      private final FormulaManager mgr;
      private final AbstractFormulaManager amgr;
      private final LogManager logger;
      
      private long totalTime = 0;
      private int count = 0;

      private AbstractFormula formula;
      private final Deque<AbstractFormula> cubes = new ArrayDeque<AbstractFormula>();

      private AllSatCallback(FormulaManager mgr, AbstractFormulaManager amgr, LogManager logger) {
          this.mgr = mgr;
          this.amgr = amgr;
          this.logger = logger;
          this.formula = amgr.makeFalse();
      }

      @Override
      public long getTotalTime() {
          return totalTime;
      }

      @Override
      public int getCount() {
        return count;
      }

      @Override
      public AbstractFormula getResult() {
          if (cubes.size() > 0) {
              buildBalancedOr();
          }
          return formula;
      }

      private void buildBalancedOr() {
          cubes.add(formula);
          while (cubes.size() > 1) {
              AbstractFormula b1 = cubes.remove();
              AbstractFormula b2 = cubes.remove();
              cubes.add(amgr.makeOr(b1, b2));
          }
          assert(cubes.size() == 1);
          formula = cubes.remove();
      }

      @Override
      public void modelFound(List<SymbolicFormula> model) {
          logger.log(Level.ALL, "Allsat found model", model);
          long start = System.currentTimeMillis();

          // the abstraction is created simply by taking the disjunction
          // of all the models found by msat_all_sat, and storing them
          // in a BDD
          // first, let's create the BDD corresponding to the model
          Deque<AbstractFormula> curCube = new ArrayDeque<AbstractFormula>();
          AbstractFormula m = amgr.makeTrue();
          for (SymbolicFormula f : model) {
              long t = ((MathsatSymbolicFormula)f).getTerm();

              AbstractFormula v;
              if (mathsat.api.msat_term_is_not(t) != 0) {
                  t = mathsat.api.msat_term_get_arg(t, 0);
                  v = mgr.getPredicate(new MathsatSymbolicFormula(t)).getFormula();
                  v = amgr.makeNot(v);
              } else {
                v = mgr.getPredicate(f).getFormula();
              }
              curCube.add(v);
          }
          // now, add the model to the bdd
          curCube.add(m);
          while (curCube.size() > 1) {
              AbstractFormula v1 = curCube.remove();
              AbstractFormula v2 = curCube.remove();
              curCube.add(amgr.makeAnd(v1, v2));
          }
          assert(curCube.size() == 1);
          m = curCube.remove();
          cubes.add(m);

          count++;

          long end = System.currentTimeMillis();
          totalTime += (end - start);
      }
  }
}
