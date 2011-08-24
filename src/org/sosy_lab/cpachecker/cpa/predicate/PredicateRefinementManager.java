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
package org.sosy_lab.cpachecker.cpa.predicate;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.CSIsatInterpolatingProver;
import org.sosy_lab.cpachecker.util.predicates.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.Model.AssignableTerm;
import org.sosy_lab.cpachecker.util.predicates.Model.TermType;
import org.sosy_lab.cpachecker.util.predicates.Model.Variable;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingTheoremProver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatInterpolatingProver;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;


@Options(prefix="cpa.predicate.refinement")
public class PredicateRefinementManager extends PredicateAbstractionManager {

  static class Stats {
    public final Timer cexAnalysisTimer = new Timer();
    public final Timer cexAnalysisSolverTimer = new Timer();
    public final Timer cexAnalysisGetUsefulBlocksTimer = new Timer();
  }

  final Stats refStats;

  private final InterpolatingTheoremProver<?> firstItpProver;
  private final InterpolatingTheoremProver<?> secondItpProver;

  private static final String BRANCHING_PREDICATE_NAME = "__ART__";
  private static final Pattern BRANCHING_PREDICATE_NAME_PATTERN = Pattern.compile(
      "^.*" + BRANCHING_PREDICATE_NAME + "(?=\\d+$)");

  @Option(name="interpolatingProver", toUppercase=true, values={"MATHSAT", "CSISAT"},
      description="which interpolating solver to use for interpolant generation?")
  private String whichItpProver = "MATHSAT";

  @Option(description="apply deletion-filter to the abstract counterexample, to get "
    + "a minimal set of blocks, before applying interpolation-based refinement")
  private boolean getUsefulBlocks = false;

  @Option(name="shortestCexTrace",
      description="use incremental search in counterexample analysis, "
        + "to find the minimal infeasible prefix")
  private boolean shortestTrace = false;

  @Option(description="only use the atoms from the interpolants as predicates, "
    + "and not the whole interpolant")
  private boolean atomicPredicates = true;

  @Option(description="split arithmetic equalities when extracting predicates from interpolants")
  private boolean splitItpAtoms = false;

  @Option(name="shortestCexTraceUseSuffix",
      description="if shortestCexTrace is used, "
        + "start from the end with the incremental search")
  private boolean useSuffix = false;

  @Option(name="shortestCexTraceZigZag",
      description="if shortestCexTrace is used, "
        + "alternatingly search from start and end of the trace")
  private boolean useZigZag = false;

  @Option(name="addWellScopedPredicates",
      description="refinement will try to build 'well-scoped' predicates, "
        + "by cutting spurious traces as explained in Section 5.2 of the paper "
        + "'Abstractions From Proofs'\n(this does not work with function inlining).\n"
        + "THIS FEATURE IS CURRENTLY NOT AVAILABLE. ")
  private boolean wellScopedPredicates = false;

  @Option(description="dump all interpolation problems")
  private boolean dumpInterpolationProblems = false;

  @Option(name="timelimit",
      description="time limit for refinement (0 is infinitely long)")
  private long itpTimeLimit = 0;

  @Option(name="changesolverontimeout",
      description="try again with a second solver if refinement timed out")
  private boolean changeItpSolveOTF = false;

  @Option(description="skip refinement if input formula is larger than "
    + "this amount of bytes (ignored if 0)")
  private int maxRefinementSize = 0;

  private final ExecutorService executor;

  public PredicateRefinementManager(
      RegionManager pRmgr,
      FormulaManager pFmgr,
      PathFormulaManager pPmgr,
      TheoremProver pThmProver,
      Configuration config,
      LogManager pLogger) throws InvalidConfigurationException {
    super(pRmgr, pFmgr, pPmgr, pThmProver, config, pLogger);
    config.inject(this, PredicateRefinementManager.class);

    refStats = new Stats();

    // create solvers
    if (whichItpProver.equals("MATHSAT")) {
      if (!(pFmgr instanceof MathsatFormulaManager)) {
        throw new InvalidConfigurationException("Need to use Mathsat as solver if Mathsat should be used for interpolation");
      }
      firstItpProver = new MathsatInterpolatingProver((MathsatFormulaManager) pFmgr, false);

    } else if (whichItpProver.equals("CSISAT")) {
      firstItpProver = new CSIsatInterpolatingProver(pFmgr, logger);

    } else {
      throw new InternalError("Update list of allowed solvers!");
    }

    if (changeItpSolveOTF) {
      if (whichItpProver.equals("MATHSAT")) {
        secondItpProver = new CSIsatInterpolatingProver(pFmgr, logger);
      } else {
        if (!(pFmgr instanceof MathsatFormulaManager)) {
          throw new InvalidConfigurationException("Need to use Mathsat as solver if Mathsat should be used for interpolation");
        }
        secondItpProver = new MathsatInterpolatingProver((MathsatFormulaManager) pFmgr, false);
      }
    } else {
      secondItpProver = null;
    }

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

  public void dumpCounterexampleToFile(CounterexampleTraceInfo cex, File file) {
    dumpFormulaToFile(makeConjunction(cex.getCounterExampleFormulas()), file);
  }

  /**
   * Counterexample analysis and predicate discovery.
   * This method is just an helper to delegate the actual work
   * This is used to detect timeouts for interpolation
   *
   * @param pFormulas the formulas for the path
   * @param elementsOnPath the ARTElements on the path (may be empty if no branching information is required)
   * @throws CPAException
   * @throws InterruptedException
   */
  public CounterexampleTraceInfo buildCounterexampleTrace(
      final List<Formula> pFormulas,
      final Set<ARTElement> elementsOnPath) throws CPAException, InterruptedException {

    // if we don't want to limit the time given to the solver
    if (itpTimeLimit == 0) {
      return buildCounterexampleTraceWithSpecifiedItp(pFormulas, elementsOnPath, firstItpProver);
    }

    assert executor != null;

    // how many times is the problem tried to be solved so far?
    int noOfTries = 0;

    while (true) {
      final InterpolatingTheoremProver<?> currentItpProver =
        (noOfTries == 0) ? firstItpProver : secondItpProver;

      Callable<CounterexampleTraceInfo> tc = new Callable<CounterexampleTraceInfo>() {
        @Override
        public CounterexampleTraceInfo call() throws CPAException, InterruptedException {
          return buildCounterexampleTraceWithSpecifiedItp(pFormulas, elementsOnPath, currentItpProver);
        }
      };

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

      } catch (ExecutionException e) {
        Throwable t = e.getCause();
        Throwables.propagateIfPossible(t, CPAException.class, InterruptedException.class);

        throw new UnexpectedCheckedException("interpolation", t);
      }
    }
  }

  /**
   * Counterexample analysis and predicate discovery.
   * @param pFormulas the formulas for the path
   * @param elementsOnPath the ARTElements on the path (may be empty if no branching information is required)
   * @param pItpProver interpolation solver used
   * @return counterexample info with predicated information
   * @throws CPAException
   */
  private <T> CounterexampleTraceInfo buildCounterexampleTraceWithSpecifiedItp(
      List<Formula> pFormulas, Set<ARTElement> elementsOnPath, InterpolatingTheoremProver<T> pItpProver) throws CPAException, InterruptedException {

    logger.log(Level.FINEST, "Building counterexample trace");
    refStats.cexAnalysisTimer.start();
    pItpProver.init();
    try {

      // Final adjustments to the list of formulas
      List<Formula> f = new ArrayList<Formula>(pFormulas); // copy because we will change the list

      if (useBitwiseAxioms) {
        addBitwiseAxioms(f);
      }

      f = Collections.unmodifiableList(f);
      logger.log(Level.ALL, "Counterexample trace formulas:", f);

      // now f is the DAG formula which is satisfiable iff there is a
      // concrete counterexample


      // Check if refinement problem is not too big
      if (maxRefinementSize > 0) {
        int size = fmgr.dumpFormula(makeConjunction(f)).length();
        if (size > maxRefinementSize) {
          logger.log(Level.FINEST, "Skipping refinement because input formula is", size, "bytes large.");
          throw new RefinementFailedException(Reason.TooMuchUnrolling, null);
        }
      }


      // Check feasibility of counterexample
      logger.log(Level.FINEST, "Checking feasibility of counterexample trace");
      refStats.cexAnalysisSolverTimer.start();

      boolean spurious;
      List<T> itpGroupsIds;
      try {

        if (shortestTrace && getUsefulBlocks) {
          f = Collections.unmodifiableList(getUsefulBlocks(f, useSuffix, useZigZag));
        }

        if (dumpInterpolationProblems) {
          dumpInterpolationProblem(f);
        }

        // initialize all interpolation group ids with "null"
        itpGroupsIds = new ArrayList<T>(Collections.<T>nCopies(f.size(), null));

        if (getUsefulBlocks || !shortestTrace) {
          spurious = checkInfeasibilityOfFullTrace(f, itpGroupsIds, pItpProver);

        } else {
          spurious = checkInfeasabilityOfShortestTrace(f, itpGroupsIds, pItpProver);
        }
        assert itpGroupsIds.size() == f.size();
        assert !itpGroupsIds.contains(null); // has to be filled completely

      } finally {
        refStats.cexAnalysisSolverTimer.stop();
      }

      logger.log(Level.FINEST, "Counterexample trace is", (spurious ? "infeasible" : "feasible"));


      // Get either interpolants or error path information
      CounterexampleTraceInfo info;
      if (spurious) {

        info = getInterpolants(pItpProver, itpGroupsIds);

      } else {
        // this is a real bug
        info = getErrorPath(f, pItpProver, elementsOnPath);
      }

      logger.log(Level.ALL, "Counterexample information:", info);

      return info;

    } finally {
      pItpProver.reset();
      refStats.cexAnalysisTimer.stop();
    }
  }

  /**
   * Add axioms about bitwise operations to a list of formulas, if such operations
   * are used. This is probably not that helpful currently, we would have to the
   * tell the solver that these are axioms.
   *
   * The axioms are added to the last part of the list of formulas.
   *
   * @param f The list of formulas to scan for bitwise operations.
   */
  private void addBitwiseAxioms(List<Formula> f) {
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

  /**
   * Try to find out which formulas out of a list of formulas are relevant for
   * making the conjunction unsatisfiable.
   *
   * @param f The list of formulas to check.
   * @param suffixTrace Whether to start from the end of the list.
   * @param zigZag Whether to use a zig-zag way, using formulas from the beginning and the end.
   * @return A sublist of f that contains the useful formulas.
   */
  private List<Formula> getUsefulBlocks(List<Formula> f, boolean suffixTrace, boolean zigZag) {

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
   * Check the satisfiability of all formulas in a list.
   *
   * @param f The list of formulas to check.
   * @param itpGroupsIds The list where to store the references to the interpolation groups.
   * @param pItpProver The solver to use.
   * @return True if the formulas are unsatisfiable.
   * @throws InterruptedException
   */
  private <T> boolean checkInfeasibilityOfFullTrace(List<Formula> f,
      List<T> itpGroupsIds, InterpolatingTheoremProver<T> pItpProver)
      throws InterruptedException {
    // check all formulas in f at once

    for (int i = useSuffix ? f.size()-1 : 0;
    useSuffix ? i >= 0 : i < f.size(); i += useSuffix ? -1 : 1) {

      itpGroupsIds.set(i, pItpProver.addFormula(f.get(i)));
    }

    return pItpProver.isUnsat();
  }

  /**
   * Check the satisfiability of a list of formulas, while trying to use as few
   * formulas as possible to make it unsatisfiable.
   *
   * @param traceFormulas The list of formulas to check.
   * @param itpGroupsIds The list where to store the references to the interpolation groups.
   * @param pItpProver The solver to use.
   * @return True if the formulas are unsatisfiable.
   * @throws InterruptedException
   */
  private <T> boolean checkInfeasabilityOfShortestTrace(List<Formula> traceFormulas,
      List<T> itpGroupsIds, InterpolatingTheoremProver<T> pItpProver) throws InterruptedException {
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

  /**
   * Get the interpolants from the solver after the formulas have been proved
   * to be unsatisfiable.
   *
   * @param pItpProver The solver.
   * @param itpGroupsIds The references to the interpolation groups
   * @return Information about the counterexample, including the interpolants.
   * @throws RefinementFailedException If there were no interpolants.
   */
  private <T> CounterexampleTraceInfo getInterpolants(
      InterpolatingTheoremProver<T> pItpProver, List<T> itpGroupsIds)
      throws RefinementFailedException {

    CounterexampleTraceInfo info = new CounterexampleTraceInfo();

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

    for (int i = 0; i < itpGroupsIds.size()-1; ++i) {
      // last iteration is left out because B would be empty
      final int start_of_a = (wellScopedPredicates ? entryPoints.peek() : 0);

      logger.log(Level.ALL, "Looking for interpolant for formulas from",
          start_of_a, "to", i);

      refStats.cexAnalysisSolverTimer.start();
      Formula itp = pItpProver.getInterpolant(itpGroupsIds.subList(start_of_a, i+1));
      refStats.cexAnalysisSolverTimer.stop();

      if (dumpInterpolationProblems) {
        File dumpFile = formatFormulaOutputFile("interpolation", refStats.cexAnalysisTimer.getNumberOfIntervals(), "interpolant", i);
        dumpFormulaToFile(itp, dumpFile);
      }

      Collection<AbstractionPredicate> preds;

      if (itp.isTrue()) {
        logger.log(Level.ALL, "For step", i, "got no interpolant.");
        preds = Collections.emptySet();

      } else {
        foundPredicates = true;

        preds = getPredicatesFromInterpolant(itp, i);
      }
      info.addPredicatesForRefinement(preds);

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
    return info;
  }

  /**
   * Get the predicates out of an interpolant.
   * @param interpolant The interpolant formula.
   * @param index The index in the list of formulas (just for debugging)
   * @return A set of predicates.
   */
  private Collection<AbstractionPredicate> getPredicatesFromInterpolant(Formula interpolant, int index) {

    Collection<AbstractionPredicate> preds;
    if (interpolant.isFalse()) {
      preds = ImmutableSet.of(makeFalsePredicate());
    } else {
      preds = getAtomsAsPredicates(interpolant);
    }
    assert !preds.isEmpty();

    logger.log(Level.ALL, "For step", index, "got:",
        "interpolant", interpolant,
        "predicates", preds);

    if (dumpInterpolationProblems) {
      File dumpFile = formatFormulaOutputFile("interpolation", refStats.cexAnalysisTimer.getNumberOfIntervals(), "atoms", index);
      Collection<Formula> atoms = Collections2.transform(preds,
          new Function<AbstractionPredicate, Formula>(){
                @Override
                public Formula apply(AbstractionPredicate pArg0) {
                  return pArg0.getSymbolicAtom();
                }
          });
      printFormulasToFile(atoms, dumpFile);
    }
    return preds;
  }

  /**
   * Create predicates for all atoms in a formula.
   */
  @SuppressWarnings("deprecation")
  private List<AbstractionPredicate> getAtomsAsPredicates(Formula f) {
    Collection<Formula> atoms;
    if (atomicPredicates) {
      atoms = fmgr.extractAtoms(f, splitItpAtoms, false);
    } else {
      atoms = Collections.singleton(fmgr.uninstantiate(f));
    }

    List<AbstractionPredicate> preds = new ArrayList<AbstractionPredicate>(atoms.size());

    for (Formula atom : atoms) {
      preds.add(makePredicate(atom));
    }
    return preds;
  }

  /**
   * Get information about the error path from the solver after the formulas
   * have been proved to be satisfiable.
   *
   * @param f The list of formulas on the path.
   * @param pItpProver The solver.
   * @param elementsOnPath The ARTElements of the paths represented by f.
   * @return Information about the error path, including a satisfying assignment.
   * @throws CPATransferException
   * @throws InterruptedException
   */
  private <T> CounterexampleTraceInfo getErrorPath(List<Formula> f,
      InterpolatingTheoremProver<T> pItpProver, Set<ARTElement> elementsOnPath)
      throws CPATransferException, InterruptedException {

    // get the branchingFormula
    // this formula contains predicates for all branches we took
    // this way we can figure out which branches make a feasible path
    Formula branchingFormula = buildBranchingFormula(elementsOnPath);

    if (branchingFormula.isTrue()) {
      return new CounterexampleTraceInfo(f, pItpProver.getModel(), Collections.<Integer, Boolean>emptyMap());
    }

    // add formula to solver environment
    pItpProver.addFormula(branchingFormula);

    // need to ask solver for satisfiability again,
    // otherwise model doesn't contain new predicates
    boolean stillSatisfiable = !pItpProver.isUnsat();

    if (stillSatisfiable) {
      Model model = pItpProver.getModel();
      return new CounterexampleTraceInfo(f, model, getBranchingPredicateValuesFromModel(model));

    } else {
      // this should not happen
      logger.log(Level.WARNING, "Could not get precise error path information because of inconsistent reachingPathsFormula!");

      dumpInterpolationProblem(f);
      File dumpFile = formatFormulaOutputFile("interpolation", refStats.cexAnalysisTimer.getNumberOfIntervals(), "formula", f.size());
      dumpFormulaToFile(branchingFormula, dumpFile);

      return new CounterexampleTraceInfo(f, new Model(fmgr), Collections.<Integer, Boolean>emptyMap());
    }
  }

  /**
   * Build a formula containing a predicate for all branching situations in the
   * ART. If a satisfying assignment is created for this formula, it can be used
   * to find out which paths in the ART are feasible.
   *
   * This method may be called with an empty set, in which case it does nothing
   * and returns the formula "true".
   *
   * @param elementsOnPath The ART elements that should be considered.
   * @return A formula containing a predicate for each branching.
   * @throws CPATransferException
   */
  protected Formula buildBranchingFormula(Set<ARTElement> elementsOnPath) throws CPATransferException {
    // build the branching formula that will help us find the real error path
    Formula branchingFormula = fmgr.makeTrue();
    for (final ARTElement pathElement : elementsOnPath) {

      if (pathElement.getChildren().size() > 1) {
        if (pathElement.getChildren().size() > 2) {
          // can't create branching formula
          logger.log(Level.WARNING, "ART branching with more than two outgoing edges");
          return fmgr.makeTrue();
        }

        Iterable<CFAEdge> outgoingEdges = Iterables.transform(pathElement.getChildren(),
            new Function<ARTElement, CFAEdge>() {
              @Override
              public CFAEdge apply(ARTElement child) {
                return pathElement.getEdgeToChild(child);
              }
        });
        if (!Iterables.all(outgoingEdges, Predicates.instanceOf(AssumeEdge.class))) {
          logger.log(Level.WARNING, "ART branching without AssumeEdge");
          return fmgr.makeTrue();
        }

        AssumeEdge edge = null;
        for (CFAEdge currentEdge : outgoingEdges) {
          if (((AssumeEdge)currentEdge).getTruthAssumption()) {
            edge = (AssumeEdge)currentEdge;
            break;
          }
        }
        assert edge != null;

        Formula pred = fmgr.makePredicateVariable(BRANCHING_PREDICATE_NAME + pathElement.getElementId(), 0);

        // create formula by edge, be sure to use the correct SSA indices!
        PredicateAbstractElement pe = AbstractElements.extractElementByType(pathElement, PredicateAbstractElement.class);
        PathFormula pf = pe.getPathFormula();
        pf = pmgr.makeEmptyPathFormula(pf); // reset everything except SSAMap
        pf = pmgr.makeAnd(pf, edge);        // conjunct with edge

        Formula equiv = fmgr.makeEquivalence(pred, pf.getFormula());
        branchingFormula = fmgr.makeAnd(branchingFormula, equiv);
      }
    }
    return branchingFormula;
  }

  /**
   * Extract the information about the branching predicates created by
   * {@link #buildBranchingFormula(Set)} from a satisfying assignment.
   *
   * A map is created that stores for each ARTElement (using its element id as
   * the map key) which edge was taken (the positive or the negated one).
   *
   * @param model A satisfying assignment that should contain values for branching predicates.
   * @return A map from ART element id to a boolean value indicating direction.
   */
  private Map<Integer, Boolean> getBranchingPredicateValuesFromModel(Model model) {
    if (model.isEmpty()) {
      logger.log(Level.WARNING, "No satisfying assignment given by solver!");
      return Collections.emptyMap();
    }

    Map<Integer, Boolean> preds = Maps.newHashMap();
    for (AssignableTerm a : model.keySet()) {
      if (a instanceof Variable && a.getType() == TermType.Boolean) {

        String name = BRANCHING_PREDICATE_NAME_PATTERN.matcher(a.getName()).replaceFirst("");
        if (!name.equals(a.getName())) {
          // pattern matched, so it's a variable with __ART__ in it

          // no NumberFormatException because of RegExp match earlier
          Integer nodeId = Integer.parseInt(name);

          assert !preds.containsKey(nodeId);


          Boolean value = (Boolean)model.get(a);
          preds.put(nodeId, value);
        }
      }
    }
    return preds;
  }


  /**
   * Helper method to create the conjunction of all formulas in a list.
   */
  private Formula makeConjunction(List<Formula> f) {
    Formula result = fmgr.makeTrue();
    for (Formula formula : f) {
      result = fmgr.makeAnd(result, formula);
    }
    return result;
  }

  /**
   * Helper method to dump a list of formulas to files.
   */
  private void dumpInterpolationProblem(List<Formula> f) {
    int k = 0;
    for (Formula formula : f) {
      File dumpFile = formatFormulaOutputFile("interpolation", refStats.cexAnalysisTimer.getNumberOfIntervals(), "formula", k++);
      dumpFormulaToFile(formula, dumpFile);
    }
  }
}