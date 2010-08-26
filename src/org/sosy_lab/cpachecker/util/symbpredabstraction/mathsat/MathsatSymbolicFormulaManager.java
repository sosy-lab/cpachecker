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

import static mathsat.api.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaList;
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
  private final Map<SymbolicFormula, SymbolicFormula> uninstantiateCache = new HashMap<SymbolicFormula, SymbolicFormula>();

  // cache for replacing assignments
  private final Map<Long, Long> replaceAssignmentsCache = new HashMap<Long, Long>();

  protected final LogManager logger;
  private final MathsatAbstractionPrinter absPrinter;  
  
  public MathsatSymbolicFormulaManager(Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    config.inject(this, MathsatSymbolicFormulaManager.class);
    logger = pLogger;
    msatEnv = msat_create_env();
    msatVarType = useIntegers ? MSAT_INT : MSAT_REAL;

    final int[] msatVarType1 = {msatVarType};
    final int[] msatVarType2 = {msatVarType, msatVarType};
    assignUfDecl = msat_declare_uif(msatEnv, ":=", MSAT_BOOL, 2, msatVarType2);

    bitwiseAndUfDecl = msat_declare_uif(msatEnv, "_&_", msatVarType, 2, msatVarType2);
    bitwiseOrUfDecl = msat_declare_uif(msatEnv, "_|_", msatVarType, 2, msatVarType2);
    bitwiseXorUfDecl = msat_declare_uif(msatEnv, "_^_", msatVarType, 2, msatVarType2);
    bitwiseNotUfDecl = msat_declare_uif(msatEnv, "_~_", msatVarType, 1, msatVarType1);
    leftShiftUfDecl = msat_declare_uif(msatEnv, "_<<_", msatVarType, 2, msatVarType2);
    rightShiftUfDecl = msat_declare_uif(msatEnv, "_>>_", msatVarType, 2, msatVarType2);
    multUfDecl = msat_declare_uif(msatEnv, "_*_", msatVarType, 2, msatVarType2);
    divUfDecl = msat_declare_uif(msatEnv, "_/_", msatVarType, 2, msatVarType2);
    modUfDecl = msat_declare_uif(msatEnv, "_%_", msatVarType, 2, msatVarType2);

    stringLitUfDecl = msat_declare_uif(msatEnv, "__string__", msatVarType, 1, msatVarType1);
    
    absPrinter = new MathsatAbstractionPrinter(msatEnv, "abs", logger);
  }

  long getMsatEnv() {
    return msatEnv;
  }

  long createEnvironment(boolean shared, boolean ghostFilter) {
    long env;
    if (shared) {
      env = msat_create_shared_env(msatEnv);
    } else {
      env = msat_create_env();
    }
    
    msat_add_theory(env, MSAT_UF);
    if (useIntegers) {
      msat_add_theory(env, MSAT_LIA);
      int ok = msat_set_option(env, "split_eq", "false");
      assert(ok == 0);
    } else {
      msat_add_theory(env, MSAT_LRA);
    }
    if (useDtc) {
      msat_set_theory_combination(env, MSAT_COMB_DTC);
    }
    // disable static learning. For small problems, this is just overhead
    msat_set_option(env, "sl", "0");
    
    if (ghostFilter) {
      msat_set_option(env, "ghost_filter", "true");
    }
    
    return env;
  }
  
  private static long getTerm(SymbolicFormula f) {
    return ((MathsatSymbolicFormula)f).getTerm();
  }
  
  private static long[] getTerm(SymbolicFormulaList f) {
    return ((MathsatSymbolicFormulaList)f).getTerms();
  }
  
  private static MathsatSymbolicFormula encapsulate(long t) {
    return new MathsatSymbolicFormula(t);
  }

  private static MathsatSymbolicFormulaList encapsulate(long[] t) {
    return new MathsatSymbolicFormulaList(t);
  }
  
  private String makeName(String name, int idx) {
    return name + INDEX_SEPARATOR + idx;
  }
  
  private Pair<String, Integer> parseName(String var) {
    String[] s = var.split(INDEX_SEPARATOR);
    if (s.length != 2) {
      throw new IllegalArgumentException("Not an instantiated variable: " + var);
    }
    
    return new Pair<String, Integer>(s[0], Integer.parseInt(s[1]));
  }

  // ----------------- Boolean formulas -----------------
  
  @Override
  public boolean isBoolean(SymbolicFormula f) {
    return msat_term_get_type(getTerm(f)) == MSAT_BOOL;
  }
  
  @Override
  public SymbolicFormula makeTrue() {
    return encapsulate(msat_make_true(msatEnv));
  }
  
  @Override
  public SymbolicFormula makeFalse() {
    return encapsulate(msat_make_false(msatEnv));
  }

  @Override
  public SymbolicFormula makeNot(SymbolicFormula f) {
    return encapsulate(msat_make_not(msatEnv, getTerm(f)));
  }

  @Override
  public SymbolicFormula makeAnd(SymbolicFormula f1, SymbolicFormula f2) {
    return encapsulate(msat_make_and(msatEnv, getTerm(f1), getTerm(f2)));
  }

  @Override
  public SymbolicFormula makeOr(SymbolicFormula f1, SymbolicFormula f2) {
    return encapsulate(msat_make_or(msatEnv, getTerm(f1), getTerm(f2)));
  }

  @Override
  public SymbolicFormula makeEquivalence(SymbolicFormula f1, SymbolicFormula f2) {
    return encapsulate(msat_make_iff(msatEnv, getTerm(f1), getTerm(f2)));
  }

  @Override
  public SymbolicFormula makeIfThenElse(SymbolicFormula condition, SymbolicFormula f1, SymbolicFormula f2) {
    return encapsulate(msat_make_ite(msatEnv, getTerm(condition), getTerm(f1), getTerm(f2)));
  }
  
  
  // ----------------- Numeric formulas -----------------

  @Override
  public SymbolicFormula makeNegate(SymbolicFormula f) {
    return encapsulate(msat_make_negate(msatEnv, getTerm(f)));
  }

  @Override
  public SymbolicFormula makeNumber(int i) {
    return encapsulate(msat_make_number(msatEnv, Integer.valueOf(i).toString()));
  }
  
  @Override
  public SymbolicFormula makeNumber(String i) {
    return encapsulate(msat_make_number(msatEnv, i));
  }
  
  @Override
  public SymbolicFormula makePlus(SymbolicFormula f1, SymbolicFormula f2) {
    return encapsulate(msat_make_plus(msatEnv, getTerm(f1), getTerm(f2)));
  }
  
  @Override
  public SymbolicFormula makeMinus(SymbolicFormula f1, SymbolicFormula f2) {
    return encapsulate(msat_make_minus(msatEnv, getTerm(f1), getTerm(f2)));
  }
  
  @Override
  public SymbolicFormula makeDivide(SymbolicFormula f1, SymbolicFormula f2) {
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
      if (MSAT_ERROR_TERM(t2)) {
        result = t2;
      } else {
        result = msat_make_times(msatEnv, t2, t1);
      }
    } else {
      result = msat_make_uif(msatEnv, divUfDecl, new long[]{t1, t2});
    }
    return encapsulate(result);
  }
  
  @Override
  public SymbolicFormula makeModulo(SymbolicFormula f1, SymbolicFormula f2) {
    return makeUIFforBinaryOperator(f1, f2, modUfDecl);
  }
  
  @Override
  public SymbolicFormula makeMultiply(SymbolicFormula f1, SymbolicFormula f2) {
    long t1 = getTerm(f1);
    long t2 = getTerm(f2);
    
    long result;
    if (msat_term_is_number(t1) != 0) {
      result = msat_make_times(msatEnv, t1, t2);
    } else if (msat_term_is_number(t2) != 0) {
      result = msat_make_times(msatEnv, t2, t1);
    } else {
      result = msat_make_uif(msatEnv, multUfDecl, new long[]{t1, t2});
    }
    
    return encapsulate(result);
  }
  
  // ----------------- Numeric relations -----------------
  
  @Override
  public SymbolicFormula makeEqual(SymbolicFormula f1, SymbolicFormula f2) {
    return encapsulate(msat_make_equal(msatEnv, getTerm(f1), getTerm(f2)));
  }
  
  @Override
  public SymbolicFormula makeGt(SymbolicFormula f1, SymbolicFormula f2) {
    return encapsulate(msat_make_gt(msatEnv, getTerm(f1), getTerm(f2)));
  }
  
  @Override
  public SymbolicFormula makeGeq(SymbolicFormula f1, SymbolicFormula f2) {
    return encapsulate(msat_make_geq(msatEnv, getTerm(f1), getTerm(f2)));
  }
  
  @Override
  public SymbolicFormula makeLt(SymbolicFormula f1, SymbolicFormula f2) {
    return encapsulate(msat_make_lt(msatEnv, getTerm(f1), getTerm(f2)));
  }
  
  @Override
  public SymbolicFormula makeLeq(SymbolicFormula f1, SymbolicFormula f2) {
    return encapsulate(msat_make_leq(msatEnv, getTerm(f1), getTerm(f2)));
  }
  
  // ----------------- Bit-manipulation functions -----------------

  @Override
  public SymbolicFormula makeBitwiseNot(SymbolicFormula f) {
    long[] args = {getTerm(f)};
    
    return encapsulate(msat_make_uif(msatEnv, bitwiseNotUfDecl, args));
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
    Arrays.fill(tp, msatVarType);
    long decl = msat_declare_uif(msatEnv, name, msatVarType, tp.length, tp);
    if (MSAT_ERROR_DECL(decl)) {
      return MSAT_MAKE_ERROR_TERM();
    }
    return msat_make_uif(msatEnv, decl, args);
  }
  
  @Override
  public SymbolicFormula makeUIF(String name, SymbolicFormulaList args) {
    return encapsulate(buildMsatUF(name, getTerm(args)));
  }

  @Override
  public SymbolicFormula makeUIF(String name, SymbolicFormulaList args, int idx) {
    return encapsulate(buildMsatUF(makeName(name, idx), getTerm(args)));
  }
  
  private SymbolicFormula makeUIFforBinaryOperator(SymbolicFormula f1, SymbolicFormula f2, long uifDecl) {
    long[] args = {getTerm(f1), getTerm(f2)};
    
    return encapsulate(msat_make_uif(msatEnv, uifDecl, args));
  }


  // ----------------- Other formulas -----------------
  
  @Override
  public boolean isErrorTerm(SymbolicFormula f) {
    return MSAT_ERROR_TERM(getTerm(f));
  }
  
  @Override
  public SymbolicFormula makeString(int i) {
    long n = msat_make_number(msatEnv, Integer.valueOf(i).toString());
    
    return encapsulate(msat_make_uif(msatEnv,
        stringLitUfDecl, new long[]{n}));
  }

  private long buildMsatVariable(String var, int idx) {
    return buildMsatVariable(makeName(var, idx));
  }
  
  private long buildMsatVariable(String var) {
    long decl = msat_declare_variable(msatEnv, var, msatVarType);
    return msat_make_variable(msatEnv, decl);
  }

  @Override
  public SymbolicFormula makeVariable(String var, int idx) {
    return encapsulate(buildMsatVariable(var, idx));
  }
  
  @Override
  public SymbolicFormula makeAssignment(SymbolicFormula f1, SymbolicFormula f2) {
    long[] args = {getTerm(f1), getTerm(f2)};

    return encapsulate(msat_make_uif(msatEnv, assignUfDecl, args));
  }

  private boolean isAssignment(long term) {
    return msat_term_get_decl(term) == assignUfDecl;
  }

  
  // ----------------- Convert to list -----------------
  
  @Override
  public SymbolicFormulaList makeList(SymbolicFormula pF) {
    return new MathsatSymbolicFormulaList(getTerm(pF));
  }
  
  @Override
  public SymbolicFormulaList makeList(SymbolicFormula pF1, SymbolicFormula pF2) {
    return new MathsatSymbolicFormulaList(getTerm(pF1), getTerm(pF2));
  }
  
  @Override
  public SymbolicFormulaList makeList(SymbolicFormula... pF) {
    long[] t = new long[pF.length];
    for (int i = 0; i < pF.length; i++) {
      t[i] = getTerm(pF[i]);
    }
    return encapsulate(t);
  }
  
  // ----------------- Complex formula manipulation -----------------
  
  @Override
  public SymbolicFormula createPredicateVariable(SymbolicFormula atom) {
    long tt = getTerm(atom);
    assert(!MSAT_ERROR_TERM(tt));

    String repr = msat_term_is_atom(tt) != 0 ?
                    msat_term_repr(tt) :
                      ("#" + msat_term_id(tt));
    long d = msat_declare_variable(msatEnv,
        "\"PRED" + repr + "\"",
        MSAT_BOOL);
    long var = msat_make_variable(msatEnv, d);
    assert(!MSAT_ERROR_TERM(var));

    return encapsulate(var);
  }

  @Override
  public String dumpFormula(SymbolicFormula f) {
    return msat_to_msat(msatEnv, getTerm(f));
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
    long f = msat_from_string(msatEnv, s);
    Preconditions.checkArgument(!MSAT_ERROR_TERM(f), "Could not parse formula as Mathsat formula.");

    return encapsulate(f);
  }

  @Override
  public SymbolicFormula instantiate(SymbolicFormula f, SSAMap ssa) {
    return instantiate(f, ssa, new HashMap<SymbolicFormula, SymbolicFormula>());
  }

  @Override
  public SymbolicFormulaList instantiate(SymbolicFormulaList f, SSAMap ssa) {
    long[] args = getTerm(f);
    long[] result = new long[args.length];
    Map<SymbolicFormula, SymbolicFormula> cache = new HashMap<SymbolicFormula, SymbolicFormula>();
    
    for (int i = 0; i < args.length; i++) {
      result[i] = getTerm(instantiate(encapsulate(args[i]), ssa, cache));
    }
    return encapsulate(result);
  }

  private SymbolicFormula instantiate(SymbolicFormula f, SSAMap ssa,
                                      Map<SymbolicFormula, SymbolicFormula> cache) {
    Deque<SymbolicFormula> toProcess = new ArrayDeque<SymbolicFormula>();

    toProcess.push(f);
    while (!toProcess.isEmpty()) {
      final SymbolicFormula tt = toProcess.peek();
      final long t = getTerm(tt);
      if (cache.containsKey(tt)) {
        toProcess.pop();
        continue;
      }

      if (msat_term_is_variable(t) != 0) {
        toProcess.pop();
        String name = msat_term_repr(t);
        int idx = (ssa != null ? ssa.getIndex(name) : 1);
        if (idx > 0) {
          // ok, the variable has an instance in the SSA, replace it
          long newt = buildMsatVariable(name, idx);
          assert(!MSAT_ERROR_TERM(newt));
          cache.put(tt, encapsulate(newt));
        } else {
          // the variable is not used in the SSA, keep it as is
          cache.put(tt, tt);
        }

      } else {
        boolean childrenDone = true;
        long[] newargs = new long[msat_term_arity(t)];
        for (int i = 0; i < newargs.length; ++i) {
          SymbolicFormula c = encapsulate(msat_term_get_arg(t, i));
          SymbolicFormula newC = cache.get(c);
          if (newC != null) {
            newargs[i] = getTerm(newC);
          } else {
            toProcess.push(c);
            childrenDone = false;
          }
        }

        if (childrenDone) {
          toProcess.pop();
          long newt;
          if (isAssignment(t)) {
            // now we replace our "fake" assignment with an equality
            assert newargs.length == 2;
            newt = msat_make_equal(msatEnv, newargs[0], newargs[1]);
          
          } else if (msat_term_is_uif(t) != 0) {
            String name = msat_decl_get_name(msat_term_get_decl(t));
            assert name != null;
            
            if (ufCanBeLvalue(name)) {
              int idx = (ssa != null ? ssa.getIndex(name, encapsulate(newargs)) : 1);
              if (idx > 0) {
                // ok, the variable has an instance in the SSA, replace it
                newt = buildMsatUF(makeName(name, idx), newargs);
              } else {
                newt = msat_replace_args(msatEnv, t, newargs);
              }
            } else {
              newt = msat_replace_args(msatEnv, t, newargs);
            }
          } else {
            newt = msat_replace_args(msatEnv, t, newargs);
          }
          
          assert(!MSAT_ERROR_TERM(newt));
          cache.put(tt, encapsulate(newt));
        }
      }
    }

    SymbolicFormula result = cache.get(f);
    assert result != null;
    return result;
  }

  private boolean ufCanBeLvalue(String name) {
    return name.startsWith(".{") || name.startsWith("->{");
  }

  @Override
  public SymbolicFormula uninstantiate(SymbolicFormula f) {
    Map<SymbolicFormula, SymbolicFormula> cache = uninstantiateCache;
    Deque<SymbolicFormula> toProcess = new ArrayDeque<SymbolicFormula>();
    
    toProcess.push(f);
    while (!toProcess.isEmpty()) {
      final SymbolicFormula tt = toProcess.peek();
      final long t = getTerm(tt);
      if (cache.containsKey(tt)) {
        toProcess.pop();
        continue;
      }

      if (msat_term_is_variable(t) != 0) {
        String name = parseName(msat_term_repr(t)).getFirst();
        
        long newt = buildMsatVariable(name);
        assert(!MSAT_ERROR_TERM(newt));
        cache.put(tt, encapsulate(newt));

      } else {
        boolean childrenDone = true;
        long[] newargs = new long[msat_term_arity(t)];
        for (int i = 0; i < newargs.length; ++i) {
          SymbolicFormula c = encapsulate(msat_term_get_arg(t, i));
          SymbolicFormula newC = cache.get(c);
          if (newC != null) {
            newargs[i] = getTerm(newC);
          } else {
            toProcess.push(c);
            childrenDone = false;
          }
        }

        if (childrenDone) {
          toProcess.pop();
          long newt;
          if (msat_term_is_uif(t) != 0) {
            String name = msat_decl_get_name(msat_term_get_decl(t));
            assert name != null;

            if (ufCanBeLvalue(name)) {
              name = parseName(name).getFirst(); 
              
              newt = buildMsatUF(name, newargs);
            } else {
              newt = msat_replace_args(msatEnv, t, newargs);
            }
          } else {
            newt = msat_replace_args(msatEnv, t, newargs);
          }
          
          assert(!MSAT_ERROR_TERM(newt));
          cache.put(tt, encapsulate(newt));
        }
      }
    }

    SymbolicFormula result = cache.get(f);
    assert result != null;
    return result;
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

    long term = getTerm(f);
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
//    long var = msat_term_get_arg(t, 0);
//    if (!cache.containsKey(var)) {
//    String name = msat_term_repr(var);

//    CPAMain.logManager.log(Level.ALL, "DEBUG_3", "SHIFTING ASSIGNMENT:",
//    encapsulate(t), " VAR: ", name);

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
//    msat_term_repr(t) + ", var: " +
//    name + ", TERM: " +
//    msat_term_repr(term));
//    System.out.flush();
//    }
//    assert(idx > 1); //TODO!!!
//    long newvar = buildMsatVariable(name, ssaidx + idx-1);
//    assert(!MSAT_ERROR_TERM(newvar));
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
//    msat_term_repr(cache.get(var)));
//    }
//    }

      if (msat_term_is_variable(t) != 0) {
        toProcess.pop();
        // check whether this is an instantiated variable
        Pair<String, Integer> var = parseName(msat_term_repr(t));
        String name = var.getFirst();
        int idx = var.getSecond();

        if (idx > 0) {
          // ok, the variable is instantiated in the formula
          // retrieve the index in the SSA, and shift
          int ssaidx = ssa.getIndex(name);
          //assert(ssaidx > 0);
          if (ssaidx > 0) {
            long newt = buildMsatVariable(name, ssaidx + idx-1);
            assert(!MSAT_ERROR_TERM(newt));
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
        long[] newargs = new long[msat_term_arity(t)];
        for (int i = 0; i < msat_term_arity(t); ++i) {
          long c = msat_term_get_arg(t, i);
          if (!cache.containsKey(c)) {
            toProcess.push(c);
            childrenDone = false;
          } else {
            newargs[i] = cache.get(c);
          }
        }
        if (childrenDone) {
          toProcess.pop();
          long newt = MSAT_MAKE_ERROR_TERM();
          if (isAssignment(t)) {
            newt = msat_make_equal(
                msatEnv, newargs[0], newargs[1]);
          } else {
            String name = null;
            if (msat_term_is_uif(t) != 0) {
              long d = msat_term_get_decl(t);
              name = msat_decl_get_name(d);
            }
            if (name != null && ufCanBeLvalue(name)) {
              // we have to shift this uif as well
              Pair<String, Integer> uif = parseName(name);
              name = uif.getFirst();
              int idx = uif.getSecond();
              
              if (idx > 0) {
                // ok, the UF is instantiated in the formula
                // retrieve the index in the SSA, and shift
                SymbolicFormulaList a = encapsulate(newargs);

                int ssaidx = ssa.getIndex(name, a);
                if (ssaidx > 0) {
                  int newidx = ssaidx + idx-1;

                  newt = buildMsatUF(makeName(name, newidx), newargs);

                  assert(!MSAT_ERROR_TERM(newt));
                  cache.put(t, newt);
                  if (newssa.getIndex(name, a) < newidx) {
                    newssa.setIndex(name, a, newidx);
                  }
                } else {
                  newt = msat_replace_args(
                      msatEnv, t, newargs);
                  if (newssa.getIndex(name, a) < idx) {
                    newssa.setIndex(name, a, idx);
                  }
                }
              } else {
                // the UF is not instantiated, keep it as is
                newt = msat_replace_args(
                    msatEnv, t, newargs);
              }
            } else { // "normal" non-variable term
              newt = msat_replace_args(
                  msatEnv, t, newargs);
            }
          }
          assert(!MSAT_ERROR_TERM(newt));

          cache.put(t, newt);
        }
      }
    }

    assert(cache.containsKey(term));
    return new PathFormula(encapsulate(cache.get(term)), newssa);
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

    toProcess.add(getTerm(f));
    while (!toProcess.isEmpty()) {
      long t = toProcess.pollLast();

      if (msat_term_is_number(t) != 0) {
        allLiterals.add(t);
      }
      if (msat_term_is_uif(t) != 0) {
        String r = msat_term_repr(t);
        if (r.startsWith("_&_")) {
          andFound = true;
        }
      }
      for (int i = 0; i < msat_term_arity(t); ++i) {
        long c = msat_term_get_arg(t, i);
        if (seen.add(c)) {
          // was not already contained in seen
          toProcess.add(c);
        }
      }
    }

    long result = msat_make_true(msatEnv);
    if (andFound) {
      long z = msat_make_number(msatEnv, "0");
      for (long n : allLiterals) {
        long u1 = msat_make_uif(msatEnv, bitwiseAndUfDecl, new long[]{n, z});
        long u2 = msat_make_uif(msatEnv, bitwiseAndUfDecl, new long[]{z, n});
        long e1 = msat_make_equal(msatEnv, u1, z);
        long e2 = msat_make_equal(msatEnv, u2, z);
        long a = msat_make_and(msatEnv, e1, e2);
        result = msat_make_and(msatEnv, result, a);
      }
    }
    return encapsulate(result);
  }

  @Override
  public Collection<SymbolicFormula> extractAtoms(SymbolicFormula f,
      boolean splitArithEqualities, boolean conjunctionsOnly) {
    Set<Long> cache = new HashSet<Long>();
    List<SymbolicFormula> atoms = new ArrayList<SymbolicFormula>();

    Stack<Long> toProcess = new Stack<Long>();
    toProcess.push(getTerm(f));

    while (!toProcess.empty()) {
      long term = toProcess.pop();
      assert(!cache.contains(term));
      cache.add(term);

      if (msat_term_is_true(term) != 0 ||
          msat_term_is_false(term) != 0) {
        continue;
      }

      if (msat_term_is_atom(term) != 0) {
        term = getTerm(uninstantiate(encapsulate(term)));
        if (splitArithEqualities &&
            msat_term_is_equal(term) != 0 &&
            isPurelyArithmetic(term, arithCache)) {
          long a1 = msat_term_get_arg(term, 0);
          long a2 = msat_term_get_arg(term, 1);
          long t1 = msat_make_leq(msatEnv, a1, a2);
          //long t2 = msat_make_leq(msatEnv, a2, a1);
          cache.add(t1);
          //cache.add(t2);
          atoms.add(encapsulate(t1));
          //atoms.add(t2);
          atoms.add(encapsulate(term));
        } else {
          atoms.add(encapsulate(term));
        }
      } else if (conjunctionsOnly) {
        if (msat_term_is_not(term) != 0 ||
            msat_term_is_and(term) != 0) {
          // ok, go into this formula
          for (int i = 0; i < msat_term_arity(term); ++i){
            long c = msat_term_get_arg(term, i);
            if (!cache.contains(c)) {
              toProcess.push(c);
            }
          }
        } else {
          // otherwise, treat this as atomic
          atoms.add(uninstantiate(encapsulate(term)));
        }
      } else {
        for (int i = 0; i < msat_term_arity(term); ++i){
          long c = msat_term_get_arg(term, i);
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
    } else if (msat_term_is_uif(term) != 0) {
      arithCache.put(term, false);
      return false;
    } else {
      int a = msat_term_arity(term);
      boolean yes = true;
      for (int i = 0; i < a; ++i) {
        yes |= isPurelyArithmetic(
            msat_term_get_arg(term, i), arithCache);
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

    long term = getTerm(f);
    toProcess.push(term);
    while (!toProcess.empty()) {
      long t = toProcess.peek();
      if (cache.containsKey(t)) {
        toProcess.pop();
        continue;
      }
      if (msat_term_arity(t) == 0) {
        cache.put(t, t);
      } else {
        long[] newargs = new long[msat_term_arity(t)];
        boolean childrenDone = true;
        for (int i = 0; i < newargs.length; ++i) {
          long c = msat_term_get_arg(t, i);
          if (!cache.containsKey(c)) {
            childrenDone = false;
            toProcess.push(c);
          } else {
            newargs[i] = cache.get(c);
          }
        }
        if (childrenDone) {
          toProcess.pop();
          long newt = MSAT_MAKE_ERROR_TERM();
          if (isAssignment(t)) {
            newt = msat_make_equal(
                msatEnv, newargs[0], newargs[1]);
          } else {
            newt = msat_replace_args(
                msatEnv, t, newargs);
          }
          assert(!MSAT_ERROR_TERM(newt));
          cache.put(t, newt);
        }
      }
    }
    assert(cache.containsKey(term));
    return encapsulate(cache.get(term));
  }

  /**
   * returns an SSA map for the instantiated formula f
   */
  @Override
  public SSAMap extractSSA(SymbolicFormula f) {
    SSAMap ssa = new SSAMap();
    Stack<Long> toProcess = new Stack<Long>();
    Set<Long> cache = new HashSet<Long>();

    toProcess.push(getTerm(f));
    while (!toProcess.empty()) {
      long t = toProcess.pop();
      if (cache.contains(t)) {
        continue;
      }
      cache.add(t);
      if (msat_term_is_variable(t) != 0) {
        Pair<String, Integer> var = parseName(msat_term_repr(t));
        String name = var.getFirst();
        int idx = var.getSecond();
        if (idx > ssa.getIndex(name)) {
          ssa.setIndex(name, idx);
        }
      } else {
        for (int i = 0; i < msat_term_arity(t); ++i) {
          toProcess.push(msat_term_get_arg(t, i));
        }
      }
    }

    return ssa;
  }

  @Override
  public void collectVarNames(SymbolicFormula term, Set<String> vars,
                              Set<Pair<String, SymbolicFormulaList>> lvals) {

    Deque<Long> toProcess = new ArrayDeque<Long>();
    toProcess.push(getTerm(term));
    // TODO - this assumes the term is small! There is no memoizing yet!!
    while (!toProcess.isEmpty()) {
        long t = toProcess.pop();
        if (msat_term_is_variable(t) != 0) {
            vars.add(msat_term_repr(t));
        } else {
            for (int i = 0; i < msat_term_arity(t); ++i) {
                toProcess.push(msat_term_get_arg(t, i));
            }
            if (msat_term_is_uif(t) != 0) {
                long d = msat_term_get_decl(t);
                String name = msat_decl_get_name(d);
                if (ufCanBeLvalue(name)) {
                    int n = msat_term_arity(t);
                    long[] a = new long[n];
                    for (int i = 0; i < n; ++i) {
                        a[i] = msat_term_get_arg(t, i);
                    }
                    SymbolicFormulaList aa = encapsulate(a);
                    lvals.add(new Pair<String, SymbolicFormulaList>(name, aa));
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
              long t = getTerm(f);

              AbstractFormula v;
              if (msat_term_is_not(t) != 0) {
                  t = msat_term_get_arg(t, 0);
                  v = mgr.getPredicate(encapsulate(t)).getFormula();
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
