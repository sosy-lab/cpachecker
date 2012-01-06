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
package org.sosy_lab.cpachecker.util.predicates.mathsat;

import static org.sosy_lab.cpachecker.util.predicates.mathsat.NativeApi.*;

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
import java.util.Vector;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaList;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;

@Options(prefix="cpa.predicate.mathsat")
public class MathsatFormulaManager implements FormulaManager  {

  @Option(description="encode program variables as INTEGERs in MathSAT, instead of "
    + "using REALs. Since interpolation is not really supported by the laz solver, "
    + "when computing interpolants we still use the LA solver, "
    + "but encoding variables as ints might still be a good idea: "
    + "we can tighten strict inequalities, and split negated equalities")
    private boolean useIntegers = false;

  @Option(description="use a combination of theories (this is incomplete)")
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
  private static final String INDEX_SEPARATOR = "@";



  // various caches for speeding up expensive tasks
  //
  // cache for splitting arithmetic equalities in extractAtoms
  private final Map<Formula, Boolean> arithCache = new HashMap<Formula, Boolean>();

  // cache for uninstantiating terms (see uninstantiate() below)
  private final Map<Formula, Formula> uninstantiateCache = new HashMap<Formula, Formula>();
  // cache for next val uninstantiating terms
  private final Map<Pair<Formula,SSAMap>, Formula> uninstantiateNextValCache = new HashMap<Pair<Formula,SSAMap>, Formula>();

  // cache Formula x NumberOfPrimes -> PrimedFormula
  private Map<Pair<Formula, Integer>, Formula> primingCache = new HashMap<Pair<Formula, Integer>, Formula>();
  private Map<Formula, Formula> unprimingCache = new HashMap<Formula, Formula>();
  Map<Triple<Formula, Integer, Multimap<Integer, Integer>>, Formula> unprimingNmCache = new HashMap<Triple<Formula, Integer, Multimap<Integer, Integer>>, Formula>();
  Map<Formula, Map<String, Integer>> nonModularCache = new HashMap<Formula, Map<String, Integer>>();
  ListMultimap<Pair<Formula, ListMultimap<String, Integer>>, Formula> nmInstancesCache =  LinkedListMultimap.create();
  private final Map<Pair<Formula, Set<Integer>>, Formula> uninstantiateSelectedCache = new HashMap<Pair<Formula, Set<Integer>>, Formula>();

  // Formula -> how many times the variables are primed in the formula
  private SetMultimap<Formula, Integer> howManyPrimesCache = HashMultimap.create();

  private Map<Pair<Formula, Map<Integer, Integer>>, Formula> adjustingCache = new HashMap<Pair<Formula, Map<Integer, Integer>>, Formula>();

  //
  private Map<Formula, Integer> atomCountingCache = new HashMap<Formula, Integer>();


  private final Formula trueFormula;
  private final Formula falseFormula;





  private static MathsatFormulaManager    fManager;


  // returns a singleton instance of MathsatFormulaManager
  public static  MathsatFormulaManager getInstance(Configuration config, LogManager logger) throws InvalidConfigurationException{
    if (fManager == null){
      fManager = new MathsatFormulaManager(config, logger);
    }
    return fManager;
  }


  public MathsatFormulaManager(Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this, MathsatFormulaManager.class);
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

  static long getTerm(Formula f) {
    return ((MathsatFormula)f).getTerm();
  }

  static long[] getTerm(FormulaList f) {
    return ((MathsatFormulaList)f).getTerms();
  }

  static Formula encapsulate(long t) {
    return new MathsatFormula(t);
  }

  private static FormulaList encapsulate(long[] t) {
    return new MathsatFormulaList(t);
  }

  private static String makeName(String name, int idx) {
    return name + INDEX_SEPARATOR + idx;
  }

  static Pair<String, Integer> parseName(String var) {
    String[] s = var.split(INDEX_SEPARATOR);
    if (s.length != 2) {
      throw new IllegalArgumentException("Not an instantiated variable: " + var);
    }

    return Pair.of(s[0], Integer.parseInt(s[1]));
  }

  // ----------------- Boolean formulas -----------------

  @Override
  public boolean isBoolean(Formula f) {
    return msat_term_get_type(getTerm(f)) == MSAT_BOOL;
  }

  @Override
  public Formula makeTrue() {
    return trueFormula;
  }

  @Override
  public Formula makeFalse() {
    return falseFormula;
  }

  @Override
  public Formula makeNot(Formula f) {
    return encapsulate(msat_make_not(msatEnv, getTerm(f)));
  }

  @Override
  public Formula makeAnd(Formula f1, Formula f2) {
    return encapsulate(msat_make_and(msatEnv, getTerm(f1), getTerm(f2)));
  }

  @Override
  public Formula makeOr(Formula f1, Formula f2) {
    return encapsulate(msat_make_or(msatEnv, getTerm(f1), getTerm(f2)));
  }

  @Override
  public Formula makeEquivalence(Formula f1, Formula f2) {
    return encapsulate(msat_make_iff(msatEnv, getTerm(f1), getTerm(f2)));
  }

  @Override
  public Formula makeIfThenElse(Formula condition, Formula f1, Formula f2) {
    return encapsulate(msat_make_ite(msatEnv, getTerm(condition), getTerm(f1), getTerm(f2)));
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
      result = msat_make_uif(msatEnv, divUfDecl, new long[]{t1, t2});
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
      result = msat_make_uif(msatEnv, multUfDecl, new long[]{t1, t2});
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

    return encapsulate(msat_make_uif(msatEnv, bitwiseNotUfDecl, args));
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
  public Formula makeUIF(String name, FormulaList args) {
    return encapsulate(buildMsatUF(name, getTerm(args)));
  }

  @Override
  public Formula makeUIF(String name, FormulaList args, int idx) {
    return encapsulate(buildMsatUF(makeName(name, idx), getTerm(args)));
  }

  private Formula makeUIFforBinaryOperator(Formula f1, Formula f2, long uifDecl) {
    long[] args = {getTerm(f1), getTerm(f2)};

    return encapsulate(msat_make_uif(msatEnv, uifDecl, args));
  }


  // ----------------- Other formulas -----------------

  @Override
  public Formula makeString(int i) {
    long n = msat_make_number(msatEnv, Integer.toString(i));

    return encapsulate(msat_make_uif(msatEnv,
        stringLitUfDecl, new long[]{n}));
  }

  private long buildMsatVariable(String var, int type) {
    long decl = msat_declare_variable(msatEnv, var, type);
    return msat_make_variable(msatEnv, decl);
  }

  @Override
  public Formula makeVariable(String var, int idx) {
    return makeVariable(makeName(var, idx));
  }

  @Override
  public Formula makeVariable(String var) {
    return encapsulate(buildMsatVariable(var, msatVarType));
  }

  @Override
  public Formula makePredicateVariable(String var, int idx) {
    String name = makeName("PRED" + var, idx);
    long decl = msat_declare_variable(msatEnv, name, MSAT_BOOL);
    return encapsulate(msat_make_variable(msatEnv, decl));
  }

  @Override
  public Formula makeAssignment(Formula f1, Formula f2) {
    return makeEqual(f1, f2);
  }


  // ----------------- Convert to list -----------------

  @Override
  public FormulaList makeList(Formula pF) {
    return new MathsatFormulaList(getTerm(pF));
  }

  @Override
  public FormulaList makeList(Formula pF1, Formula pF2) {
    return new MathsatFormulaList(getTerm(pF1), getTerm(pF2));
  }

  @Override
  public FormulaList makeList(List<Formula> pFs) {
    long[] t = new long[pFs.size()];
    for (int i = 0; i < t.length; i++) {
      t[i] = getTerm(pFs.get(i));
    }
    return encapsulate(t);
  }
  // ----------------- Complex formula manipulation -----------------

  @Override
  public Formula createPredicateVariable(Formula atom) {
    long t = getTerm(atom);

    String repr = (msat_term_is_atom(t) != 0)
    ? msat_term_repr(t) : ("#" + msat_term_id(t));
    long d = msat_declare_variable(msatEnv, "\"PRED" + repr + "\"", MSAT_BOOL);
    long var = msat_make_variable(msatEnv, d);

    return encapsulate(var);
  }

  @Override
  public String dumpFormula(Formula f) {
    return msat_to_msat(msatEnv, getTerm(f));
  }

  /* Method for converting MSAT format to NUSMV format.
  public String printNusmvFormat(Formula f, Set<Formula> preds) {

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
    for (Formula p : preds) {
      repr = p.toString();
      repr = repr.replaceAll("([a-zA-Z:_0-9]+@[0-9]+)", "\"$1\"");
      out.append("PRED " + repr + "\n");
    }
    return out.toString();
  }
   */

  @Override
  public Formula parseInfix(String s) {
    long f = msat_from_string(msatEnv, s);
    Preconditions.checkArgument(!MSAT_ERROR_TERM(f),
        "Could not parse formula %s as Mathsat formula.", s);

    return encapsulate(f);
  }




  @Override
  public Formula parse(String s) {
    long f = msat_from_msat(msatEnv, s);
    Preconditions.checkArgument(!MSAT_ERROR_TERM(f),
        "Could not parse formula %s as Mathsat formula.", s);

    return encapsulate(f);
  }

  @Override
  public Formula instantiate(Formula f, SSAMap ssa) {
    Deque<Formula> toProcess = new ArrayDeque<Formula>();
    Map<Formula, Formula> cache = new HashMap<Formula, Formula>();

    toProcess.push(f);
    while (!toProcess.isEmpty()) {
      final Formula tt = toProcess.peek();
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
          long newt = buildMsatVariable(makeName(name, idx), msat_term_get_type(t));
          cache.put(tt, encapsulate(newt));
        } else {
          // TODO changes
          // the variable is not used in the SSA, keep it as is
          long newt = buildMsatVariable(makeName(name, 1), msat_term_get_type(t));
          cache.put(tt, encapsulate(newt));
        }

      } else {
        boolean childrenDone = true;
        long[] newargs = new long[msat_term_arity(t)];
        for (int i = 0; i < newargs.length; ++i) {
          Formula c = encapsulate(msat_term_get_arg(t, i));
          Formula newC = cache.get(c);
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

    Formula result = cache.get(f);
    assert result != null;
    return result;
  }

  @Override
  public Formula instantiateNextVal(Formula f, SSAMap oldSsa, SSAMap newSsa) {
    Deque<Formula> toProcess = new ArrayDeque<Formula>();
    Map<Formula, Formula> cache = new HashMap<Formula, Formula>();

    toProcess.push(f);
    while (!toProcess.isEmpty()) {
      final Formula tt = toProcess.peek();
      if (cache.containsKey(tt)) {
        toProcess.pop();
        continue;
      }
      final long t = getTerm(tt);

      if (msat_term_is_variable(t) != 0) {
        toProcess.pop();
        String name = msat_term_repr(t);
        String uname = PathFormula.getUnhashed(name);
        int idx = uname == null ?  oldSsa.getIndex(name) : newSsa.getIndex(uname);
        name = uname == null ? name : uname;

        if (idx > 0) {
          // ok, the variable has an instance in the SSA, replace it
          long newt = buildMsatVariable(makeName(name, idx), msat_term_get_type(t));
          cache.put(tt, encapsulate(newt));
        } else {
          // TODO changes
          // the variable is not used in the SSA, keep it as is
          long newt = buildMsatVariable(makeName(name, 1), msat_term_get_type(t));
          cache.put(tt, encapsulate(newt));
        }
      } else {
        boolean childrenDone = true;
        long[] newargs = new long[msat_term_arity(t)];
        for (int i = 0; i < newargs.length; ++i) {
          Formula c = encapsulate(msat_term_get_arg(t, i));
          Formula newC = cache.get(c);
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
            // not supported
            newt = -1;
            assert false;
            /*
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
            }*/
          } else {
            newt = msat_replace_args(msatEnv, t, newargs);
          }

          cache.put(tt, encapsulate(newt));
        }
      }
    }

    Formula result = cache.get(f);
    assert result != null;
    return result;
  }

  private boolean ufCanBeLvalue(String name) {
    return name.startsWith(".{") || name.startsWith("->{");
  }

  @Override
  public Formula uninstantiate(Formula f) {
    Map<Formula, Formula> cache = uninstantiateCache;
    Deque<Formula> toProcess = new ArrayDeque<Formula>();

    toProcess.push(f);
    while (!toProcess.isEmpty()) {
      final Formula tt = toProcess.peek();
      if (cache.containsKey(tt)) {
        toProcess.pop();
        continue;
      }
      final long t = getTerm(tt);

      if (msat_term_is_variable(t) != 0) {
        String name = parseName(msat_term_repr(t)).getFirst();

        long newt = buildMsatVariable(name, msat_term_get_type(t));
        cache.put(tt, encapsulate(newt));

      } else {
        boolean childrenDone = true;
        long[] newargs = new long[msat_term_arity(t)];
        for (int i = 0; i < newargs.length; ++i) {
          Formula c = encapsulate(msat_term_get_arg(t, i));
          Formula newC = cache.get(c);
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

    Formula result = cache.get(f);
    assert result != null;
    return result;
  }

  /**
   * Strips the formula out of indexes. Variables with indexes equal to the map
   * are given a '#' prefix.
   * @param f
   * @param ssa
   * @return
   */
  private Formula uninstantiateNextVal(Formula f, SSAMap ssa) {
    Map<Pair<Formula, SSAMap>, Formula> cache = uninstantiateNextValCache;
    Deque<Formula> toProcess = new ArrayDeque<Formula>();

    toProcess.push(f);
    while (!toProcess.isEmpty()) {
      final Formula tt = toProcess.peek();
      if (cache.containsKey(Pair.of(tt, ssa))) {
        toProcess.pop();
        continue;
      }
      final long t = getTerm(tt);

      if (msat_term_is_variable(t) != 0) {
        Pair<String, Integer> pair = parseName(msat_term_repr(t));
        String name = pair.getFirst();
        Integer idx = pair.getSecond();
        if (ssa.getIndex(name) == idx){
          name = PathFormula.NEXTVAL_SYMBOL + name;
        }
        long newt = buildMsatVariable(name, msat_term_get_type(t));
        cache.put(Pair.of(tt, ssa), encapsulate(newt));

      } else {
        boolean childrenDone = true;
        long[] newargs = new long[msat_term_arity(t)];
        for (int i = 0; i < newargs.length; ++i) {
          Formula c = encapsulate(msat_term_get_arg(t, i));
          Formula newC = cache.get(Pair.of(c, ssa));
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

          cache.put(Pair.of(tt, ssa), encapsulate(newt));
        }
      }
    }

    Formula result = cache.get(Pair.of(f, ssa));
    assert result != null;
    return result;
  }

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
  public Collection<Formula> extractAtoms(Formula f,
      boolean splitArithEqualities, boolean conjunctionsOnly) {
    Set<Formula> cache = new HashSet<Formula>();
    List<Formula> atoms = new ArrayList<Formula>();

    Deque<Formula> toProcess = new ArrayDeque<Formula>();
    toProcess.push(f);

    while (!toProcess.isEmpty()) {
      Formula tt = toProcess.pop();
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
          Formula tt1 = encapsulate(t1);
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
          Formula c = encapsulate(msat_term_get_arg(t, i));
          if (!cache.contains(c)) {
            toProcess.push(c);
          }
        }
      }
    }

    return atoms;
  }

  @Override
  public Collection<Formula> extractNextValAtoms(Formula f, SSAMap ssa) {
    Set<Formula> cache = new HashSet<Formula>();
    List<Formula> atoms = new ArrayList<Formula>();

    Deque<Formula> toProcess = new ArrayDeque<Formula>();
    toProcess.push(f);

    while (!toProcess.isEmpty()) {
      Formula tt = toProcess.pop();
      long t = getTerm(tt);
      assert !cache.contains(tt);
      cache.add(tt);

      if (msat_term_is_true(t) != 0 || msat_term_is_false(t) != 0) {
        continue;
      }

      if (msat_term_is_atom(t) != 0) {
        tt = uninstantiateNextVal(tt, ssa);
        t = getTerm(tt);
        atoms.add(tt);

      } else {
        // ok, go into this formula
        for (int i = 0; i < msat_term_arity(t); ++i){
          Formula c = encapsulate(msat_term_get_arg(t, i));
          if (!cache.contains(c)) {
            toProcess.push(c);
          }
        }
      }
    }

    return atoms;
  }




  // returns true if the given term is a pure arithmetic term
  private boolean isPurelyArithmetic(Formula f) {
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


  // gives the number of times that the variables in the formula are prime
  // works only on unistanitated variables
  public Set<Integer> howManyPrimes(Formula f){
    // TODO caching
    SetMultimap<Formula, Integer> cache = this.howManyPrimesCache;
    Deque<Formula> toProcess = new ArrayDeque<Formula>();

    toProcess.push(f);
    while (!toProcess.isEmpty()) {
      final Formula tt = toProcess.peek();
      if (cache.containsKey(f)) {
        toProcess.pop();
        continue;
      }
      final long t = getTerm(tt);

      if (msat_term_is_variable(t) != 0) {
        String s = msat_term_repr(t);
        String[] split = s.split(INDEX_SEPARATOR);

        Pair<String, Integer> data = null;
        if (split.length == 2){
          // formula was instantiated
          data = PathFormula.getPrimeData(split[0]);
        } else {
          // uninstantiated formula
          data = PathFormula.getPrimeData(s);
        }
        cache.put(tt, data.getSecond());
        toProcess.pop();
      }
      else if(msat_term_is_number(t) != 0) {
        cache.put(tt, null);
        toProcess.pop();
      }
      else {
        boolean childrenDone = true;
        int arity = msat_term_arity(t);
        Collection<Integer> primes = new HashSet<Integer>();
        for (int i = 0; i < arity; ++i) {
          Formula c = encapsulate(msat_term_get_arg(t, i));
          Collection<Integer> cachedResult = cache.get(c);
          if (!cachedResult.isEmpty()) {
            primes.addAll(cachedResult);
          } else {
            toProcess.push(c);
            childrenDone = false;
          }
        }

        if (childrenDone) {
          toProcess.pop();
          cache.putAll(tt, primes);
        }
      }
    }

    return cache.get(f);
  }

  @Override
  public Formula unprimeFormula(Formula formula) {
    Map<Formula, Formula> cache = this.unprimingCache;
    Deque<Formula> toProcess = new ArrayDeque<Formula>();

    toProcess.push(formula);
    while (!toProcess.isEmpty()) {
      final Formula tt = toProcess.peek();
      if (cache.containsKey(tt)) {
        toProcess.pop();
        continue;
      }
      final long t = getTerm(tt);

      if (msat_term_is_variable(t) != 0) {
        Pair<String, Integer> data = PathFormula.getPrimeData(msat_term_repr(t));
        // long newt = buildMsatVariable(name, msat_term_get_type(t));
        long newt = buildMsatVariable(data.getFirst(), msat_term_get_type(t));
        toProcess.pop();
        cache.put(tt, encapsulate(newt));
      } else {
        boolean childrenDone = true;
        long[] newargs = new long[msat_term_arity(t)];
        for (int i = 0; i < newargs.length; ++i) {
          Formula c = encapsulate(msat_term_get_arg(t, i));
          Formula newC = cache.get(c);
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

    return cache.get(formula);
  }

  /*@Override
  public Formula extractNonmodularFormula(Formula formula, Integer tid) {
    Map<Triple<Formula, Integer, Multimap<Integer, Integer>>, Formula> cache = unprimingNmCache;
    Deque<Formula> toProcess = new ArrayDeque<Formula>();

    toProcess.push(formula);
    while (!toProcess.isEmpty()) {
      final Formula tt = toProcess.peek();
      if (cache.containsKey(Triple.of(tt, tid, traceMap))) {
        toProcess.pop();
        continue;
      }
      final long t = getTerm(tt);

      if (msat_term_is_variable(t) != 0) {
        Pair<String, Integer> data = PathFormula.getPrimeData(msat_term_repr(t));
        // find the thread of the variable
        Integer sourceTid = null;
        for (Integer i : traceMap.keySet()){
          if (traceMap.get(i).contains(data.getSecond())){
            sourceTid = i;
            break;
          }
        }

        assert sourceTid != null;

        String name = null;
        if (sourceTid == tid){
          name = data.getFirst();
        } else {
          name = data.getFirst()+PathFormula.THREAD_SYMBOL+sourceTid;
        }

        long newt = buildMsatVariable(name, msat_term_get_type(t));
        toProcess.pop();
        cache.put(Triple.of(tt, tid, traceMap), encapsulate(newt));
      } else {
        boolean childrenDone = true;
        long[] newargs = new long[msat_term_arity(t)];
        for (int i = 0; i < newargs.length; ++i) {
          Formula c = encapsulate(msat_term_get_arg(t, i));
          Formula newC = cache.get(Triple.of(c, tid, traceMap));
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

          cache.put(Triple.of(tt, tid, traceMap), encapsulate(newt));
        }
      }
    }

    return cache.get(Triple.of(formula, tid, traceMap));
  }*/


  @Override
  // primed the variables inside the formula given number of times
  public Formula primeFormula(Formula f, int howManyPrimes) {
    Map<Pair<Formula, Integer>, Formula> cache = this.primingCache;
    Deque<Formula> toProcess = new ArrayDeque<Formula>();
    Pair<Formula, Integer> key = null;

    toProcess.push(f);
    while (!toProcess.isEmpty()) {
      final Formula tt = toProcess.peek();
      key = new Pair<Formula, Integer>(tt,howManyPrimes);
      if (cache.containsKey(key)) {
        toProcess.pop();
        continue;
      }
      final long t = getTerm(tt);

      if (msat_term_is_variable(t) != 0) {
        Pair<String, Integer> lVariable = parseName(msat_term_repr(t));
        String newName = PathFormula.primeVariable(lVariable.getFirst(),howManyPrimes);
        long newt = buildMsatVariable(makeName(newName, lVariable.getSecond()), msat_term_get_type(t));
        key = new Pair<Formula, Integer>(tt,howManyPrimes);
        cache.put(key, encapsulate(newt));

      } else {
        boolean childrenDone = true;
        long[] newargs = new long[msat_term_arity(t)];
        for (int i = 0; i < newargs.length; ++i) {
          Formula c = encapsulate(msat_term_get_arg(t, i));
          key = new Pair<Formula, Integer>(c,howManyPrimes);
          Formula newC = cache.get(key);
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

          key = new Pair<Formula, Integer>(tt,howManyPrimes);
          cache.put(key, encapsulate(newt));
        }
      }
    }

    key = new Pair<Formula, Integer>(f,howManyPrimes);
    Formula result = cache.get(key);
    assert result != null;
    return result;
  }

  @Override
  public Formula primeFormula(Formula f, int howManyPrimes, SSAMap ssa) {
    Map<Triple<Formula, Integer, SSAMap>, Formula> cache = new HashMap<Triple<Formula, Integer, SSAMap>, Formula>();
    Deque<Formula> toProcess = new ArrayDeque<Formula>();
    Pair<Formula, Integer> key = null;

    toProcess.push(f);
    while (!toProcess.isEmpty()) {
      final Formula tt = toProcess.peek();
      key = new Pair<Formula, Integer>(tt,howManyPrimes);
      if (cache.containsKey(Triple.of(f, howManyPrimes, ssa))) {
        toProcess.pop();
        continue;
      }
      final long t = getTerm(tt);

      if (msat_term_is_variable(t) != 0) {
        toProcess.pop();
        String repr = msat_term_repr(t);
        if (repr.contains(PathFormula.THREAD_SYMBOL)){
          Pair<String, Integer> pair = PathFormula.getNonModularData(repr);
          String name = pair.getFirst();
          Integer idx = ssa.getIndex(name);
          if (idx == null || idx < 1){
            idx = 1;
          }
          long newt = buildMsatVariable(makeName(name, idx), msat_term_get_type(t));
          cache.put(Triple.of(tt, howManyPrimes, ssa), encapsulate(newt));
        } else {
          Pair<String, Integer> lVariable = parseName(repr);
          String newName = PathFormula.primeVariable(lVariable.getFirst(),howManyPrimes);
          long newt = buildMsatVariable(makeName(newName, lVariable.getSecond()), msat_term_get_type(t));
          cache.put(Triple.of(tt, howManyPrimes, ssa), encapsulate(newt));
        }
      } else {
        boolean childrenDone = true;
        long[] newargs = new long[msat_term_arity(t)];
        for (int i = 0; i < newargs.length; ++i) {
          Formula c = encapsulate(msat_term_get_arg(t, i));
          Formula newC = cache.get(Triple.of(c, howManyPrimes, ssa));
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

          cache.put(Triple.of(tt, howManyPrimes, ssa), encapsulate(newt));
        }
      }
    }

    Formula result = cache.get(Triple.of(f, howManyPrimes, ssa));
    assert result != null;
    return result;
  }

  // count the number of atoms in the formula
  public int countAtoms(Formula f) {
    Map<Formula, Integer> cache = this.atomCountingCache;
    Deque<Formula> toProcess = new ArrayDeque<Formula>();

    toProcess.push(f);

    while (!toProcess.isEmpty()) {
      Formula tt = toProcess.peek();
      long t = getTerm(tt);
      if (cache.containsKey(tt)) {
        toProcess.pop();
        continue;
      }

      if (msat_term_is_true(t) !=0 || msat_term_is_false(t) !=0){
        cache.put(tt, 0);
      }
      else if (msat_term_is_atom(t) != 0) {
        cache.put(tt, 1);
      }
      else {
        int atomNo = 0;
        boolean childrenDone = true;
        for (int i=0; i<msat_term_arity(t); i++){
          Formula c = encapsulate(msat_term_get_arg(t, i));
          if (!cache.containsKey(c)) {
            toProcess.push(c);
            childrenDone = false;
          } else {
            atomNo = atomNo + cache.get(c);
          }
        }
        if (childrenDone){
          toProcess.pop();
          cache.put(tt, atomNo);
        }

      }
    }

    return cache.get(f);

  }

  // TODO could use caching
  @Override
  // reduces idenexes to the lowest possible value
  public Pair<Formula,Map<String, Integer>>  normalize(Formula formula) {
    Map<String, Integer> lowestIndex = new HashMap<String, Integer>();
    // gather information about the lowest index
    Deque<Formula> toProcess = new ArrayDeque<Formula>();

    toProcess.push(formula);
    while(!toProcess.isEmpty()){
      Formula f = toProcess.pop();
      long term = getTerm(f);
      if (msat_term_is_variable(term) != 0){
        Pair<String, Integer> data = parseName(msat_term_repr(term));
        Integer currentLowest = lowestIndex.get(data.getFirst());
        // update the index
        if (currentLowest==null || currentLowest > data.getSecond()){
          lowestIndex.put(data.getFirst(), data.getSecond());
        }
      } else {
        for (int i=0; i<msat_term_arity(term); i++) {
          Formula c = encapsulate(msat_term_get_arg(term, i));
          toProcess.push(c);
        }
      }
    }
    // TODO check if all minimal index == 2, the do nothing

    // rebuild the formula reducing indexes
    // cache for and index of subformulas entire formulas
    Map<Formula, Formula> lcache = new HashMap<Formula, Formula>();     // can't be global cache
    toProcess.push(formula);
    while (!toProcess.isEmpty()) {
      final Formula f = toProcess.peek();
      final long term = getTerm(f);
      if (msat_term_is_variable(term) != 0) {
        Pair<String, Integer> data = parseName(msat_term_repr(term));
        // adjust the index; the lowest is 2
        String name = data.getFirst();
        int nidx = data.getSecond() - lowestIndex.get(name)+2;
        long newterm = buildMsatVariable(makeName(name, nidx), msat_term_get_type(term));
        lcache.put(f, encapsulate(newterm));
        toProcess.pop();
      } else {
        boolean childrenDone = true;
        long[] newargs = new long[msat_term_arity(term)];
        for (int i = 0; i < newargs.length; ++i) {
          Formula c = encapsulate(msat_term_get_arg(term, i));
          Formula newC = lcache.get(c);
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
          if (msat_term_is_uif(term) != 0) {
            /* TODO not handled yet
             * String name = msat_decl_get_name(msat_term_get_decl(term));
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
            }*/
            newt = -1;
          } else {
            newt = msat_replace_args(msatEnv, term, newargs);
          }

          lcache.put(f, encapsulate(newt));
        }
      }
    }

    return new Pair<Formula,Map<String, Integer>> (lcache.get(formula), lowestIndex);
  }


  @Override
  public Formula adjustedPrimedNo(Formula formula, Map<Integer, Integer> primedMap) {
    if (primedMap.isEmpty()){
      return formula;
    }
    Map<Pair<Formula, Map<Integer, Integer>>, Formula> cache = this.adjustingCache;
    Deque<Pair<Formula,Map<Integer, Integer>>> toProcess = new ArrayDeque<Pair<Formula,Map<Integer, Integer>>>();
    Pair<Formula, Map<Integer, Integer>> key = null;

    toProcess.push(Pair.of(formula, primedMap));
    while (!toProcess.isEmpty()) {
      final Formula tt = toProcess.peek().getFirst();
      final Map<Integer, Integer> ttMap = toProcess.peek().getSecond();
      key = Pair.of(tt, ttMap);
      if (cache.containsKey(key)) {
        toProcess.pop();
        continue;
      }
      final long t = getTerm(tt);
      if (msat_term_is_variable(t) != 0) {
        String name = null;
        Integer index = null;
        String repr = msat_term_repr(t);
        String[] s = repr.split(INDEX_SEPARATOR);
        if (s.length == 2) {
          name = s[0];
          index = Integer.parseInt(s[1]);
        } else {
          name = s[0];
        }
        Pair<String, Integer> data = PathFormula.getPrimeData(name);
        Integer newPrimeNo = ttMap.get(data.getSecond());
        key = Pair.of(tt, ttMap);
        if (newPrimeNo != null){
          int shift = newPrimeNo - data.getSecond();
          String newName = PathFormula.primeVariable(name,shift);
          if (index != null){
            newName = makeName(newName, index);
          }
          long newt = buildMsatVariable(newName, msat_term_get_type(t));
          cache.put(key, encapsulate(newt));
        } else {
          cache.put(key, tt);
        }
      } else {
        boolean childrenDone = true;
        long[] newargs = new long[msat_term_arity(t)];
        for (int i = 0; i < newargs.length; ++i) {
          Formula c = encapsulate(msat_term_get_arg(t, i));
          key = Pair.of(c, ttMap);
          Formula newC = cache.get(key);
          if (newC != null) {
            newargs[i] = getTerm(newC);
          } else {
            toProcess.push(Pair.of(c, ttMap));
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

          key = Pair.of(tt, ttMap);
          cache.put(key, encapsulate(newt));
        }
      }
    }

    key = Pair.of(formula, primedMap);
    Formula result = cache.get(key);
    assert result != null;
    return result;
  }


  @Override
  public Map<String, Integer> getNonModularData(Formula formula) {

    Map<Formula, Map<String, Integer>> cache = nonModularCache;
    Deque<Formula> toProcess = new ArrayDeque<Formula>();

    toProcess.push(formula);
    while (!toProcess.isEmpty()) {
      final Formula tt = toProcess.peek();
      Map<String, Integer> cached = cache.get(tt);
      if (cached != null) {
        toProcess.pop();
        continue;
      }

      final long t = getTerm(tt);

      if (msat_term_is_variable(t) != 0) {
        Pair<String, Integer> data = PathFormula.getNonModularData(msat_term_repr(t));
        long newt = buildMsatVariable(data.getFirst(), msat_term_get_type(t));
        Map<String, Integer> val = new HashMap<String, Integer>();
        val.put(data.getFirst(), data.getSecond());
        cache.put(tt, val);
        toProcess.pop();
      } else {
        boolean childrenDone = true;
        for (int i = 0; i < msat_term_arity(t); ++i) {
          Formula c = encapsulate(msat_term_get_arg(t, i));
          Map<String, Integer> cachedChild = cache.get(c);
          if (cachedChild == null) {
            toProcess.push(c);
            childrenDone = false;
          }
        }

        if (childrenDone){
          toProcess.pop();
          Map<String, Integer> val = new HashMap<String, Integer>();
          for (int i = 0; i < msat_term_arity(t); ++i) {
            Formula c = encapsulate(msat_term_get_arg(t, i));
            Map<String, Integer> cachedChild = cache.get(c);
            assert cachedChild != null;

            val.putAll(cachedChild);
          }
          cache.put(tt, val);
        }
      }
    }

    return cache.get(formula);
  }



  @Override
  public List<Formula> nonModularInstances(Formula formula, ListMultimap<String, Integer> envMap) {
    if (envMap.isEmpty()){
      List<Formula> result = new Vector<Formula>(1);
      result.add(formula);
      return result;
    }

    ListMultimap<Pair<Formula, ListMultimap<String, Integer>>, Formula> cache = this.nmInstancesCache;
    Deque<Formula> toProcess = new ArrayDeque<Formula>();
    //Pair<Formula, Map<Integer, Integer>> key = null;

    toProcess.push(formula);
    while (!toProcess.isEmpty()) {
      final Formula tt = toProcess.peek();
      if (cache.containsKey(Pair.of(tt, envMap))) {
        toProcess.pop();
        continue;
      }
      final long t = getTerm(tt);

      if (msat_term_is_variable(t) != 0) {
        toProcess.pop();
        Pair<String, Integer> pair = PathFormula.getNonModularData(msat_term_repr(t));
        Integer sTid = pair.getSecond();
        assert sTid == null || sTid >= 0;
        if (sTid == null){
          cache.put(Pair.of(tt, envMap), tt);
        } else {
          String var = pair.getFirst();
          // create instances
          for (Integer primeNo : envMap.get(var)){
            String name = pair.getFirst()+"^"+primeNo;
            long newt = buildMsatVariable(name, msat_term_get_type(t));
            cache.put(Pair.of(tt, envMap), encapsulate(newt));
          }
          assert cache.get(Pair.of(tt, envMap)) != null;
        }
      } else {
        boolean childrenDone = true;
        List<Formula>[] fnewargs = new List[msat_term_arity(t)];
        for (int i = 0; i < fnewargs.length; ++i) {
          Formula c = encapsulate(msat_term_get_arg(t, i));
          List<Formula> newC = cache.get(Pair.of(c, envMap));
          if (newC.isEmpty()) {
            toProcess.push(c);
            childrenDone = false;
          } else {
            fnewargs[i] = cache.get(Pair.of(c, envMap));
          }
        }


        if (childrenDone) {
          toProcess.pop();
          List<long []> newargs = new Vector<long []>();
          // counters
          int[] ctr = new int[msat_term_arity(t)];

          boolean finish = false;
          while(!finish){
            long[] arg = new long[msat_term_arity(t)];

            for (int i=0; i < msat_term_arity(t); ++i){
              long v = getTerm(fnewargs[i].get(ctr[i]));
              arg[i] = v;
            }

            newargs.add(arg);

            finish = true;
            for (int i=0; i < fnewargs.length; ++i){
              if (ctr[i] < fnewargs[i].size()-1){
                ctr[i]++;
                for (int j=0; j<i ;j++){
                  ctr[j]=0;
                }
                finish = false;
                break;
              }
            }
          }

          List<Formula> newf = new Vector<Formula>();
          if (msat_term_is_uif(t) != 0) {
            String name = msat_decl_get_name(msat_term_get_decl(t));
            assert name != null;

            if (ufCanBeLvalue(name)) {
              name = parseName(name).getFirst();
              for (long[] newarg : newargs){
                long newt = buildMsatUF(name, newarg);
                newf.add(encapsulate(newt));
              }

              if (newargs.isEmpty()){
                long[] newarg = new long[0];
                long newt = buildMsatUF(name, newarg);
                newf.add(encapsulate(newt));
              }

            } else {
              for (long[] newarg : newargs){
                long newt = msat_replace_args(msatEnv, t, newarg);
                newf.add(encapsulate(newt));
              }

              if (newargs.isEmpty()){
                long[] newarg = new long[0];
                long newt = msat_replace_args(msatEnv, t, newarg);;
                newf.add(encapsulate(newt));
              }
            }
          } else {
            for (long[] newarg : newargs){
              long newt = msat_replace_args(msatEnv, t, newarg);
              newf.add(encapsulate(newt));
            }

            if (newargs.isEmpty()){
              long[] newarg = new long[0];
              long newt = msat_replace_args(msatEnv, t, newarg);;
              newf.add(encapsulate(newt));
            }
          }
          cache.putAll(Pair.of(tt, envMap), newf);
        }
      }
    }

    return cache.get(Pair.of(formula, envMap));
  }


  @Override
  public Formula removePrimed(Formula formula,  Set<Integer> primesToRemove) {

    if (primesToRemove.isEmpty()){
      return formula;
    }

    // TODO maybe cache?
    Map<Pair<Formula, Set<Integer>>, Formula> cache = new HashMap<Pair<Formula, Set<Integer>>, Formula>();
    // formulas that have to be removed
    Set<Formula> toRemove = new HashSet<Formula>();
    // formulas that mustn't be removed
    Set<Formula> mustStay = new HashSet<Formula>();
    Deque<Formula> toProcess = new ArrayDeque<Formula>();
    toProcess.push(formula);

    while (!toProcess.isEmpty()) {
      final Formula tt = toProcess.peek();
      if (cache.containsKey(Pair.of(tt, primesToRemove))) {
        toProcess.pop();
        continue;
      }
      final long t = getTerm(tt);

      if (msat_term_is_variable(t) != 0) {
        toProcess.pop();
        Pair<String, Integer> lVariable = parseName(msat_term_repr(t));
        String name = lVariable.getFirst();
        Pair<String, Integer> data = PathFormula.getPrimeData(name);
        Integer primedNo = data.getSecond();
        if (primesToRemove.contains(primedNo)){
          toRemove.add(tt);
        } else {
          mustStay.add(tt);
        }
        cache.put(Pair.of(tt, primesToRemove), tt);
      }
      else {
        boolean childrenDone    = true;
        // at least one argument can be removed
        boolean argToRemove     = false;
        // al least one argument should stay
        boolean argToStay       = false;
        // all arguments can be removed (true if no arguments)
        boolean allArgToRemove  = true;
        boolean termArg         = false;

        int arity = msat_term_arity(t);
        long[] newargs = new long[arity];
        for (int i = 0; i < newargs.length; ++i) {
          long tc = msat_term_get_arg(t, i);
          Formula c = encapsulate(tc);

          if (msat_term_get_type(tc) == MSAT_INT || msat_term_get_type(tc) == MSAT_REAL){
            termArg = true;
          }

          if (toRemove.contains(c)){
            argToRemove = true;
          }
          else if (mustStay.contains(c)) {
            argToStay = true;
            allArgToRemove = false;
          }
          else {
            allArgToRemove = false;
          }

          Formula newC = cache.get(Pair.of(c, primesToRemove));
          if (newC != null) {
            newargs[i] = getTerm(newC);
          } else {
            toProcess.push(c);
            childrenDone = false;
          }
        }

        if (childrenDone) {
          toProcess.pop();
          Formula newF = null;

          if(!termArg){
            if (allArgToRemove && argToRemove){
              newF = makeTrue();
              toRemove.add(tt);
            }
            else {
              // replace
              long newt = 0;
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
              newF = encapsulate(newt);
            }
          }
          else {
            if (argToRemove && !argToStay){
              newF = makeTrue();
              toRemove.add(tt);
            }
            else if (!argToRemove && !argToStay){
              newF = tt;
            } else {
              newF = tt;
              mustStay.add(tt);
            }
          }

          assert newF != null;
          cache.put(Pair.of(tt, primesToRemove), newF);
        }
      }
    }

    Formula result = cache.get(Pair.of(formula, primesToRemove));
    System.out.println(formula+"\t\t->\t\t"+result);
    assert result != null;
    return result;
  }


  @Override
  public List<Formula> extractNonModularAtoms(Formula itp, int tid) {
    Set<Formula> cache      = new HashSet<Formula>();
    List<Formula> atoms     = new ArrayList<Formula>();
    Set<Integer>  uninstSet = new HashSet<Integer>(1);
    uninstSet.add(tid);
    Deque<Formula> toProcess = new ArrayDeque<Formula>();
    toProcess.push(itp);

    while (!toProcess.isEmpty()) {
      Formula tt = toProcess.pop();
      long t = getTerm(tt);
      assert !cache.contains(tt);
      cache.add(tt);

      if (msat_term_is_true(t) != 0 || msat_term_is_false(t) != 0) {
        continue;
      }

      if (msat_term_is_atom(t) != 0) {
        tt = uninstantiateSelected(tt, uninstSet);
        t = getTerm(tt);
        atoms.add(tt);
      }
      else {
        for (int i = 0; i < msat_term_arity(t); ++i){
          Formula c = encapsulate(msat_term_get_arg(t, i));
          if (!cache.contains(c)) {
            toProcess.push(c);
          }
        }
      }
    }

    return atoms;
  }


  /**
   * Uninstantiates variables, whose prime numbers belongs to the set.
   * @param fr
   * @param set
   * @return
   */
  private Formula uninstantiateSelected(Formula fr, Set<Integer> set) {
    Map<Pair<Formula, Set<Integer>>, Formula> cache = uninstantiateSelectedCache;
    Deque<Formula> toProcess = new ArrayDeque<Formula>();

    toProcess.push(fr);
    while (!toProcess.isEmpty()) {
      final Formula tt = toProcess.peek();
      if (cache.containsKey(Pair.of(tt, set))) {
        toProcess.pop();
        continue;
      }
      final long t = getTerm(tt);

      if (msat_term_is_variable(t) != 0) {
        toProcess.pop();
        String name = msat_term_repr(t);
        Pair<String, Integer> iData = parseName(name);
        Pair<String, Integer> pData = PathFormula.getPrimeData(iData.getFirst());

        if (pData.getSecond() != null && set.contains(pData.getSecond())){
          name = iData.getFirst();
        }

        long newt = buildMsatVariable(name, msat_term_get_type(t));
        cache.put(Pair.of(tt, set), encapsulate(newt));

      } else {
        boolean childrenDone = true;
        long[] newargs = new long[msat_term_arity(t)];
        for (int i = 0; i < newargs.length; ++i) {
          Formula c = encapsulate(msat_term_get_arg(t, i));
          Formula newC = cache.get(Pair.of(c, set));
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

          cache.put(Pair.of(tt, set), encapsulate(newt));
        }
      }
    }

    Formula result = cache.get(Pair.of(fr, set));
    assert result != null;
    return result;
  }


  @Override
  public Formula instantiateModular(Formula fr, SSAMap ssa) {
    Deque<Formula> toProcess = new ArrayDeque<Formula>();
    Map<Formula, Formula> cache = new HashMap<Formula, Formula>();

    toProcess.push(fr);
    while (!toProcess.isEmpty()) {
      final Formula tt = toProcess.peek();
      if (cache.containsKey(tt)) {
        toProcess.pop();
        continue;
      }
      final long t = getTerm(tt);

      if (msat_term_is_variable(t) != 0) {
        toProcess.pop();
        String name = msat_term_repr(t);

        String[] s = name.split(INDEX_SEPARATOR);
        assert s.length == 1 || s.length == 2;

        String newName = null;
        if (s.length == 1){
          int idx = ssa.getIndex(name);
          if (idx > 0) {
            // ok, the variable has an instance in the SSA, replace it
            newName = makeName(name, idx);
          } else {
            // TODO changes
            // the variable is not used in the SSA, keep it as is
            newName = makeName(name, 1);
          }
        }
        else {
          // variable already instantiated
          newName = name;
        }

        long newt = buildMsatVariable(newName, msat_term_get_type(t));
        cache.put(tt, encapsulate(newt));
      } else {
        boolean childrenDone = true;
        long[] newargs = new long[msat_term_arity(t)];
        for (int i = 0; i < newargs.length; ++i) {
          Formula c = encapsulate(msat_term_get_arg(t, i));
          Formula newC = cache.get(c);
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

    Formula result = cache.get(fr);
    assert result != null;
    return result;
  }


  @Override
  public Formula renameIndexes(Formula fr, SSAMap ssa, int p) {

    Map<Formula, Formula> cache = new HashMap<Formula, Formula>();
    Deque<Formula> toProcess = new ArrayDeque<Formula>();

    toProcess.push(fr);
    while (!toProcess.isEmpty()) {
      final Formula tt = toProcess.peek();

      if (cache.containsKey(tt)) {
        toProcess.pop();
        continue;
      }
      final long t = getTerm(tt);

      if (msat_term_is_variable(t) != 0) {
        Pair<String, Integer> lVariable = parseName(msat_term_repr(t));
        String var = lVariable.getFirst();
        int idx   = lVariable.getSecond();

        if (idx < ssa.getIndex(var)){
          // rename
          Pair<String, Integer> data = PathFormula.getPrimeData(var);
          var = data.getFirst()+ PathFormula.PRIME_SYMBOL+p;
          long newt = buildMsatVariable(makeName(var, idx), msat_term_get_type(t));
          cache.put(tt, encapsulate(newt));
        } else {
          cache.put(tt, tt);
        }
      } else {
        boolean childrenDone = true;
        long[] newargs = new long[msat_term_arity(t)];
        for (int i = 0; i < newargs.length; ++i) {
          Formula c = encapsulate(msat_term_get_arg(t, i));
          Formula newC = cache.get(c);
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

    Formula result = cache.get(fr);
    assert result != null;
    return result;
  }












}
