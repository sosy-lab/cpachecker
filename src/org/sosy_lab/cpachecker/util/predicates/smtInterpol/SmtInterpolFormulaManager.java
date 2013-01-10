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

import java.io.StringReader;
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
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaList;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smtInterpol.SmtInterpolEnvironment.Type;

import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;

public abstract class SmtInterpolFormulaManager implements FormulaManager {

  // the environment in which all terms are created
  final SmtInterpolEnvironment env;
  Type type; // INT or REAL, depends on logic

  // set to store uninterpreted functions, later we check, if a term is a UIF.
  Set<Term> uifs = new HashSet<Term>();

  // the character for separating name and index of a value
  private final static String INDEX_SEPARATOR = "@";

  // various caches for speeding up expensive tasks
  //
  // cache for splitting arithmetic equalities in extractAtoms
  private final Map<Formula, Boolean> arithCache = new HashMap<Formula, Boolean>();

  // cache for uninstantiating terms (see uninstantiate() below)
  private final Map<Formula, Formula> uninstantiateCache = new HashMap<Formula, Formula>();

  Term falseTerm;
  Term trueTerm;

  SmtInterpolFormulaManager(Configuration config, LogManager logger, Type type)
      throws InvalidConfigurationException {
    this.env = new SmtInterpolEnvironment(config);
    this.type = type;
  }

  /** This method returns a 'shared' environment or
   * a complete new environment. */
  SmtInterpolEnvironment createEnvironment() {
    assert env != null;
    return env;
  }

  // ----------------- Helper function -----------------

  /** ApplicationTerms can be wrapped with "|".
   * This function removes those chars. */
  static String dequote(String s) {
    return s.replace("|", "");
  }

  /** ApplicationTerms can be wrapped with "|".
   * This function replaces those chars with "\"". */
  private static String convertQuotes(String s) {
    return s.replace("|", "\"");
  }

  private String makeName(String name, int idx) {
    return name + INDEX_SEPARATOR + idx;
  }

  static Pair<String, Integer> parseName(String var) {
    String[] s = dequote(var).split(INDEX_SEPARATOR);
    if (s.length != 2) { throw new IllegalArgumentException(
        "Not an instantiated variable: " + var); }

    return Pair.of(s[0], Integer.parseInt(s[1]));
  }

  protected static Term getTerm(Formula f) {
    return ((SmtInterpolFormula) f).getTerm();
  }

  protected static Term[] getTerm(FormulaList f) {
    return ((SmtInterpolFormulaList) f).getTerms();
  }

  protected static Formula encapsulate(Term t) {
    return new SmtInterpolFormula(t);
  }

  protected static FormulaList encapsulate(Term[] t) {
    return new SmtInterpolFormulaList(t);
  }

  protected Term getTrueTerm() {
    if (trueTerm == null) { trueTerm = env.term("true"); }
    return trueTerm;
  }

  private Term getFalseTerm() {
    if (falseTerm == null) { falseTerm = env.term("false"); }
    return falseTerm;
  }

  // ----------------- Boolean formulas -----------------

  @Override
  public boolean isBoolean(Formula f) {
    return SmtInterpolUtil.isBoolean(getTerm(f));
  }

  @Override
  public Formula makeTrue() {
    return encapsulate(getTrueTerm());
  }

  @Override
  public Formula makeFalse() {
    return encapsulate(getFalseTerm());
  }

  @Override
  public Formula makeNot(Formula f) {
    Term t = getTerm(f);

    // simplify term (not not t)
    if (isNot(t)) {
      return encapsulate(((ApplicationTerm) t).getParameters()[0]);
    } else {
      return encapsulate(env.term("not", t));
    }
  }

  @Override
  public Formula makeAnd(Formula f1, Formula f2) {
    Term t1 = getTerm(f1);
    Term t2 = getTerm(f2);
    if (t1 == t2) { return f1;}
    if (t1 == getTrueTerm()) { return f2;}
    if (t2 == getTrueTerm()) { return f1;}
    Term t = env.term("and", t1, t2);
    return encapsulate(simplify(env, t));
  }

  @Override
  public Formula makeOr(Formula f1, Formula f2) {
    Term t1 = getTerm(f1);
    Term t2 = getTerm(f2);
    if (t1 == getFalseTerm()) { return f2;}
    if (t2 == getFalseTerm()) { return f1;}
    Term t = env.term("or", t1, t2);
    return encapsulate(simplify(env, t));
  }

  @Override
  public Formula makeEquivalence(Formula f1, Formula f2) {
    Term t1 = getTerm(f1);
    Term t2 = getTerm(f2);
    Sort booleanSort = env.sort(Type.BOOL);
    assert t1.getSort() == booleanSort && t2.getSort() == booleanSort :
      "Cannot make equivalence of non-boolean terms:\nTerm 1:\n" +
      t1.toStringDirect() + "\nTerm 2:\n" + t2.toStringDirect();
    return encapsulate(env.term("=", t1, t2));
  }

  @Override
  public Formula makeIfThenElse(Formula condition, Formula f1, Formula f2) {
    return encapsulate(env.term("ite",
          getTerm(condition), getTerm(f1), getTerm(f2)));
  }

  // ----------------- Uninterpreted functions -----------------

  protected Term buildUF(String name, Term[] args, boolean predicate) {
    Sort[] sorts = new Sort[args.length];
    for (int i = 0; i < args.length; i++) {
      sorts[i] = args[i].getSort();
    }

    env.declareFun(name, sorts, env.sort(predicate ? Type.BOOL : type));
    return env.term(name, args);
  }

  @Override
  public Formula makeUIP(String name, FormulaList args) {
    Term uif = buildUF(name, getTerm(args), true);
    uifs.add(uif);
    return encapsulate(uif);
  }

  @Override
  public Formula makeUIF(String name, FormulaList args) {
    Term uif = buildUF(name, getTerm(args), false);
    uifs.add(uif);
    return encapsulate(uif);
  }

  @Override
  public Formula makeUIF(String name, FormulaList args, int idx) {
    Term uif = buildUF(makeName(name, idx), getTerm(args), false);
    uifs.add(uif);
    return encapsulate(uif);
  }

  @Override
  public void declareUIP(String name, int argCount) {
    Sort[] sorts = new Sort[argCount];
    Arrays.fill(sorts, env.sort(Type.BOOL));
    env.declareFun(name, sorts, env.sort(Type.BOOL));
  }

  // ----------------- Other formulas -----------------

  @Override
  public Formula makeString(int i) {
    return encapsulate(env.numeral(Integer.toString(i)));
  }

  private Term buildVariable(String var, Sort sort) {
     env.declareFun(var, new Sort[]{}, sort);
     return env.term(var);
  }

  @Override
  public Formula makeVariable(String var, int idx) {
    return makeVariable(makeName(var, idx));
  }

  @Override
  public Formula makeVariable(String var) {
    return encapsulate(buildVariable(var, env.sort(type)));
  }

  @Override
  public Formula makePredicateVariable(String var, int idx) {
    return encapsulate(buildVariable(makeName("PRED_" + var, idx), env.sort(Type.BOOL)));
  }

  @Override
  public Formula makeAssignment(Formula f1, Formula f2) {
    return makeEqual(f1, f2);
  }

  // ----------------- Convert to list -----------------

  @Override
  public FormulaList makeList(Formula pF) {
    return new SmtInterpolFormulaList(getTerm(pF));
  }

  @Override
  public FormulaList makeList(Formula pF1, Formula pF2) {
    return new SmtInterpolFormulaList(getTerm(pF1), getTerm(pF2));
  }

  @Override
  public FormulaList makeList(List<Formula> pFs) {
    Term[] t = new Term[pFs.size()];
    for (int i = 0; i < t.length; i++) {
      t[i] = getTerm(pFs.get(i));
    }
    return encapsulate(t);
  }

  // ----------------- Complex formula manipulation -----------------

  @Override
  public Formula createPredicateVariable(Formula f) {
    Term t = getTerm(f);
    // TODO is something better than hashcode??
    String repr = (isAtom(t) ? convertQuotes(t.toStringDirect()) : ("#" + t.hashCode()));
    return encapsulate(buildVariable("\"PRED" + repr + "\"", env.sort(Type.BOOL)));
  }

  @Override
  public String dumpFormula(Formula f) {
    return getTerm(f).toStringDirect(); // creates prefix notation with brackets
  }

  @Override
  public Formula parseInfix(String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Formula parse(String s) {
    return encapsulate(parseStringToTerms(s)[0]);
  }

  /** Parse a String to Terms and Declarations.
   * The String may contain terms and function-declarations in SMTLIB2-format.
   * Use Prefix-notation! */
  private Term[] parseStringToTerms(String s) {
    Parser parser = new Parser(env, new StringReader(s));

    try {
      parser.parse();
    } catch (Exception e) {
      throw new IllegalArgumentException("Could not parse term:" + e.getMessage(), e);
    }

    Term[] terms = parser.getTerms();
    return terms;
  }

  @Override
  public boolean isPurelyConjunctive(Formula f) {
    Term t = getTerm(f);

    if (isAtom(t) || uifs.contains(t)) {
      // term is atom
      return true;

    } else if (isNot(t)) {
      t = getArg(t, 0);
      return (uifs.contains(t) || isAtom(t));

    } else if (isAnd(t)) {
      for (int i = 0; i < getArity(t); ++i) {
        if (!isPurelyConjunctive(encapsulate(getArg(t, i)))) {
          return false;
        }
      }
      return true;

    } else {
      return false;
    }
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
      final Term t = getTerm(tt);

      if (isVariable(t)) {
        toProcess.pop();
        String name = dequote(t.toString());
        int idx = ssa.getIndex(name);
        if (idx > 0) {
          // ok, the variable has an instance in the SSA, replace it
          Term newt = buildVariable(makeName(name, idx), t.getSort());
          cache.put(tt, encapsulate(newt));
        } else {
          // the variable is not used in the SSA, keep it as is
          cache.put(tt, tt);
        }

      } else {
        boolean childrenDone = true;
        Term[] newargs = new Term[getArity(t)];
        for (int i = 0; i < newargs.length; ++i) {
          Formula c = encapsulate(getArg(t, i));
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
          Term newt;
          if (uifs.contains(t)) {
            String name = ((ApplicationTerm)t).getFunction().toString();
            assert name != null;

            if (ufCanBeLvalue(name)) {
              int idx = ssa.getIndex(name, encapsulate(newargs));
              if (idx > 0) {
                // ok, the variable has an instance in the SSA, replace it
                newt = buildUF(makeName(name, idx), newargs, false);
              } else {
                newt = replaceArgs(env, t, newargs);
              }
            } else {
              newt = replaceArgs(env, t, newargs);
            }
          } else {
            newt = replaceArgs(env, t, newargs);
          }

          cache.put(tt, encapsulate(newt));
        }
      }
    }

    Formula result = cache.get(f);
    assert result != null;
    return result;
  }

  private boolean ufCanBeLvalue(String name) { // TODO what does this function??
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
      final Term t = getTerm(tt);
      if (isVariable(t)) {
        String name = parseName(t.toString()).getFirst();
        Term newt = buildVariable(name, t.getSort());
        cache.put(tt, encapsulate(newt));

      } else {
        boolean childrenDone = true;
        Term[] newargs = new Term[getArity(t)];
        for (int i = 0; i < newargs.length; ++i) {
          Formula c = encapsulate(getArg(t, i));
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
          Term newt;
          if (uifs.contains(t)) {
            String name = ((ApplicationTerm)t).getFunction().toString();
            assert name != null;

            if (ufCanBeLvalue(name)) {
              name = parseName(name).getFirst();

              newt = buildUF(name, newargs, false);
            } else {
              newt = replaceArgs(env, t, newargs);
            }
          } else {
            newt = replaceArgs(env, t, newargs);
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
    Set<Formula> cache = new HashSet<Formula>();
    List<Formula> atoms = new ArrayList<Formula>();

    Deque<Formula> toProcess = new ArrayDeque<Formula>();
    toProcess.push(f);

    while (!toProcess.isEmpty()) {
      Formula tt = toProcess.pop();
      Term t = getTerm(tt);

      if (cache.contains(tt)) {
        continue;
      }
      cache.add(tt);

      if (tt.isTrue() || tt.isFalse()) {
        continue;
      }

      if (isAtom(t)) {
        tt = uninstantiate(tt);
        t = getTerm(tt);

        if (splitArithEqualities && isEqual(t) && isPurelyArithmetic(tt)) {
          Term a1 = getArg(t, 0);
          Term a2 = getArg(t, 1);
          Formula tt1 = encapsulate(env.term("<=", a1, a2)); // TODO why "<="??
          cache.add(tt1);
          atoms.add(tt1);
        }
        atoms.add(tt);
      } else if (conjunctionsOnly && !isNot(t) && !isAnd(t)) {
        // conjunctions only, but formula is neither "not" nor "and"
        // treat this as atomic
        atoms.add(uninstantiate(tt));

      } else {
        // ok, go into this formula
        for (int i = 0; i < getArity(t); ++i) {
          Formula c = encapsulate(getArg(t, i));
          if (!cache.contains(c)) {
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
      Term t = getTerm(toProcess.pop());

      if (isTrue(t) || isFalse(t)) {
        continue;
      }

      if (isVariable(t)) {
        vars.add(t.toString());

      } else {
        // ok, go into this formula
        for (int i = 0; i < getArity(t); ++i){
          Formula c = encapsulate(getArg(t, i));

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
    if (result != null) {
      return result;
    } else {

      Term t = getTerm(f);
      boolean res = true;
      if (uifs.contains(t)) {
        res = false;

      } else {
        for (int i = 0; i < getArity(t); ++i) {
          res |= isPurelyArithmetic(encapsulate(getArg(t, i)));
          if (!res) {
            break;
          }
        }
      }
      arithCache.put(f, res);
      return res;
    }
  }

  @Override
  public boolean checkSyntacticEntails(Formula leftFormula, Formula rightFormula) {
    final Term leftTerm = getTerm(leftFormula);

    Deque<Term> toProcess = new ArrayDeque<Term>();
    Set<Term> seen = new HashSet<Term>();

    toProcess.push(getTerm(rightFormula));
    while (!toProcess.isEmpty()) {
      final Term rightSubTerm = toProcess.pop();

      if (rightSubTerm == leftTerm) { // TODO equal? compare Strings?
        return true;
      }

      if (!isVariable(rightSubTerm)) {
        int args = getArity(rightSubTerm);
        for (int i = 0; i < args; ++i) {
          Term arg = getArg(rightSubTerm, i);
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
  public String getVersion(){
    return env.getVersion();
  }
}
