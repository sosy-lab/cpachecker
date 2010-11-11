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

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.symbpredabstraction.SSAMap;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaList;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaManager;

import com.google.common.base.Preconditions;

@Options(prefix="cpas.symbpredabs.mathsat")
public class MathsatSymbolicFormulaManager implements SymbolicFormulaManager  {

  @Option
  private boolean useIntegers = false;

  @Option
  private boolean useDtc = false;
  
  // the MathSAT environment in which all terms are created
  private final long msatEnv;

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
  protected final int msatVarType;
  
  // the character for separating name and index of a value
  public static final String INDEX_SEPARATOR = "@";

  // various caches for speeding up expensive tasks
  //
  // cache for splitting arithmetic equalities in extractAtoms
  private final Map<SymbolicFormula, Boolean> arithCache = new HashMap<SymbolicFormula, Boolean>();

  // cache for uninstantiating terms (see uninstantiate() below)
  private final Map<SymbolicFormula, SymbolicFormula> uninstantiateCache = new HashMap<SymbolicFormula, SymbolicFormula>();
  
  private final SymbolicFormula trueFormula;
  private final SymbolicFormula falseFormula;
  
  public MathsatSymbolicFormulaManager(Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this, MathsatSymbolicFormulaManager.class);
    msatEnv = msat_create_env();
    msatVarType = useIntegers ? MSAT_INT : MSAT_REAL;

    final int[] msatVarType1 = {msatVarType};
    final int[] msatVarType2 = {msatVarType, msatVarType};

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
    
    trueFormula = encapsulate(msat_make_true(msatEnv));
    falseFormula = encapsulate(msat_make_false(msatEnv));
  }

  public long getMsatEnv() {
    return msatEnv;
  }

  public long createEnvironment(boolean shared, boolean ghostFilter) {
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
  
  public static long getTerm(SymbolicFormula f) {
    return ((MathsatSymbolicFormula)f).getTerm();
  }
  
  public static long[] getTerm(SymbolicFormulaList f) {
    return ((MathsatSymbolicFormulaList)f).getTerms();
  }
  
  public static SymbolicFormula encapsulate(long t) {
    return new MathsatSymbolicFormula(t);
  }

  public static SymbolicFormulaList encapsulate(long[] t) {
    return new MathsatSymbolicFormulaList(t);
  }
  
  public String makeName(String name, int idx) {
    return name + INDEX_SEPARATOR + idx;
  }
  
  public Pair<String, Integer> parseName(String var) {
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
    return trueFormula;
  }
  
  @Override
  public SymbolicFormula makeFalse() {
    return falseFormula;
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
      result = msat_make_times(msatEnv, t2, t1);
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
 
  public long buildMsatUF(String name, long[] args) {
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
  public SymbolicFormula makeString(int i) {
    long n = msat_make_number(msatEnv, Integer.valueOf(i).toString());
    
    return encapsulate(msat_make_uif(msatEnv,
        stringLitUfDecl, new long[]{n}));
  }

  public long buildMsatVariable(String var, int idx) {
    return buildMsatVariable(makeName(var, idx));
  }
  
  public long buildMsatVariable(String var) {
    long decl = msat_declare_variable(msatEnv, var, msatVarType);
    return msat_make_variable(msatEnv, decl);
  }

  @Override
  public SymbolicFormula makeVariable(String var, int idx) {
    return encapsulate(buildMsatVariable(var, idx));
  }
  
  @Override
  public SymbolicFormula makePredicateVariable(String var, int idx) {
    String name = makeName("PRED" + var, idx);
    long decl = msat_declare_variable(msatEnv, name, MSAT_BOOL);
    return encapsulate(msat_make_variable(msatEnv, decl));
  }

  @Override
  public SymbolicFormula makeAssignment(SymbolicFormula f1, SymbolicFormula f2) {
    return makeEqual(f1, f2);
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
    long t = getTerm(atom);

    String repr = (msat_term_is_atom(t) != 0)
                    ? msat_term_repr(t) : ("#" + msat_term_id(t));
    long d = msat_declare_variable(msatEnv, "\"PRED" + repr + "\"", MSAT_BOOL);
    long var = msat_make_variable(msatEnv, d);

    return encapsulate(var);
  }

  @Override
  public String dumpFormula(SymbolicFormula f) {
    return msat_to_msat(msatEnv, getTerm(f));
  }

/* Method for converting MSAT format to NUSMV format.
  public String printNusmvFormat(SymbolicFormula f, Set<SymbolicFormula> preds) {

    StringBuilder out = new StringBuilder();
    out.append("MODULE main\n");
    String repr = dumpFormula(f);
    for (String line : repr.split("\n")) {
      if (line.startsWith("VAR")) {
        out.append(line + ";\n");
      } else if (line.startsWith("DEFINE")) {
        String[] bits = line.split(" +", 5);
        out.append("DEFINE " + bits[1] + " " + bits[4] + ";\n");
      } else if (line.startsWith("FORMULA")) {
        out.append("INIT" + line.substring(7) + "\n");
      } else {
        out.append(line);
        out.append('\n');
      }
    }
    out.append("\nTRANS FALSE\n");
    out.append("INVARSPEC (0 = 0)\n");
    for (SymbolicFormula p : preds) {
      repr = p.toString();
      repr = repr.replaceAll("([a-zA-Z:_0-9]+@[0-9]+)", "\"$1\"");
      out.append("PRED " + repr + "\n");
    }
    return out.toString();
  }
*/

  @Override
  public SymbolicFormula parseInfix(String s) {
    long f = msat_from_string(msatEnv, s);
    Preconditions.checkArgument(!MSAT_ERROR_TERM(f),
        "Could not parse formula %s as Mathsat formula.", s);

    return encapsulate(f);
  }

  @Override
  public SymbolicFormula parse(String s) {
    long f = msat_from_msat(msatEnv, s);
    Preconditions.checkArgument(!MSAT_ERROR_TERM(f),
        "Could not parse formula %s as Mathsat formula.", s);

    return encapsulate(f);
  }
  
  @Override
  public SymbolicFormula instantiate(SymbolicFormula f, SSAMap ssa) {
    Deque<SymbolicFormula> toProcess = new ArrayDeque<SymbolicFormula>();
    Map<SymbolicFormula, SymbolicFormula> cache = new HashMap<SymbolicFormula, SymbolicFormula>();

    toProcess.push(f);
    while (!toProcess.isEmpty()) {
      final SymbolicFormula tt = toProcess.peek();
      if (cache.containsKey(tt)) {
        toProcess.pop();
        continue;
      }
      final long t = getTerm(tt);

      if (msat_term_is_variable(t) != 0) {
        toProcess.pop();
        String name = msat_term_repr(t);
        int idx = ssa.getIndex(name);
        if (idx > 0) {
          // ok, the variable has an instance in the SSA, replace it
          long newt = buildMsatVariable(name, idx);
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
          if (msat_term_is_uif(t) != 0) {
            String name = msat_decl_get_name(msat_term_get_decl(t));
            assert name != null;
            
            if (ufCanBeLvalue(name)) {
              int idx = ssa.getIndex(name, encapsulate(newargs));
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
          
          cache.put(tt, encapsulate(newt));
        }
      }
    }

    SymbolicFormula result = cache.get(f);
    assert result != null;
    return result;
  }

  public boolean ufCanBeLvalue(String name) {
    return name.startsWith(".{") || name.startsWith("->{");
  }

  @Override
  public SymbolicFormula uninstantiate(SymbolicFormula f) {
    Map<SymbolicFormula, SymbolicFormula> cache = uninstantiateCache;
    Deque<SymbolicFormula> toProcess = new ArrayDeque<SymbolicFormula>();
    
    toProcess.push(f);
    while (!toProcess.isEmpty()) {
      final SymbolicFormula tt = toProcess.peek();
      if (cache.containsKey(tt)) {
        toProcess.pop();
        continue;
      }
      final long t = getTerm(tt);

      if (msat_term_is_variable(t) != 0) {
        assert msat_term_get_type(t) == msatVarType : "Invalid type for variable " + tt;
        String name = parseName(msat_term_repr(t)).getFirst();
        
        long newt = buildMsatVariable(name);
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
          
          cache.put(tt, encapsulate(newt));
        }
      }
    }

    SymbolicFormula result = cache.get(f);
    assert result != null;
    return result;
  }
  
  // returns a formula with some "static learning" about some bitwise
  // operations, so that they are (a bit) "less uninterpreted"
  // Currently it add's the following formulas for each number literal n that
  // appears in the formula: "(n & 0 == 0) and (0 & n == 0)"
  // But only if an bitwise "and" occurs in the formula.
  @Override
  public SymbolicFormula getBitwiseAxioms(SymbolicFormula f) {
    Deque<SymbolicFormula> toProcess = new ArrayDeque<SymbolicFormula>();
    Set<SymbolicFormula> seen = new HashSet<SymbolicFormula>();
    Set<SymbolicFormula> allLiterals = new HashSet<SymbolicFormula>();

    boolean andFound = false;

    toProcess.add(f);
    while (!toProcess.isEmpty()) {
      final SymbolicFormula tt = toProcess.pollLast();
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
        SymbolicFormula c = encapsulate(msat_term_get_arg(t, i));
        if (seen.add(c)) {
          // was not already contained in seen
          toProcess.add(c);
        }
      }
    }

    long result = msat_make_true(msatEnv);
    if (andFound) {
      long z = msat_make_number(msatEnv, "0");
      for (SymbolicFormula nn : allLiterals) {
        long n = getTerm(nn);
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
    Set<SymbolicFormula> cache = new HashSet<SymbolicFormula>();
    List<SymbolicFormula> atoms = new ArrayList<SymbolicFormula>();

    Deque<SymbolicFormula> toProcess = new ArrayDeque<SymbolicFormula>();
    toProcess.push(f);

    while (!toProcess.isEmpty()) {
      SymbolicFormula tt = toProcess.pop();
      long t = getTerm(tt);
      assert !cache.contains(tt);
      cache.add(tt);

      if (msat_term_is_true(t) != 0 || msat_term_is_false(t) != 0) {
        continue;
      }

      if (msat_term_is_atom(t) != 0) {
        tt = uninstantiate(tt);
        t = getTerm(tt);

        if (   splitArithEqualities
            && (msat_term_is_equal(t) != 0)
            && isPurelyArithmetic(tt)) {
          long a1 = msat_term_get_arg(t, 0);
          long a2 = msat_term_get_arg(t, 1);
          long t1 = msat_make_leq(msatEnv, a1, a2);
          //long t2 = msat_make_leq(msatEnv, a2, a1);
          SymbolicFormula tt1 = encapsulate(t1);
          //SymbolicFormula tt2 = encapsulate(t2);
          cache.add(tt1);
          //cache.add(tt2);
          atoms.add(tt1);
          //atoms.add(tt2);
          atoms.add(tt);
        } else {
          atoms.add(tt);
        }

      } else if (conjunctionsOnly
            && !((msat_term_is_not(t) != 0) || (msat_term_is_and(t) != 0))) {
        // conjunctions only, but formula is neither "not" nor "and"
        // treat this as atomic
        atoms.add(uninstantiate(tt));

      } else {
        // ok, go into this formula
        for (int i = 0; i < msat_term_arity(t); ++i){
          SymbolicFormula c = encapsulate(msat_term_get_arg(t, i));
          if (!cache.contains(c)) {
            toProcess.push(c);
          }
        }
      }
    }

    return atoms;
  }

  // returns true if the given term is a pure arithmetic term
  private boolean isPurelyArithmetic(SymbolicFormula f) {
    Boolean result = arithCache.get(f);
    if (result != null) {
      return result;
    }
    
    long t = getTerm(f);

    boolean res = true;
    if (msat_term_is_uif(t) != 0) {
      res = false;

    } else {
      int arity = msat_term_arity(t);
      for (int i = 0; i < arity; ++i) {
        res |= isPurelyArithmetic(encapsulate(msat_term_get_arg(t, i)));
        if (!res) {
          break;
        }
      }
    }
    arithCache.put(f, res);
    return res;
  }
}
