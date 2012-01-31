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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaList;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.FunctionSymbol;
import de.uni_freiburg.informatik.ultimate.logic.LoggingScript;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.smtinterpol.smtlib2.Benchmark;

@Options(prefix="cpa.predicate.smtinterpol")
public abstract class SmtInterpolFormulaManager implements FormulaManager {

  // the environment in which all terms are created
  final Script script;
  String sort; // sort is the type (i.e. INT, REAL), depends on logic

  // the character for separating name and index of a value
  private final String INDEX_SEPARATOR = "@";

  // various caches for speeding up expensive tasks
  //
  // cache for splitting arithmetic equalities in extractAtoms
  private final Map<Formula, Boolean> arithCache = new HashMap<Formula, Boolean>();

  // cache for uninstantiating terms (see uninstantiate() below)
  private final Map<Formula, Formula> uninstantiateCache = new HashMap<Formula, Formula>();


  SmtInterpolFormulaManager(Configuration config, LogManager logger, String sort)
      throws InvalidConfigurationException {
    config.inject(this, SmtInterpolFormulaManager.class);
    script = createEnvironment();
  }

  Script createEnvironment() {
    if (script != null) {
      return script; // TODO working?? correct??
    }

    Logger logger = Logger.getRootLogger(); // TODO use SosyLAb-Logger
    SimpleLayout layout = new SimpleLayout();
    try {
      FileAppender fileAppender = new FileAppender(layout, "output/smtinterpol.log", false);
      logger.addAppender(fileAppender);
    } catch (IOException e) {
      e.printStackTrace();
    }
    // levels: ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF:
    logger.setLevel( Level.WARN );
    Script env = null;
    try {
      // create a thin wrapper around Benchmark,
      // this allows to write most formulas of the solver to outputfile
      env = new LoggingScript(new Benchmark(logger), "interpol.smt2", true);
    } catch (FileNotFoundException e1) {
      e1.printStackTrace();
    }

    try {
      env.setOption(":produce-proofs", true);
      env.setOption(":produce-models", true);
      env.setOption(":produce-assignments", true);
      env.setOption(":interactive-mode", true);
    //  BigInteger verbosity = (BigInteger) env.getOption(":verbosity");
    //  env.setOption(":verbosity", verbosity.subtract(BigInteger.ONE));
    } catch (SMTLIBException e) {
      e.printStackTrace();
    }

    return env;
  }

  // ----------------- Helper function -----------------

  private String makeName(String name, int idx) {
    return name + INDEX_SEPARATOR + idx;
  }

  Pair<String, Integer> parseName(String var) {
    String[] s = var.split(INDEX_SEPARATOR);
    if (s.length != 2) { throw new IllegalArgumentException(
        "Not an instantiated variable: " + var); }

    return Pair.of(s[0], Integer.parseInt(s[1]));
  }

  Term getTerm(Formula f) {
    return ((SmtInterpolFormula) f).getTerm();
  }

  Term[] getTerm(FormulaList f) {
    return ((SmtInterpolFormulaList) f).getTerms();
  }

  Formula encapsulate(Term t) {
    return new SmtInterpolFormula(t, script);
  }

  FormulaList encapsulate(Term[] t) {
    return new SmtInterpolFormulaList(t);
  }

  // ----------------- Boolean formulas -----------------

  @Override
  public boolean isBoolean(Formula f) {
    // TODO "equals" checks same object, working??
    return getTerm(f).getSort().equals(script.getTheory().getBooleanSort());
  }

  @Override
  public Formula makeTrue() {
    try {
      return encapsulate(script.term("true"));
    } catch (SMTLIBException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Formula makeFalse() {
    try {
      return encapsulate(script.term("false"));
    } catch (SMTLIBException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Formula makeNot(Formula f) {
    try {
      return encapsulate(script.term("not", getTerm(f)));
    } catch (SMTLIBException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Formula makeAnd(Formula f1, Formula f2) {
    try {
      return encapsulate(script.term("and", getTerm(f1), getTerm(f2)));
    } catch (SMTLIBException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Formula makeOr(Formula f1, Formula f2) {
    try {
      return encapsulate(script.term("or", getTerm(f1), getTerm(f2)));
    } catch (SMTLIBException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Formula makeEquivalence(Formula f1, Formula f2) {
    try {
      return encapsulate(script.term("=", getTerm(f1), getTerm(f2)));
    } catch (SMTLIBException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Formula makeIfThenElse(Formula condition, Formula f1, Formula f2) {
    try {
      return encapsulate(script.term("ite",
          getTerm(condition), getTerm(f1), getTerm(f2)));
    } catch (SMTLIBException e) {
      e.printStackTrace();
      return null;
    }
  }

  // ----------------- Uninterpreted functions -----------------

  protected Term buildUF(String name, Term... args) {
    Sort[] sorts = new Sort[args.length];
    for (int i = 0; i < args.length; i++) {
      sorts[i] = args[i].getSort();
    }

    FunctionSymbol func = script.getTheory().getFunction(name, sorts);
    if (func == null) {
      try {
        script.declareFun(name, sorts, script.sort(sort));
      } catch (SMTLIBException e) {
        e.printStackTrace();
      }
    }
    try {
      return script.term(name);
    } catch (SMTLIBException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Formula makeUIF(String name, FormulaList args) {
    return encapsulate(buildUF(name, getTerm(args)));
  }

  @Override
  public Formula makeUIF(String name, FormulaList args, int idx) {
    return encapsulate(buildUF(makeName(name, idx), getTerm(args)));
  }

  // ----------------- Other formulas -----------------

  @Override
  public Formula makeString(int i) {
    return encapsulate(script.getTheory().numeral(BigInteger.valueOf(i)));
    // TODO String?? decimal??
  }

  private Term buildVariable(String var, Sort sort) {
     var = var.replace("::", "_XX_"); //TODO this is only for printing

     FunctionSymbol func = script.getTheory().getFunction(var);
     if (func == null) {
       try {
         script.declareFun(var, new Sort[]{}, sort);
       } catch (SMTLIBException e) {
         e.printStackTrace();
       }
     }
     try {
       return script.term(var);
     } catch (SMTLIBException e) {
       e.printStackTrace();
       return null;
     }
  }

  @Override
  public Formula makeVariable(String var, int idx) {
    // System.out.println("makeVar: " + var + " " + idx);
    return makeVariable(makeName(var, idx));
  }

  @Override
  public Formula makeVariable(String var) {
    return encapsulate(buildVariable(var, script.getTheory().getSort(sort)));
  }

  @Override
  public Formula makePredicateVariable(String var, int idx) {
    // System.out.println("DEBUG: termvariable '" + var + " " + idx + "'- perhaps not working?");
    return makeVariable(makeName("PRED_" + var, idx));
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
    String repr = (isAtom(script, t) ? t.toString() : ("#" + t.hashCode()));
    return encapsulate(buildVariable("\"PRED" + repr + "\"",
          script.getTheory().getBooleanSort()));
  }

  @Override
  public String dumpFormula(Formula f) {
    return getTerm(f).toString(); // creates prefix notation with brackets
  }

  @Override
  public Formula parseInfix(String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Formula parse(String s) {
    throw new UnsupportedOperationException();
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
        String name = t.toString();
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
          if (isUIF(script, t)) {
            String name = ((ApplicationTerm)t).getFunction().toString();
            assert name != null;

            if (ufCanBeLvalue(name)) {
              int idx = ssa.getIndex(name, encapsulate(newargs));
              if (idx > 0) {
                // ok, the variable has an instance in the SSA, replace it
                newt = buildUF(makeName(name, idx), newargs);
              } else {
                newt = replaceArgs(script, t, newargs);
              }
            } else {
              newt = replaceArgs(script, t, newargs);
            }
          } else {
            newt = replaceArgs(script, t, newargs);
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
    // System.out.println("formula: " + f);

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
          if (isUIF(script, t)) {
            String name = ((ApplicationTerm)t).getFunction().toString();
            assert name != null;

            if (ufCanBeLvalue(name)) {
              name = parseName(name).getFirst();

              newt = buildUF(name, newargs);
            } else {
              newt = replaceArgs(script, t, newargs);
            }
          } else {
            newt = replaceArgs(script, t, newargs);
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
      assert !cache.contains(tt);
      cache.add(tt);

      if (tt.isTrue() || tt.isFalse()) {
        continue;
      }

      if (isAtom(script, t)) {
        tt = uninstantiate(tt);
        t = getTerm(tt);

        if (splitArithEqualities && isEqual(script, t) && isPurelyArithmetic(tt)) {
          Term a1 = getArg(t, 0);
          Term a2 = getArg(t, 1);

          Term t1 = null;
          try {
            t1 = script.term("<=", a1, a2); // TODO why "<="??
          } catch (SMTLIBException e) {
            e.printStackTrace();
          }

          Formula tt1 = encapsulate(t1);
          cache.add(tt1);

          atoms.add(tt1);
          atoms.add(tt);
        } else {
          atoms.add(tt);
        }
      } else if (conjunctionsOnly && !isNot(script, t)
                  && !isAnd(script, t)) {
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

      if (isTrue(script, t) || isFalse(script, t)) {
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
      if (isUIF(script, t)) {
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
}
