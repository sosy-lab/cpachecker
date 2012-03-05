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
package org.sosy_lab.cpachecker.util.predicates.mathsat5;

import static org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5NativeApi.*;

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
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaList;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

import com.google.common.base.Preconditions;

@Options(prefix = "cpa.predicate.mathsat")
public abstract class Mathsat5FormulaManager implements FormulaManager {

  @Option(description = "Use UIFs (recommended because its more precise)")
  private boolean useUIFs = true;

  // the MathSAT environment in which all terms are created
  // default visibility because its heavily used in sub-classes
  final long msatEnv;
  final long msatConf;



  // UF encoding of some unsupported operations
  private final long stringLitUfDecl;

  // datatype to use for variables, when converting them to mathsat vars
  // can be either MSAT_REAL or MSAT_INT
  // Note that MSAT_INT does not mean that we support the full linear
  // integer arithmetic (LIA)! At the moment, interpolation doesn't work on
  // LIA, only difference logic or on LRA (i.e. on the rationals). However
  // by setting the vars to be MSAT_INT, the solver tries some heuristics
  // that might work (e.g. tightening of a < b into a <= b - 1, splitting
  // negated equalities, ...)
  private final long msatVarType;

  // the character for separating name and index of a value
  private static final String INDEX_SEPARATOR = "@";

  // variable name and index that is used for replacing UIFs when they are disabled
  private static final String UIF_VARIABLE = "__uif__";
  private int uifVariableCounter = 0;

  // various caches for speeding up expensive tasks
  //
  // cache for splitting arithmetic equalities in extractAtoms
  private final Map<Formula, Boolean> arithCache = new HashMap<Formula, Boolean>();

  // cache for uninstantiating terms (see uninstantiate() below)
  private final Map<Formula, Formula> uninstantiateCache = new HashMap<Formula, Formula>();

  private final Formula trueFormula;
  private final Formula falseFormula;

  Mathsat5FormulaManager(Configuration config, LogManager logger, int pVarType) throws InvalidConfigurationException {
    config.inject(this, Mathsat5FormulaManager.class);


    msatConf = msat_create_config();

    msatEnv = msat_create_env(msatConf);
    msatVarType = Mathsat5NativeApi.get_msat_type_struct(msatEnv, pVarType);

    trueFormula = encapsulate(msat_make_true(msatEnv));
    falseFormula = encapsulate(msat_make_false(msatEnv));

    long integer_type = msat_get_integer_type(msatEnv);

    long stringLitUfDeclType = msat_get_function_type(msatEnv, new long[] { integer_type }, 1, msatVarType);
    stringLitUfDecl = msat_declare_function(msatEnv, "__string__", stringLitUfDeclType);
  }

  long getMsatEnv() {
    return msatEnv;
  }

  long createEnvironment(long cfg, boolean shared, boolean ghostFilter) {
    long env;

    if (ghostFilter) {
      msat_set_option(cfg, "ghost_filtering", "true");
    }

    if (shared) {
      env = msat_create_shared_env(cfg, msatEnv);
    } else {
      env = msat_create_env(cfg);
    }

    return env;
  }

  static long getTerm(Formula f) {
    return ((Mathsat5Formula) f).getTerm();
  }

  static long[] getTerm(FormulaList f) {
    return ((Mathsat5FormulaList) f).getTerms();
  }

  protected Formula encapsulate(long t) {
    return new Mathsat5Formula(msatEnv, t);
  }

  private static FormulaList encapsulate(long[] t) {
    return new Mathsat5FormulaList(t);
  }

  private static String makeName(String name, int idx) {
    return name + INDEX_SEPARATOR + idx;
  }

  static Pair<String, Integer> parseName(String var) {
    String[] s = var.split(INDEX_SEPARATOR);
    if (s.length != 2) { throw new IllegalArgumentException("Not an instantiated variable: " + var); }

    return Pair.of(s[0], Integer.parseInt(s[1]));
  }

  abstract long interpreteBitvector(long bv);

  // ----------------- Boolean formulas -----------------

  @Override
  public boolean isBoolean(Formula f) {

    long a = Mathsat5NativeApi.msat_term_get_type(getTerm(f));



    return Mathsat5NativeApi.msat_is_bool_type(msatEnv, a) == 1;
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

    long f1Type = msat_term_get_type(getTerm(f1));
    long f2Type = msat_term_get_type(getTerm(f2));

    // there currently exists a bug in the msat-api, where make_equal with 2 bool-types results in error-term
    if (msat_is_bool_type(msatEnv, f1Type) == 1 && msat_is_bool_type(msatEnv, f2Type) == 1) {
      return encapsulate(msat_make_iff(msatEnv, getTerm(f1), getTerm(f2)));
    } else {
      return encapsulate(msat_make_equal(msatEnv, getTerm(f1), getTerm(f2)));
    }



  }

  @Override
  public Formula makeIfThenElse(Formula condition, Formula f1, Formula f2) {
    long t;
    long f1Type = msat_term_get_type(getTerm(f1));
    long f2Type = msat_term_get_type(getTerm(f2));

    // ite currently doesnt work with bool-types as branch arguments
    if (msat_is_bool_type(msatEnv, f1Type) == 0 || msat_is_bool_type(msatEnv, f2Type) == 0) {
      t = msat_make_term_ite(msatEnv, getTerm(condition), getTerm(f1), getTerm(f2));
    } else {
      t =
          msat_make_and(msatEnv, msat_make_or(msatEnv, msat_make_not(msatEnv, getTerm(condition)), getTerm(f1)),
              msat_make_or(msatEnv, getTerm(condition), getTerm(f2)));
    }
    return encapsulate(t);
  }


  // ----------------- Uninterpreted functions -----------------

  private long buildMsatUF(String name, long[] args, boolean predicate) {
    if (!useUIFs) { return buildMsatUfReplacement(); // shortcut
    }

    // only build a function when there actually are arguments
    if (args.length > 0) {
      long[] tp;
      long boolType, funcType, decl;

      boolType = msat_get_bool_type(msatEnv);

      tp = new long[args.length];
      Arrays.fill(tp, predicate ? boolType : msatVarType);

      funcType = msat_get_function_type(msatEnv, tp, tp.length, predicate ? boolType : msatVarType);

      decl = msat_declare_function(msatEnv, name, funcType);

      if (MSAT_ERROR_DECL(decl)) { return MSAT_MAKE_ERROR_TERM(); }

      return buildMsatUF(decl, args);
    } else {
      long type, decl;

      type = msat_get_simple_type(msatEnv, name);
      decl = msat_declare_function(msatEnv, name, type);

      return msat_make_constant(msatEnv, decl);
    }

  }

  /**
   * Replacement for msat_make_uif, never call that method directly!
   */
  protected long buildMsatUF(long func, long[] args) {
    if (useUIFs) {

      long t = msat_make_uf(msatEnv, func, args);
      assert (!MSAT_ERROR_TERM(t));
      return t;
    } else {
      return buildMsatUfReplacement();
    }
  }

  private long buildMsatUfReplacement() {
    // just create a fresh variable
    String var = makeName(UIF_VARIABLE, ++uifVariableCounter);
    long decl = msat_declare_function(msatEnv, var, msatVarType);

    long t = msat_make_constant(msatEnv, decl);
    assert (!MSAT_ERROR_TERM(t));
    return t;
  }

  @Override
  public Formula makeUIP(String name, FormulaList args) {
    return encapsulate(buildMsatUF(name, getTerm(args), true));
  }

  @Override
  public Formula makeUIF(String name, FormulaList args) {
    return encapsulate(buildMsatUF(name, getTerm(args), false));
  }

  @Override
  public Formula makeUIF(String name, FormulaList args, int idx) {
    return encapsulate(buildMsatUF(makeName(name, idx), getTerm(args), false));
  }

  // ----------------- Other formulas -----------------

  @Override
  public Formula makeString(int i) {
    long n = msat_make_number(msatEnv, Integer.toString(i));
    assert (!MSAT_ERROR_TERM(n));
    return encapsulate(buildMsatUF(stringLitUfDecl, new long[] { n }));
  }

  private long buildMsatVariable(String var, long type) {
    long decl = msat_declare_function(msatEnv, var, type);

    long t = msat_make_constant(msatEnv, decl);
    assert (!MSAT_ERROR_TERM(t));
    return t;
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

    long bool_type = msat_get_bool_type(msatEnv);

    long decl = msat_declare_function(msatEnv, name, bool_type);
    long t = msat_make_constant(msatEnv, decl);
    assert (!MSAT_ERROR_TERM(t));
    return encapsulate(t);
  }

  @Override
  public Formula makeAssignment(Formula f1, Formula f2) {
    return makeEqual(f1, f2);
  }


  // ----------------- Convert to list -----------------

  @Override
  public FormulaList makeList(Formula pF) {
    return new Mathsat5FormulaList(getTerm(pF));
  }

  @Override
  public FormulaList makeList(Formula pF1, Formula pF2) {
    return new Mathsat5FormulaList(getTerm(pF1), getTerm(pF2));
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

    String repr = (msat_term_is_atom(msatEnv, t) != 0)
        ? msat_term_repr(t) : ("#" + msat_term_id(t));

    long bool_type = get_msat_type_struct(msatEnv, MSAT_BOOL);
    long d = msat_declare_function(msatEnv, "\"PRED" + repr + "\"", bool_type);
    long var = msat_make_constant(msatEnv, d);
    assert (!MSAT_ERROR_TERM(var));
    return encapsulate(var);
  }

  @Override
  public String dumpFormula(Formula f) {
    return msat_to_smtlib2(msatEnv, getTerm(f));
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
    assert (!MSAT_ERROR_TERM(f));
    return encapsulate(f);
  }

  @Override
  public Formula parse(String s) {
    long f = msat_from_smtlib2(msatEnv, s);
    Preconditions.checkArgument(!MSAT_ERROR_TERM(f),
        "Could not parse formula %s as Mathsat formula.", s);
    assert (!MSAT_ERROR_TERM(f));
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

      if (msat_term_is_constant(msatEnv, t) != 0) {
        toProcess.pop();
        String name = msat_term_repr(t);
        int idx = ssa.getIndex(name);
        if (idx > 0) {
          // ok, the variable has an instance in the SSA, replace it
          long newt = buildMsatVariable(makeName(name, idx), msat_term_get_type(t));
          assert (!MSAT_ERROR_TERM(newt));
          cache.put(tt, encapsulate(newt));
        } else {
          // the variable is not used in the SSA, keep it as is
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

          if (msat_term_is_uf(msatEnv, t) != 0) {
            String name = msat_decl_get_name(msat_term_get_decl(t));
            assert name != null;

            if (ufCanBeLvalue(name)) {
              int idx = ssa.getIndex(name, encapsulate(newargs));
              if (idx > 0) {
                // ok, the variable has an instance in the SSA, replace it
                newt = buildMsatUF(makeName(name, idx), newargs, false);
                assert (!MSAT_ERROR_TERM(newt));
              } else {
                long tDecl = msat_term_get_decl(t);
                newt = msat_make_term(msatEnv, tDecl, newargs);
                assert (!MSAT_ERROR_TERM(newt));
              }
            } else {
              long tDecl = msat_term_get_decl(t);
              newt = msat_make_term(msatEnv, tDecl, newargs);
              assert (!MSAT_ERROR_TERM(newt));

            }
          } else {

            long tDecl = msat_term_get_decl(t);
            if (newargs.length > 0) {
              newt = msat_make_term(msatEnv, tDecl, newargs);
              assert (!MSAT_ERROR_TERM(newt));
            } else
              newt = msat_make_constant(msatEnv, tDecl);
              assert (!MSAT_ERROR_TERM(newt));
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

      if (msat_term_is_constant(msatEnv, t) != 0) {
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
          if (msat_term_is_uf(msatEnv, t) != 0) {
            String name = msat_decl_get_name(msat_term_get_decl(t));
            assert name != null;

            if (ufCanBeLvalue(name)) {
              name = parseName(name).getFirst();

              newt = buildMsatUF(name, newargs, false);
              assert (!MSAT_ERROR_TERM(newt));
            } else {
              long tDecl = msat_term_get_decl(t);
              newt = msat_make_term(msatEnv, tDecl, newargs);
              assert (!MSAT_ERROR_TERM(newt));
            }
          } else {
            long tDecl = msat_term_get_decl(t);
            newt = msat_make_term(msatEnv, tDecl, newargs);
            assert (!MSAT_ERROR_TERM(newt));
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
  public Collection<Formula> extractAtoms(Formula f,
      boolean splitArithEqualities, boolean conjunctionsOnly) {
    Set<Formula> handled = new HashSet<Formula>();
    List<Formula> atoms = new ArrayList<Formula>();

    Deque<Formula> toProcess = new ArrayDeque<Formula>();
    toProcess.push(f);
    handled.add(f);

    while (!toProcess.isEmpty()) {
      Formula tt = toProcess.pop();
      long t = getTerm(tt);
      assert handled.contains(tt);

      if (msat_term_is_true(msatEnv, t) != 0 || msat_term_is_false(msatEnv, t) != 0) {
        continue;
      }

      if (msat_term_is_atom(msatEnv, t) != 0) {
        tt = uninstantiate(tt);
        t = getTerm(tt);

        if (splitArithEqualities
            && (msat_term_is_equal(msatEnv, t) != 0)
            && isPurelyArithmetic(tt)) {
          long a1 = msat_term_get_arg(t, 0);
          assert (!MSAT_ERROR_TERM(a1));
          long a2 = msat_term_get_arg(t, 1);
          assert (!MSAT_ERROR_TERM(a2));
          long t1 = msat_make_leq(msatEnv, a1, a2);
          assert (!MSAT_ERROR_TERM(t1));
          //long t2 = msat_make_leq(msatEnv, a2, a1);
          Formula tt1 = encapsulate(t1);
          //SymbolicFormula tt2 = encapsulate(t2);
          handled.add(tt1);
          //cache.add(tt2);
          atoms.add(tt1);
          //atoms.add(tt2);
          atoms.add(tt);
        } else {
          atoms.add(tt);
        }

      } else if (conjunctionsOnly
          && !((msat_term_is_not(msatEnv, t) != 0) || (msat_term_is_and(msatEnv, t) != 0))) {
        // conjunctions only, but formula is neither "not" nor "and"
        // treat this as atomic
        atoms.add(uninstantiate(tt));

      } else {
        // ok, go into this formula
        for (int i = 0; i < msat_term_arity(t); ++i) {
          long newt = msat_term_get_arg(t, i);
          assert (!MSAT_ERROR_TERM(newt));
          Formula c = encapsulate(newt);
          if (handled.add(c)) {
            toProcess.push(c);
          }
        }
      }
    }

    return atoms;
  }

  @Override
  public Set<String> extractVariables(Formula f) {
    Set<Formula> seen = new HashSet<Formula>();
    Set<String> vars = new HashSet<String>();

    Deque<Formula> toProcess = new ArrayDeque<Formula>();
    toProcess.push(f);

    while (!toProcess.isEmpty()) {
      long t = getTerm(toProcess.pop());

      if (msat_term_is_true(msatEnv, t) != 0 || msat_term_is_false(msatEnv, t) != 0) {
        continue;
      }

      if (msat_term_is_constant(msatEnv, t) != 0) {
        vars.add(msat_term_repr(t));

      } else {
        // ok, go into this formula
        for (int i = 0; i < msat_term_arity(t); ++i) {
          long newt = msat_term_get_arg(t, i);
          assert (!MSAT_ERROR_TERM(newt));
          Formula c = encapsulate(newt);

          if (seen.add(c)) {
            toProcess.push(c);
          }
        }
      }
    }

    return vars;
  }

  // returns true if the given term is a pure arithmetic term
  private boolean isPurelyArithmetic(Formula f) {
    Boolean result = arithCache.get(f);
    if (result != null) { return result; }

    long t = getTerm(f);

    boolean res = true;
    if (msat_term_is_uf(msatEnv, t) != 0) {
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

  @Override
  public boolean checkSyntacticEntails(Formula leftFormula, Formula rightFormula) {
    final long leftTerm = getTerm(leftFormula);

    Deque<Long> toProcess = new ArrayDeque<Long>();
    Set<Long> seen = new HashSet<Long>();

    toProcess.push(getTerm(rightFormula));
    while (!toProcess.isEmpty()) {
      final long rightSubTerm = toProcess.pop();

      if (rightSubTerm == leftTerm) { return true; }

      if (Mathsat5NativeApi.msat_term_is_constant(msatEnv, rightSubTerm) == 0) {
        int args = msat_term_arity(rightSubTerm);
        for (int i = 0; i < args; ++i) {
          long arg = msat_term_get_arg(rightSubTerm, i);
          assert (!MSAT_ERROR_TERM(arg));

          if (!seen.contains(arg)) {
            toProcess.add(arg);
            seen.add(arg);
          }
        }
      }
    }

    return false;
  }

  @Override
  public Formula[] getArguments(Formula f) {
    final long t = getTerm(f);
    int arity = msat_term_arity(t);
    Formula[] result = new Formula[arity];
    for (int i = 0; i < arity; ++i) {
      result[i] = encapsulate(msat_term_get_arg(t, i));
    }
    return result;
  }

  @Override
  public void declareUIP(String name, int argCount) {
    long[] tp;
    long boolType, funcType;

    boolType = msat_get_bool_type(msatEnv);

    tp = new long[argCount];
    Arrays.fill(tp, boolType);

    funcType = msat_get_function_type(msatEnv, tp, tp.length, boolType);

    msat_declare_function(msatEnv, name, funcType);
  }

  @Override
  public String getVersion() {
//    return msat_get_version(); // not implemented
    return "MathSAT 5";
  }
}
