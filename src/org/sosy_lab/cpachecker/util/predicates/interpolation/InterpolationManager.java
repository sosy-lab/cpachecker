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
package org.sosy_lab.cpachecker.util.predicates.interpolation;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.CSIsatInterpolatingProver;
import org.sosy_lab.cpachecker.util.predicates.ExtendedFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingTheoremProver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatInterpolatingProver;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;


@Options(prefix="cpa.predicate.refinement")
public abstract class InterpolationManager<I> {

  static class Stats {
    private final Timer cexAnalysisTimer = new Timer();
    private final Timer cexAnalysisSolverTimer = new Timer();
    private final Timer cexAnalysisGetUsefulBlocksTimer = new Timer();
    private final Timer interpolantVerificationTimer = new Timer();

    void printStatistics(PrintStream out, Result result, ReachedSet reached) {
      out.println("  Counterexample analysis:        " + cexAnalysisTimer + " (Max: " + cexAnalysisTimer.printMaxTime() + ", Calls: " + cexAnalysisTimer.getNumberOfIntervals() + ")");
      if (cexAnalysisGetUsefulBlocksTimer.getMaxTime() != 0) {
        out.println("    Cex.focusing:                 " + cexAnalysisGetUsefulBlocksTimer + " (Max: " + cexAnalysisGetUsefulBlocksTimer.printMaxTime() + ")");
      }
      out.println("    Solving time only:            " + cexAnalysisSolverTimer + " (Max: " + cexAnalysisSolverTimer.printMaxTime() + ", Calls: " + cexAnalysisSolverTimer.getNumberOfIntervals() + ")");
      if (interpolantVerificationTimer.getNumberOfIntervals() > 0) {
        out.println("    Interpolant verification:     " + interpolantVerificationTimer);
      }
    }
  }

  private final Stats stats = new Stats();

  protected final LogManager logger;
  protected final ExtendedFormulaManager fmgr;
  protected final PathFormulaManager pmgr;
  private final TheoremProver thmProver;

  private final InterpolatingTheoremProver<?> firstItpProver;
  private final InterpolatingTheoremProver<?> secondItpProver;

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

  @Option(description="verify if the interpolants fulfill the interpolant properties")
  private boolean verifyInterpolants = false;

  @Option(name="timelimit",
      description="time limit for refinement (use milliseconds or specify a unit; 0 for infinite)")
  @TimeSpanOption(codeUnit=TimeUnit.MILLISECONDS,
      defaultUserUnit=TimeUnit.MILLISECONDS,
      min=0)
  private long itpTimeLimit = 0;

  @Option(name="changesolverontimeout",
      description="try again with a second solver if refinement timed out")
  private boolean changeItpSolveOTF = false;

  @Option(description="skip refinement if input formula is larger than "
    + "this amount of bytes (ignored if 0)")
  private int maxRefinementSize = 0;

  private final ExecutorService executor;

  public InterpolationManager(
      ExtendedFormulaManager pFmgr,
      PathFormulaManager pPmgr,
      TheoremProver pThmProver,
      Configuration config,
      LogManager pLogger) throws InvalidConfigurationException {
    config.inject(this, InterpolationManager.class);

    logger = pLogger;
    fmgr = pFmgr;
    pmgr = pPmgr;
    thmProver = pThmProver;

    // create solvers
    FormulaManager realFormulaManager = fmgr.getDelegate();
    if (whichItpProver.equals("MATHSAT")) {
      if (!(realFormulaManager instanceof MathsatFormulaManager)) {
        throw new InvalidConfigurationException("Need to use Mathsat as solver if Mathsat should be used for interpolation");
      }
      firstItpProver = new MathsatInterpolatingProver((MathsatFormulaManager) realFormulaManager, false);

    } else if (whichItpProver.equals("CSISAT")) {
      firstItpProver = new CSIsatInterpolatingProver(pFmgr, logger);

    } else {
      throw new InternalError("Update list of allowed solvers!");
    }

    if (changeItpSolveOTF) {
      if (whichItpProver.equals("MATHSAT")) {
        secondItpProver = new CSIsatInterpolatingProver(pFmgr, logger);
      } else {
        if (!(realFormulaManager instanceof MathsatFormulaManager)) {
          throw new InvalidConfigurationException("Need to use Mathsat as solver if Mathsat should be used for interpolation");
        }
        secondItpProver = new MathsatInterpolatingProver((MathsatFormulaManager) realFormulaManager, false);
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

  public String dumpCounterexample(CounterexampleTraceInfo<I> cex) {
    return fmgr.dumpFormula(fmgr.makeConjunction(cex.getCounterExampleFormulas()));
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
  public CounterexampleTraceInfo<I> buildCounterexampleTrace(
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

      Callable<CounterexampleTraceInfo<I>> tc = new Callable<CounterexampleTraceInfo<I>>() {
        @Override
        public CounterexampleTraceInfo<I> call() throws CPAException, InterruptedException {
          return buildCounterexampleTraceWithSpecifiedItp(pFormulas, elementsOnPath, currentItpProver);
        }
      };

      Future<CounterexampleTraceInfo<I>> future = executor.submit(tc);

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
  private <T> CounterexampleTraceInfo<I> buildCounterexampleTraceWithSpecifiedItp(
      List<Formula> pFormulas, Set<ARTElement> elementsOnPath, InterpolatingTheoremProver<T> pItpProver) throws CPAException, InterruptedException {

    logger.log(Level.FINEST, "Building counterexample trace");
    refStats.cexAnalysisTimer.start();
    pItpProver.init();
    try {

      // Final adjustments to the list of formulas
      List<Formula> f = new ArrayList<Formula>(pFormulas); // copy because we will change the list

      if (fmgr.useBitwiseAxioms()) {
        addBitwiseAxioms(f);
      }

      f = Collections.unmodifiableList(f);
      logger.log(Level.ALL, "Counterexample trace formulas:", f);

      // now f is the DAG formula which is satisfiable iff there is a
      // concrete counterexample


      // Check if refinement problem is not too big
      if (maxRefinementSize > 0) {
        int size = fmgr.dumpFormula(fmgr.makeConjunction(f)).length();
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
      CounterexampleTraceInfo<I> info;
      if (spurious) {

        List<Formula> interpolants = getInterpolants(pItpProver, itpGroupsIds);
        if (verifyInterpolants) {
          verifyInterpolants(interpolants, f, pItpProver);
        }
        info = extractPredicates(interpolants);

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

    Formula[] needed = new Formula[f.size()];
    for (int i = 0; i < needed.length; ++i) {
      needed[i] = fmgr.makeTrue();
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
      if (thmProver.isUnsat()) {
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
          if (fromStart) {
            ++s;
          } else {
            --e;
          }
          fromStart = !fromStart;

          Formula t = f.get(i);
          thmProver.push(t);
          ++toPop;
          if (thmProver.isUnsat()) {
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

          if (e < s) {
            break;
          }
        }
      } else {
        for (int i = pos; suffixTrace ? i >= 0 : i < f.size();
        i += incr) {
          Formula t = f.get(i);
          thmProver.push(t);
          ++toPop;
          if (thmProver.isUnsat()) {
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
        if (fromStart) {
          s++;
        } else {
          e--;
        }
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
   * @return A list of all the interpolants.
   */
  private <T> List<Formula> getInterpolants(
      InterpolatingTheoremProver<T> pItpProver, List<T> itpGroupsIds) {

    List<Formula> interpolants = Lists.newArrayListWithExpectedSize(itpGroupsIds.size()-1);

    // The counterexample is spurious. Get the interpolants.

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

    for (int i = 0; i < itpGroupsIds.size()-1; ++i) {
      // last iteration is left out because B would be empty
      final int start_of_a = (wellScopedPredicates ? entryPoints.peek() : 0);

      logger.log(Level.ALL, "Looking for interpolant for formulas from",
          start_of_a, "to", i);

      refStats.cexAnalysisSolverTimer.start();
      Formula itp = pItpProver.getInterpolant(itpGroupsIds.subList(start_of_a, i+1));
      refStats.cexAnalysisSolverTimer.stop();

      if (dumpInterpolationProblems) {
        File dumpFile = formatFormulaOutputFile("interpolant", i);
        fmgr.dumpFormulaToFile(itp, dumpFile);
      }

      interpolants.add(itp);

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

    return interpolants;
  }

  private <T> void verifyInterpolants(List<Formula> interpolants, List<Formula> formulas, InterpolatingTheoremProver<T> prover) throws SolverException, InterruptedException {
    refStats.interpolantVerificationTimer.start();
    try {

      final int n = interpolants.size();;
      assert n == (formulas.size() - 1);

      // The following three properties need to be checked:
      // (A)                          true      & f_0 => itp_0
      // (B) \forall i \in [1..n-1] : itp_{i-1} & f_i => itp_i
      // (C)                          itp_{n-1} & f_n => false

      // Check (A)
      if (!checkImplication(formulas.get(0), interpolants.get(0), prover)) {
        throw new SolverException("First interpolant is not implied by first formula");
      }

      // Check (B).
      for (int i = 1; i <= (n-1); i++) {
        Formula conjunct = fmgr.makeAnd(interpolants.get(i-1), formulas.get(i));

        if (!checkImplication(conjunct, interpolants.get(i), prover)) {
          throw new SolverException("Interpolant " + interpolants.get(i) + " is not implied by previous part of the path");
        }
      }

      // Check (C).
      Formula conjunct = fmgr.makeAnd(interpolants.get(n-1), formulas.get(n));
      if (!checkImplication(conjunct, fmgr.makeFalse(), prover)) {
        throw new SolverException("Last interpolant fails to prove infeasibility of the path");
      }


      // Furthermore, check if the interpolants contains only the allowed variables
      List<Set<String>> variablesInFormulas = Lists.newArrayListWithExpectedSize(formulas.size());
      for (Formula f : formulas) {
        variablesInFormulas.add(fmgr.extractVariables(f));
      }

      for (int i = 0; i < interpolants.size(); i++) {

        Set<String> variablesInA = new HashSet<String>();
        for (int j = 0; j <= i; j++) {
          // formula i is in group A
          variablesInA.addAll(variablesInFormulas.get(j));
        }

        Set<String> variablesInB = new HashSet<String>();
        for (int j = i+1; j < formulas.size(); j++) {
          // formula i is in group A
          variablesInB.addAll(variablesInFormulas.get(j));
        }

        Set<String> allowedVariables = Sets.intersection(variablesInA, variablesInB).immutableCopy();
        Set<String> variablesInInterpolant = fmgr.extractVariables(interpolants.get(i));

        variablesInInterpolant.removeAll(allowedVariables);

        if (!variablesInInterpolant.isEmpty()) {
          throw new SolverException("Interpolant "  + interpolants.get(i) + " contains forbidden variable(s) " + variablesInInterpolant);
        }
      }

    } finally {
      refStats.interpolantVerificationTimer.stop();
    }
  }

  private <T> boolean checkImplication(Formula a, Formula b, InterpolatingTheoremProver<T> prover) throws InterruptedException {
    // check unsatisfiability of negation of (a => b),
    // i.e., check unsatisfiability of (a & !b)
    Formula f = fmgr.makeAnd(a, fmgr.makeNot(b));
    prover.reset();
    prover.init();

    prover.addFormula(f);
    boolean unsat = prover.isUnsat();
    return unsat;
  }

  /**
   * Get the predicates out of the interpolants.
   *
   * @param interpolants the interpolants
   * @return Information about the counterexample, including the predicates.
   * @throws RefinementFailedException If there were no predicates.
   */
  private <T> CounterexampleTraceInfo<I> extractPredicates(
      List<Formula> interpolants) throws RefinementFailedException {

    // the counterexample is spurious. Extract the predicates from
    // the interpolants

    CounterexampleTraceInfo<I> info = new CounterexampleTraceInfo<I>();
    boolean foundPredicates = false;

    int i = 1;
    for (Formula itp : interpolants) {
      I preds;

      if (itp.isTrue()) {
        logger.log(Level.ALL, "For step", i, "got no interpolant.");
        preds = getTrueInterpolant();

      } else {
        logger.log(Level.ALL, "For step", i, "got:", "interpolant", itp);
        foundPredicates = true;

        preds = convertInterpolant(itp, i);
      }
      info.addPredicatesForRefinement(preds);
      i++;
    }

    if (!foundPredicates) {
      throw new RefinementFailedException(RefinementFailedException.Reason.InterpolationFailed, null);
    }
    return info;
  }

  protected abstract I convertInterpolant(Formula itp, int step);

  protected abstract I getTrueInterpolant();

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
  private <T> CounterexampleTraceInfo<I> getErrorPath(List<Formula> f,
      InterpolatingTheoremProver<T> pItpProver, Set<ARTElement> elementsOnPath)
      throws CPATransferException, InterruptedException {

    // get the branchingFormula
    // this formula contains predicates for all branches we took
    // this way we can figure out which branches make a feasible path
    Formula branchingFormula = pmgr.buildBranchingFormula(elementsOnPath);

    if (branchingFormula.isTrue()) {
      return new CounterexampleTraceInfo<I>(f, pItpProver.getModel(), Collections.<Integer, Boolean>emptyMap());
    }

    // add formula to solver environment
    pItpProver.addFormula(branchingFormula);

    // need to ask solver for satisfiability again,
    // otherwise model doesn't contain new predicates
    boolean stillSatisfiable = !pItpProver.isUnsat();

    if (stillSatisfiable) {
      Model model = pItpProver.getModel();
      return new CounterexampleTraceInfo<I>(f, model, pmgr.getBranchingPredicateValuesFromModel(model));

    } else {
      // this should not happen
      logger.log(Level.WARNING, "Could not get precise error path information because of inconsistent reachingPathsFormula!");

      dumpInterpolationProblem(f);
      File dumpFile = formatFormulaOutputFile("formula", f.size());
      fmgr.dumpFormulaToFile(branchingFormula, dumpFile);

      return new CounterexampleTraceInfo<I>(f, new Model(fmgr), Collections.<Integer, Boolean>emptyMap());
    }
  }


  public CounterexampleTraceInfo<I> checkPath(List<CFAEdge> pPath) throws CPATransferException {
    Formula f = pmgr.makeFormulaForPath(pPath).getFormula();

    thmProver.init();
    try {
      thmProver.push(f);
      if (thmProver.isUnsat()) {
        return new CounterexampleTraceInfo<I>();
      } else {
        return new CounterexampleTraceInfo<I>(Collections.singletonList(f), thmProver.getModel(), ImmutableMap.<Integer, Boolean>of());
      }
    } finally {
      thmProver.reset();
    }
  }

  /**
   * Helper method to dump a list of formulas to files.
   */
  private void dumpInterpolationProblem(List<Formula> f) {
    int k = 0;
    for (Formula formula : f) {
      File dumpFile = formatFormulaOutputFile("formula", k++);
      fmgr.dumpFormulaToFile(formula, dumpFile);
    }
  }

  protected File formatFormulaOutputFile(String formula, int index) {
    if (dumpInterpolationProblems) {
      return fmgr.formatFormulaOutputFile("interpolation", refStats.cexAnalysisTimer.getNumberOfIntervals(), formula, index);
    } else {
      return null;
    }
  }
}