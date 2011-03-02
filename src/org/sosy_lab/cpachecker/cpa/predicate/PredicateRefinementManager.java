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
package org.sosy_lab.cpachecker.cpa.predicate;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ForceStopCPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.Model.AssignableTerm;
import org.sosy_lab.cpachecker.util.predicates.Model.TermType;
import org.sosy_lab.cpachecker.util.predicates.Model.Variable;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingTheoremProver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;


@Options(prefix="cpa.predicate.refinement")
class PredicateRefinementManager<T1, T2> extends PredicateAbstractionManager {

  static class Stats {
    public final Timer cexAnalysisTimer = new Timer();
    public final Timer cexAnalysisSolverTimer = new Timer();
    public final Timer cexAnalysisGetUsefulBlocksTimer = new Timer();
  }
  
  final Stats refStats;

  private final InterpolatingTheoremProver<T1> firstItpProver;
  private final InterpolatingTheoremProver<T2> secondItpProver;

  private static final Pattern PREDICATE_NAME_PATTERN = Pattern.compile(
      "^.*" + CtoFormulaConverter.PROGRAM_COUNTER_PREDICATE + "(?=\\d+$)");

  @Option
  private boolean getUsefulBlocks = false;

  @Option(name="shortestCexTrace")
  private boolean shortestTrace = false;

  @Option
  private boolean atomicPredicates = true;

  @Option
  private boolean splitItpAtoms = false;

  @Option(name="shortestCexTraceUseSuffix")
  private boolean useSuffix = false;

  @Option(name="shortestCexTraceZigZag")
  private boolean useZigZag = false;

  @Option(name="addWellScopedPredicates")
  private boolean wellScopedPredicates = false;

  @Option
  private boolean dumpInterpolationProblems = false;

  @Option(name="timelimit")
  private long itpTimeLimit = 0;

  @Option(name="changesolverontimeout")
  private boolean changeItpSolveOTF = false;

  @Option
  private int maxRefinementSize = 0;
  
  private final ExecutorService executor;

  public PredicateRefinementManager(
      RegionManager pRmgr,
      FormulaManager pFmgr,
      PathFormulaManager pPmgr,
      TheoremProver pThmProver,
      InterpolatingTheoremProver<T1> pItpProver,
      InterpolatingTheoremProver<T2> pAltItpProver,
      Configuration config,
      LogManager pLogger) throws InvalidConfigurationException {
    super(pRmgr, pFmgr, pPmgr, pThmProver, config, pLogger);
    config.inject(this);

    refStats = new Stats();
    firstItpProver = pItpProver;
    secondItpProver = pAltItpProver;

    if (itpTimeLimit == 0) {
      executor = null;
    } else {
      // important to use daemon threads here, because we never have the chance to stop the executor
      executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).build());
    }
    
    if (wellScopedPredicates) {
      throw new InvalidConfigurationException("wellScopePredicates are currently disabled");
    }
//    if (inlineFunctions && wellScopedPredicates) {
//      logger.log(Level.WARNING, "Well scoped predicates not possible with function inlining, disabling them.");
//      wellScopedPredicates = false;
//    }
  }

  /**
   * Counterexample analysis and predicate discovery.
   * @param pAbstractTrace abstract trace of the error path
   * @param pItpProver interpolation solver used
   * @return counterexample info with predicated information
   * @throws CPAException
   */
  private <T> CounterexampleTraceInfo buildCounterexampleTraceWithSpecifiedItp(
      ArrayList<PredicateAbstractElement> pAbstractTrace, InterpolatingTheoremProver<T> pItpProver) throws CPAException {
    
    refStats.cexAnalysisTimer.start();

    logger.log(Level.FINEST, "Building counterexample trace");

    List<Formula> f = getFormulasForTrace(pAbstractTrace);

    if (useBitwiseAxioms) {
      Formula bitwiseAxioms = fmgr.makeTrue();
  
      for (Formula fm : f) {
        Formula a = fmgr.getBitwiseAxioms(fm);
        if (!a.isTrue()) {
          bitwiseAxioms = fmgr.makeAnd(bitwiseAxioms, a);  
        }
      }
  
      if (!bitwiseAxioms.isTrue()) {
        logger.log(Level.ALL, "DEBUG_3", "ADDING BITWISE AXIOMS TO THE",
            "LAST GROUP: ", bitwiseAxioms);
        int lastIndex = f.size()-1;
        f.set(lastIndex, fmgr.makeAnd(f.get(lastIndex), bitwiseAxioms));
      }
    }

    f = Collections.unmodifiableList(f);

    logger.log(Level.ALL, "Counterexample trace formulas:", f);

    if (maxRefinementSize > 0) {
      Formula cex = fmgr.makeTrue();
      for (Formula formula : f) {
        cex = fmgr.makeAnd(cex, formula);
      }
      int size = fmgr.dumpFormula(cex).length();
      if (size > maxRefinementSize) {
        logger.log(Level.FINEST, "Skipping refinement because input formula is", size, "bytes large.");
        throw new RefinementFailedException(Reason.TooMuchUnrolling, null);
      }
    }
    
    logger.log(Level.FINEST, "Checking feasibility of counterexample trace");

    // now f is the DAG formula which is satisfiable iff there is a
    // concrete counterexample

    // create a working environment
    pItpProver.init();

    refStats.cexAnalysisSolverTimer.start();

    if (shortestTrace && getUsefulBlocks) {
      f = Collections.unmodifiableList(getUsefulBlocks(f, useSuffix, useZigZag));
    }

    if (dumpInterpolationProblems) {
      int k = 0;
      for (Formula formula : f) {
        String dumpFile = String.format(formulaDumpFilePattern,
                    "interpolation", refStats.cexAnalysisTimer.getNumberOfIntervals(), "formula", k++);
        dumpFormulaToFile(formula, new File(dumpFile));
      }
    }

    List<T> itpGroupsIds = new ArrayList<T>(f.size());
    for (int i = 0; i < f.size(); i++) {
      itpGroupsIds.add(null);
    }

    boolean spurious;
    if (getUsefulBlocks || !shortestTrace) {
      // check all formulas in f at once

      for (int i = useSuffix ? f.size()-1 : 0;
      useSuffix ? i >= 0 : i < f.size(); i += useSuffix ? -1 : 1) {

        itpGroupsIds.set(i, pItpProver.addFormula(f.get(i)));
      }
      spurious = pItpProver.isUnsat();

    } else {
      spurious = checkInfeasabilityOfShortestTrace(f, itpGroupsIds, pItpProver);
    }
    assert itpGroupsIds.size() == f.size();
    assert !itpGroupsIds.contains(null); // has to be filled completely

    refStats.cexAnalysisSolverTimer.stop();

    logger.log(Level.FINEST, "Counterexample trace is", (spurious ? "infeasible" : "feasible"));

    CounterexampleTraceInfo info;

    if (spurious) {
      info = new CounterexampleTraceInfo();

      // the counterexample is spurious. Extract the predicates from
      // the interpolants

      // how to partition the trace into (A, B) depends on whether
      // there are function calls involved or not: in general, A
      // is the trace from the entry point of the current function
      // to the current point, and B is everything else. To implement
      // this, we keep track of which function we are currently in.
      // if we don't want "well-scoped" predicates, A always starts at the beginning
      Deque<Integer> entryPoints = null;
      if (wellScopedPredicates) {
        entryPoints = new ArrayDeque<Integer>();
        entryPoints.push(0);
      }
      boolean foundPredicates = false;

      for (int i = 0; i < f.size()-1; ++i) {
        // last iteration is left out because B would be empty
        final int start_of_a = (wellScopedPredicates ? entryPoints.peek() : 0);
        PredicateAbstractElement e = pAbstractTrace.get(i);

        logger.log(Level.ALL, "Looking for interpolant for formulas from",
            start_of_a, "to", i);

        refStats.cexAnalysisSolverTimer.start();
        Formula itp = pItpProver.getInterpolant(itpGroupsIds.subList(start_of_a, i+1));
        refStats.cexAnalysisSolverTimer.stop();
        
        if (dumpInterpolationProblems) {
          String dumpFile = String.format(formulaDumpFilePattern,
                  "interpolation", refStats.cexAnalysisTimer.getNumberOfIntervals(), "interpolant", i);
          dumpFormulaToFile(itp, new File(dumpFile));
        }

        if (itp.isTrue()) {
          logger.log(Level.ALL, "For step", i, "got no interpolant.");

        } else {
          foundPredicates = true;
          Collection<AbstractionPredicate> preds;
          
          if (itp.isFalse()) {
            preds = ImmutableSet.of(amgr.makeFalsePredicate());
          } else {
            preds = getAtomsAsPredicates(itp);
          }
          assert !preds.isEmpty();
          info.addPredicatesForRefinement(e, preds);

          logger.log(Level.ALL, "For step", i, "got:",
              "interpolant", itp,
              "predicates", preds);

          if (dumpInterpolationProblems) {
            String dumpFile = String.format(formulaDumpFilePattern,
                        "interpolation", refStats.cexAnalysisTimer.getNumberOfIntervals(), "atoms", i);
            Collection<Formula> atoms = Collections2.transform(preds,
                new Function<AbstractionPredicate, Formula>(){
                      @Override
                      public Formula apply(AbstractionPredicate pArg0) {
                        return pArg0.getSymbolicAtom();
                      }
                });
            printFormulasToFile(atoms, new File(dumpFile));
          }
        }

        // TODO wellScopedPredicates have been disabled
        
        // TODO the following code relies on the fact that there is always an abstraction on function call and return

        // If we are entering or exiting a function, update the stack
        // of entry points
        // TODO checking if the abstraction node is a new function
//        if (wellScopedPredicates && e.getAbstractionLocation() instanceof CFAFunctionDefinitionNode) {
//          entryPoints.push(i);
//        }
          // TODO check we are returning from a function
//        if (wellScopedPredicates && e.getAbstractionLocation().getEnteringSummaryEdge() != null) {
//          entryPoints.pop();
//        }
      }

      if (!foundPredicates) {
        throw new RefinementFailedException(RefinementFailedException.Reason.InterpolationFailed, null);
      }

    } else {
      // this is a real bug, notify the user

      // get the reachingPathsFormula and add it to the solver environment
      // this formula contains predicates for all branches we took
      // this way we can figure out which branches make a feasible path
      PredicateAbstractElement lastElement = pAbstractTrace.get(pAbstractTrace.size()-1);
      pItpProver.addFormula(lastElement.getPathFormula().getReachingPathsFormula());
      Model model;
      NavigableMap<Integer, Map<Integer, Boolean>> preds;

      // need to ask solver for satisfiability again,
      // otherwise model doesn't contain new predicates
      boolean stillSatisfiable = !pItpProver.isUnsat();
      if (!stillSatisfiable) {
        pItpProver.reset();
        pItpProver.init();
        logger.log(Level.WARNING, "Could not get precise error path information because of inconsistent reachingPathsFormula!");

        int k = 0;
        for (Formula formula : f) {
          pItpProver.addFormula(formula);
          String dumpFile =
              String.format(formulaDumpFilePattern, "interpolation",
                      refStats.cexAnalysisTimer.getNumberOfIntervals(), "formula", k++);
          dumpFormulaToFile(formula, new File(dumpFile));
        }
        String dumpFile =
            String.format(formulaDumpFilePattern, "interpolation",
                refStats.cexAnalysisTimer.getNumberOfIntervals(), "formula", k++);
        dumpFormulaToFile(lastElement.getPathFormula()
            .getReachingPathsFormula(), new File(dumpFile));
        pItpProver.isUnsat();
        model = pItpProver.getModel();
  preds = Maps.newTreeMap();
      } else {
        model = pItpProver.getModel();
        if (model.isEmpty()) {
          logger.log(Level.WARNING, "No satisfying assignment given by solver!");
          preds = Maps.newTreeMap();
        } else {
          preds = getPredicateValuesFromModel(model);
        }
      }

      info = new CounterexampleTraceInfo(f, pItpProver.getModel(), preds);
    }

    pItpProver.reset();

    // update stats
    refStats.cexAnalysisTimer.stop();

    logger.log(Level.ALL, "Counterexample information:", info);

    return info;

  }
  
  public void dumpCounterexampleToFile(CounterexampleTraceInfo cex, File file) {
    Formula f = fmgr.makeTrue();
    for (Formula part : cex.getCounterExampleFormulas()) {
      f = fmgr.makeAnd(f, part);
    }
    dumpFormulaToFile(f, file);
  }
  
  /**
   * Counterexample analysis and predicate discovery.
   * This method is just an helper to delegate the actual work
   * This is used to detect timeouts for interpolation
   * @throws CPAException
   */
  public CounterexampleTraceInfo buildCounterexampleTrace(
      ArrayList<PredicateAbstractElement> pAbstractTrace) throws CPAException {
    
    // if we don't want to limit the time given to the solver
    if (itpTimeLimit == 0) {
      return buildCounterexampleTraceWithSpecifiedItp(pAbstractTrace, firstItpProver);
    }
    
    assert executor != null;
    
    // how many times is the problem tried to be solved so far?
    int noOfTries = 0;
    
    while (true) {
      TransferCallable<?> tc;
      
      if (noOfTries == 0) {
        tc = new TransferCallable<T1>(pAbstractTrace, firstItpProver);
      } else {
        tc = new TransferCallable<T2>(pAbstractTrace, secondItpProver);
      }

      Future<CounterexampleTraceInfo> future = executor.submit(tc);

      try {
        // here we get the result of the post computation but there is a time limit
        // given to complete the task specified by timeLimit
        return future.get(itpTimeLimit, TimeUnit.MILLISECONDS);
        
      } catch (TimeoutException e){
        // if first try failed and changeItpSolveOTF enabled try the alternative solver
        if (changeItpSolveOTF && noOfTries == 0) {
          logger.log(Level.WARNING, "SMT-solver timed out during interpolation process, trying next solver.");
          noOfTries++;

        } else {
          logger.log(Level.SEVERE, "SMT-solver timed out during interpolation process");
          throw new RefinementFailedException(Reason.TIMEOUT, null);
        }
      } catch (InterruptedException e) {
        throw new ForceStopCPAException();
      
      } catch (ExecutionException e) {
        Throwable t = e.getCause();
        Throwables.propagateIfPossible(t, CPAException.class);
        
        logger.logException(Level.SEVERE, t, "Unexpected exception during interpolation!");
        throw new ForceStopCPAException();
      }
    }
  }

  private List<Formula> getFormulasForTrace(
      List<PredicateAbstractElement> abstractTrace) {

    // create the DAG formula corresponding to the abstract trace. We create
    // n formulas, one per interpolation group
    List<Formula> result = new ArrayList<Formula>(abstractTrace.size());

    for (PredicateAbstractElement e : abstractTrace) {
      result.add(e.getAbstractionFormula().getBlockFormula());
    }
    return result;
  }

  private <T> boolean checkInfeasabilityOfShortestTrace(List<Formula> traceFormulas,
        List<T> itpGroupsIds, InterpolatingTheoremProver<T> pItpProver) {
    Boolean tmpSpurious = null;

    if (useZigZag) {
      int e = traceFormulas.size()-1;
      int s = 0;
      boolean fromStart = false;
      while (s <= e) {
        int i = fromStart ? s : e;
        if (fromStart) s++;
        else e--;
        fromStart = !fromStart;

        tmpSpurious = null;
        Formula fm = traceFormulas.get(i);
        itpGroupsIds.set(i, pItpProver.addFormula(fm));
        if (!fm.isTrue()) {
          if (pItpProver.isUnsat()) {
            tmpSpurious = Boolean.TRUE;
            for (int j = s; j <= e; ++j) {
              itpGroupsIds.set(j, pItpProver.addFormula(traceFormulas.get(j)));
            }
            break;
          } else {
            tmpSpurious = Boolean.FALSE;
          }
        }
      }

    } else {
      for (int i = useSuffix ? traceFormulas.size()-1 : 0;
      useSuffix ? i >= 0 : i < traceFormulas.size(); i += useSuffix ? -1 : 1) {

        tmpSpurious = null;
        Formula fm = traceFormulas.get(i);
        itpGroupsIds.set(i, pItpProver.addFormula(fm));
        if (!fm.isTrue()) {
          if (pItpProver.isUnsat()) {
            tmpSpurious = Boolean.TRUE;
            // we need to add the other formulas to the itpProver
            // anyway, so it can setup its internal state properly
            for (int j = i+(useSuffix ? -1 : 1);
            useSuffix ? j >= 0 : j < traceFormulas.size();
            j += useSuffix ? -1 : 1) {
              itpGroupsIds.set(j, pItpProver.addFormula(traceFormulas.get(j)));
            }
            break;
          } else {
            tmpSpurious = Boolean.FALSE;
          }
        }
      }
    }

    return (tmpSpurious == null) ? pItpProver.isUnsat() : tmpSpurious;
  }

  private List<Formula> getUsefulBlocks(List<Formula> f,
      boolean suffixTrace, boolean zigZag) {
    
    refStats.cexAnalysisGetUsefulBlocksTimer.start();

    // try to find a minimal-unsatisfiable-core of the trace (as Blast does)

    thmProver.init();

    logger.log(Level.ALL, "DEBUG_1", "Calling getUsefulBlocks on path",
        "of length:", f.size());

    Formula trueFormula = fmgr.makeTrue();
    Formula[] needed = new Formula[f.size()];
    for (int i = 0; i < needed.length; ++i) {
      needed[i] = trueFormula;
    }
    int pos = suffixTrace ? f.size()-1 : 0;
    int incr = suffixTrace ? -1 : 1;
    int toPop = 0;

    while (true) {
      boolean consistent = true;
      // 1. assert all the needed constraints
      for (int i = 0; i < needed.length; ++i) {
        if (!needed[i].isTrue()) {
          thmProver.push(needed[i]);
          ++toPop;
        }
      }
      // 2. if needed is inconsistent, then return it
      if (thmProver.isUnsat(trueFormula)) {
        f = Arrays.asList(needed);
        break;
      }
      // 3. otherwise, assert one block at a time, until we get an
      // inconsistency
      if (zigZag) {
        int s = 0;
        int e = f.size()-1;
        boolean fromStart = false;
        while (true) {
          int i = fromStart ? s : e;
          if (fromStart) ++s;
          else --e;
          fromStart = !fromStart;

          Formula t = f.get(i);
          thmProver.push(t);
          ++toPop;
          if (thmProver.isUnsat(trueFormula)) {
            // add this block to the needed ones, and repeat
            needed[i] = t;
            logger.log(Level.ALL, "DEBUG_1",
                "Found needed block: ", i, ", term: ", t);
            // pop all
            while (toPop > 0) {
              --toPop;
              thmProver.pop();
            }
            // and go to the next iteration of the while loop
            consistent = false;
            break;
          }

          if (e < s) break;
        }
      } else {
        for (int i = pos; suffixTrace ? i >= 0 : i < f.size();
        i += incr) {
          Formula t = f.get(i);
          thmProver.push(t);
          ++toPop;
          if (thmProver.isUnsat(trueFormula)) {
            // add this block to the needed ones, and repeat
            needed[i] = t;
            logger.log(Level.ALL, "DEBUG_1",
                "Found needed block: ", i, ", term: ", t);
            // pop all
            while (toPop > 0) {
              --toPop;
              thmProver.pop();
            }
            // and go to the next iteration of the while loop
            consistent = false;
            break;
          }
        }
      }
      if (consistent) {
        // if we get here, the trace is consistent:
        // this is a real counterexample!
        break;
      }
    }

    while (toPop > 0) {
      --toPop;
      thmProver.pop();
    }

    thmProver.reset();

    logger.log(Level.ALL, "DEBUG_1", "Done getUsefulBlocks");

    refStats.cexAnalysisGetUsefulBlocksTimer.stop();

    return f;
  }

  /**
   * Create predicates for all atoms in a formula.
   */
  @SuppressWarnings("deprecation")
  public List<AbstractionPredicate> getAtomsAsPredicates(Formula f) {
    Collection<Formula> atoms;
    if (atomicPredicates) {
      atoms = fmgr.extractAtoms(f, splitItpAtoms, false);
    } else {
      atoms = Collections.singleton(fmgr.uninstantiate(f));
    }

    List<AbstractionPredicate> preds = new ArrayList<AbstractionPredicate>(atoms.size());

    for (Formula atom : atoms) {
      preds.add(amgr.makePredicate(atom));
    }
    return preds;    
  }

  private NavigableMap<Integer, Map<Integer, Boolean>> getPredicateValuesFromModel(Model model) {

    NavigableMap<Integer, Map<Integer, Boolean>> preds = Maps.newTreeMap();
    for (AssignableTerm a : model.keySet()) {
      if (a instanceof Variable && a.getType() == TermType.Boolean) {
        
        String name = PREDICATE_NAME_PATTERN.matcher(a.getName()).replaceFirst("");
        if (!name.equals(a.getName())) {
          // pattern matched, so it's a variable with __pc__ in it

          Variable v = (Variable)a;
          // no NumberFormatException because of RegExp match earlier
          Integer edgeId = Integer.parseInt(name);
          Integer idx = v.getSSAIndex();          
          
          Map<Integer, Boolean> p = preds.get(idx);
          if (p == null) {
            p = new HashMap<Integer, Boolean>(2);
            preds.put(idx, p);
          }
          
          Boolean value = (Boolean)model.get(a);
          p.put(edgeId, value);
        }             
      }
    }
    return preds;
  }

  private class TransferCallable<T> implements Callable<CounterexampleTraceInfo> {

    private final ArrayList<PredicateAbstractElement> abstractTrace;
    private final InterpolatingTheoremProver<T> currentItpProver;

    public TransferCallable(ArrayList<PredicateAbstractElement> pAbstractTrace,
        InterpolatingTheoremProver<T> pItpProver) {
      abstractTrace = pAbstractTrace;
      currentItpProver = pItpProver;
    }

    @Override
    public CounterexampleTraceInfo call() throws CPAException {
      return buildCounterexampleTraceWithSpecifiedItp(abstractTrace, currentItpProver);
    }
  }
}
