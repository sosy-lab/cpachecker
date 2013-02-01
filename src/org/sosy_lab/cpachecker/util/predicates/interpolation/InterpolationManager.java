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

import static com.google.common.collect.FluentIterable.from;

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
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingTheoremProver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;


@Options(prefix="cpa.predicate.refinement")
public final class InterpolationManager {

  private final Timer cexAnalysisTimer = new Timer();
  private final Timer satCheckTimer = new Timer();
  private final Timer getInterpolantTimer = new Timer();
  private final Timer cexAnalysisGetUsefulBlocksTimer = new Timer();
  private final Timer interpolantVerificationTimer = new Timer();

  public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
    out.println("  Counterexample analysis:            " + cexAnalysisTimer + " (Max: " + cexAnalysisTimer.printMaxTime() + ", Calls: " + cexAnalysisTimer.getNumberOfIntervals() + ")");
    if (cexAnalysisGetUsefulBlocksTimer.getMaxTime() != 0) {
      out.println("    Cex.focusing:                     " + cexAnalysisGetUsefulBlocksTimer + " (Max: " + cexAnalysisGetUsefulBlocksTimer.printMaxTime() + ")");
    }
    out.println("    Refinement sat check:             " + satCheckTimer);
    out.println("    Interpolant computation:          " + getInterpolantTimer);
    if (interpolantVerificationTimer.getNumberOfIntervals() > 0) {
      out.println("    Interpolant verification:         " + interpolantVerificationTimer);
    }
  }


  private final LogManager logger;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final PathFormulaManager pmgr;
  private final Solver solver;

  private final InterpolatingTheoremProver<?> itpProver;

  @Option(description="apply deletion-filter to the abstract counterexample, to get "
    + "a minimal set of blocks, before applying interpolation-based refinement")
  private boolean getUsefulBlocks = false;

  @Option(name="incrementalCexTraceCheck",
      description="use incremental search in counterexample analysis, "
        + "to find the minimal infeasible prefix")
  private boolean incrementalCheck = false;

  @Option(name="cexTraceCheckDirection",
      description="Direction for doing counterexample analysis: from start of trace, from end of trace, or alternatingly from start and end of the trace towards the middle")
  private CexTraceAnalysisDirection direction = CexTraceAnalysisDirection.FORWARDS;
  private static enum CexTraceAnalysisDirection {
    FORWARDS,
    BACKWARDS,
    ZIGZAG,
    ;
  }

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

  @Option(description="skip refinement if input formula is larger than "
    + "this amount of bytes (ignored if 0)")
  private int maxRefinementSize = 0;

  private final ExecutorService executor;


  public InterpolationManager(
      FormulaManagerView pFmgr,
      PathFormulaManager pPmgr,
      Solver pSolver,
      FormulaManagerFactory pFmgrFactory,
      Configuration config,
      LogManager pLogger) throws InvalidConfigurationException {
    config.inject(this, InterpolationManager.class);

    logger = pLogger;
    fmgr = pFmgr;
    bfmgr = fmgr.getBooleanFormulaManager();
    pmgr = pPmgr;
    solver = pSolver;

    itpProver = pFmgrFactory.createInterpolatingTheoremProver(false);

    if (itpTimeLimit == 0) {
      executor = null;
    } else {
      // important to use daemon threads here, because we never have the chance to stop the executor
      executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).build());
    }

    if (wellScopedPredicates) {
      throw new InvalidConfigurationException("wellScopedPredicates are currently disabled");
    }
//    if (inlineFunctions && wellScopedPredicates) {
//      logger.log(Level.WARNING, "Well scoped predicates not possible with function inlining, disabling them.");
//      wellScopedPredicates = false;
//    }
  }

  public String dumpCounterexample(CounterexampleTraceInfo cex) {
    return fmgr.dumpFormula(bfmgr.conjunction(cex.getCounterExampleFormulas()));
  }

  /**
   * Counterexample analysis.
   * This method is just an helper to delegate the actual work
   * This is used to detect timeouts for interpolation
   *
   * @param pFormulas the formulas for the path
   * @param elementsOnPath the ARGElements on the path (may be empty if no branching information is required)
   * @throws CPAException
   * @throws InterruptedException
   */
  public CounterexampleTraceInfo buildCounterexampleTrace(
      final List<BooleanFormula> pFormulas,
      final Set<ARGState> elementsOnPath) throws CPAException, InterruptedException {

    // if we don't want to limit the time given to the solver
    if (itpTimeLimit == 0) {
      return buildCounterexampleTraceWithSpecifiedItp(pFormulas, elementsOnPath, itpProver);
    }

    assert executor != null;

    Callable<CounterexampleTraceInfo> tc = new Callable<CounterexampleTraceInfo>() {
      @Override
      public CounterexampleTraceInfo call() throws CPAException, InterruptedException {
        return buildCounterexampleTraceWithSpecifiedItp(pFormulas, elementsOnPath, itpProver);
      }
    };

    Future<CounterexampleTraceInfo> future = executor.submit(tc);

    try {
      // here we get the result of the post computation but there is a time limit
      // given to complete the task specified by timeLimit
      return future.get(itpTimeLimit, TimeUnit.MILLISECONDS);

    } catch (TimeoutException e){
      logger.log(Level.SEVERE, "SMT-solver timed out during interpolation process");
      throw new RefinementFailedException(Reason.TIMEOUT, null);

    } catch (ExecutionException e) {
      Throwable t = e.getCause();
      Throwables.propagateIfPossible(t, CPAException.class, InterruptedException.class);

      throw new UnexpectedCheckedException("interpolation", t);
    }
  }

  /**
   * Counterexample analysis and predicate discovery.
   * @param pFormulas the formulas for the path
   * @param elementsOnPath the ARGElements on the path (may be empty if no branching information is required)
   * @param pItpProver interpolation solver used
   * @return counterexample info with predicated information
   * @throws CPAException
   */
  private <T> CounterexampleTraceInfo buildCounterexampleTraceWithSpecifiedItp(
      List<BooleanFormula> pFormulas, Set<ARGState> elementsOnPath, InterpolatingTheoremProver<T> pItpProver) throws CPAException, InterruptedException {

    logger.log(Level.FINEST, "Building counterexample trace");
    cexAnalysisTimer.start();
    pItpProver.init();
    try {

      // Final adjustments to the list of formulas
      List<BooleanFormula> f = new ArrayList<>(pFormulas); // copy because we will change the list

      if (bfmgr.useBitwiseAxioms()) {
        addBitwiseAxioms(f);
      }

      f = Collections.unmodifiableList(f);
      logger.log(Level.ALL, "Counterexample trace formulas:", f);

      // now f is the DAG formula which is satisfiable iff there is a
      // concrete counterexample


      // Check if refinement problem is not too big
      if (maxRefinementSize > 0) {
        int size = fmgr.dumpFormula(bfmgr.conjunction(f)).length();
        if (size > maxRefinementSize) {
          logger.log(Level.FINEST, "Skipping refinement because input formula is", size, "bytes large.");
          throw new RefinementFailedException(Reason.TooMuchUnrolling, null);
        }
      }


      // Check feasibility of counterexample
      logger.log(Level.FINEST, "Checking feasibility of counterexample trace");
      satCheckTimer.start();

      boolean spurious;
      List<T> itpGroupsIds;
      try {

        if (getUsefulBlocks) {
          f = Collections.unmodifiableList(getUsefulBlocks(f));
        }

        if (dumpInterpolationProblems) {
          dumpInterpolationProblem(f);
        }

        // re-order formulas if needed
        List<Pair<BooleanFormula, Integer>> orderedFormulas = orderFormulas(f);

        // initialize all interpolation group ids with "null"
        itpGroupsIds = new ArrayList<>(Collections.<T>nCopies(f.size(), null));

        // ask solver for satisfiability
        spurious = checkInfeasabilityOfTrace(orderedFormulas, itpGroupsIds, pItpProver);
        assert itpGroupsIds.size() == f.size();
        assert !itpGroupsIds.contains(null); // has to be filled completely

      } finally {
        satCheckTimer.stop();
      }

      logger.log(Level.FINEST, "Counterexample trace is", (spurious ? "infeasible" : "feasible"));


      // Get either interpolants or error path information
      CounterexampleTraceInfo info;
      if (spurious) {

        List<BooleanFormula> interpolants = getInterpolants(pItpProver, itpGroupsIds);
        if (verifyInterpolants) {
          verifyInterpolants(interpolants, f, pItpProver);
        }

        if (logger.wouldBeLogged(Level.ALL)) {
          int i = 1;
          for (BooleanFormula itp : interpolants) {
            logger.log(Level.ALL, "For step", i++, "got:", "interpolant", itp);
          }
        }

        info = CounterexampleTraceInfo.infeasible(interpolants);

      } else {
        // this is a real bug
        info = getErrorPath(f, pItpProver, elementsOnPath);
      }

      logger.log(Level.ALL, "Counterexample information:", info);

      return info;

    } finally {
      pItpProver.reset();
      cexAnalysisTimer.stop();
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
  private void addBitwiseAxioms(List<BooleanFormula> f) {
    BooleanFormula bitwiseAxioms = bfmgr.makeBoolean(true);

    for (BooleanFormula fm : f) {
      BooleanFormula a = fmgr.getBitwiseAxioms(fm);
      if (!bfmgr.isTrue(a)) {
        bitwiseAxioms =  fmgr.getBooleanFormulaManager().and(bitwiseAxioms, a);
      }
    }

    if (!bfmgr.isTrue(bitwiseAxioms)) {
      logger.log(Level.ALL, "DEBUG_3", "ADDING BITWISE AXIOMS TO THE",
          "LAST GROUP: ", bitwiseAxioms);
      int lastIndex = f.size()-1;
      f.set(lastIndex, bfmgr.and(f.get(lastIndex), bitwiseAxioms));
    }
  }

  /**
   * Try to find out which formulas out of a list of formulas are relevant for
   * making the conjunction unsatisfiable.
   * This method honors the {@link #direction} configuration option.
   *
   * @param f The list of formulas to check.
   * @return A sublist of f that contains the useful formulas.
   */
  private List<BooleanFormula> getUsefulBlocks(List<BooleanFormula> f) {

    cexAnalysisGetUsefulBlocksTimer.start();

    // try to find a minimal-unsatisfiable-core of the trace (as Blast does)

    try (ProverEnvironment thmProver = solver.getTheoremProver()) {

    logger.log(Level.ALL, "DEBUG_1", "Calling getUsefulBlocks on path",
        "of length:", f.size());

    final BooleanFormula[] needed = new BooleanFormula[f.size()];
    for (int i = 0; i < needed.length; ++i) {
      needed[i] =  bfmgr.makeBoolean(true);
    }
    final boolean backwards = direction == CexTraceAnalysisDirection.BACKWARDS;
    final int start = backwards ? f.size()-1 : 0;
    final int increment = backwards ? -1 : 1;
    int toPop = 0;

    while (true) {
      boolean consistent = true;
      // 1. assert all the needed constraints
      for (int i = 0; i < needed.length; ++i) {
        if (!bfmgr.isTrue(needed[i])) {
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
      if (direction == CexTraceAnalysisDirection.ZIGZAG) {
        int s = 0;
        int e = f.size()-1;
        boolean fromStart = false;
        while (true) {
          int i = fromStart ? s++ : e--;
          fromStart = !fromStart;

          BooleanFormula t = f.get(i);
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
        for (int i = start;
             backwards ? i >= 0 : i < f.size();
             i += increment) {
          BooleanFormula t = f.get(i);
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

    }

    logger.log(Level.ALL, "DEBUG_1", "Done getUsefulBlocks");

    cexAnalysisGetUsefulBlocksTimer.stop();

    return f;
  }

  /**
   * Put the list of formulas into the order in which they should be given to
   * the solver, as defined by the {@link #direction} configuration option.
   * @param traceFormulas The list of formulas to check.
   * @return The same list of formulas in different order, and each formula has its position in the original list as second element of the pair.
   */
  private List<Pair<BooleanFormula, Integer>> orderFormulas(final List<BooleanFormula> traceFormulas) {

    // In this list are all formulas together with their position in the original list
    ImmutableList.Builder<Pair<BooleanFormula, Integer>> orderedFormulas = ImmutableList.builder();

    if (direction == CexTraceAnalysisDirection.ZIGZAG) {
      int e = traceFormulas.size()-1;
      int s = 0;
      boolean fromStart = false;
      while (s <= e) {
        int i = fromStart ? s++ : e--;
        fromStart = !fromStart;

        orderedFormulas.add(Pair.of(traceFormulas.get(i), i));
      }

    } else {
      final boolean backwards = direction == CexTraceAnalysisDirection.BACKWARDS;
      final int increment = backwards ? -1 : 1;

      for (int i = backwards ? traceFormulas.size()-1 : 0;
           backwards ? i >= 0 : i < traceFormulas.size();
           i += increment) {

        orderedFormulas.add(Pair.of(traceFormulas.get(i), i));
      }
    }

    ImmutableList<Pair<BooleanFormula, Integer>> result = orderedFormulas.build();
    assert traceFormulas.size() == result.size();
    assert ImmutableMultiset.copyOf(from(result).transform(Pair.getProjectionToFirst()))
                             .equals(ImmutableMultiset.copyOf(traceFormulas))
            : "Ordered list does not contain the same formulas with the same count";
    return result;
  }

  /**
   * Check the satisfiability of a list of formulas, using them in the given order.
   * This method honors the {@link #incrementalCheck} configuration option.
   *
   * @param traceFormulas The list of formulas to check, each formula with its index of where it should be added in the list of interpolation groups.
   * @param itpGroupsIds The list where to store the references to the interpolation groups.
   * @param pItpProver The solver to use.
   * @return True if the formulas are unsatisfiable.
   * @throws InterruptedException
   */
  private <T> boolean checkInfeasabilityOfTrace(List<Pair<BooleanFormula, Integer>> traceFormulas,
      List<T> itpGroupsIds, InterpolatingTheoremProver<T> pItpProver) throws InterruptedException {

    boolean isStillFeasible = true;

    for (Pair<BooleanFormula, Integer> p : traceFormulas) {
      BooleanFormula f = p.getFirst();
      int index = p.getSecond();

      assert itpGroupsIds.get(index) == null;
      itpGroupsIds.set(index, pItpProver.addFormula(f));

      if (incrementalCheck && isStillFeasible && !bfmgr.isTrue(f)) {
        if (pItpProver.isUnsat()) {
          // We need to iterate through the full loop
          // to add all formulas, but this prevents us from doing further sat checks.
          isStillFeasible = false;
        }
      }
    }

    if (incrementalCheck) {
      // we did unsat checks
      return !isStillFeasible;
    } else {
      return pItpProver.isUnsat();
    }
  }

  /**
   * Get the interpolants from the solver after the formulas have been proved
   * to be unsatisfiable.
   *
   * @param pItpProver The solver.
   * @param itpGroupsIds The references to the interpolation groups
   * @return A list of all the interpolants.
   */
  private <T> List<BooleanFormula> getInterpolants(
      InterpolatingTheoremProver<T> pItpProver, List<T> itpGroupsIds) {

    List<BooleanFormula> interpolants = Lists.newArrayListWithExpectedSize(itpGroupsIds.size()-1);

    // The counterexample is spurious. Get the interpolants.

    // how to partition the trace into (A, B) depends on whether
    // there are function calls involved or not: in general, A
    // is the trace from the entry point of the current function
    // to the current point, and B is everything else. To implement
    // this, we keep track of which function we are currently in.
    // if we don't want "well-scoped" predicates, A always starts at the beginning
    Deque<Integer> entryPoints = null;
    if (wellScopedPredicates) {
      entryPoints = new ArrayDeque<>();
      entryPoints.push(0);
    }

    for (int i = 0; i < itpGroupsIds.size()-1; ++i) {
      // last iteration is left out because B would be empty
      final int start_of_a = (wellScopedPredicates ? entryPoints.peek() : 0);

      logger.log(Level.ALL, "Looking for interpolant for formulas from",
          start_of_a, "to", i);

      getInterpolantTimer.start();
      BooleanFormula itp = pItpProver.getInterpolant(itpGroupsIds.subList(start_of_a, i+1));
      getInterpolantTimer.stop();

      if (dumpInterpolationProblems) {
        dumpFormulaToFile("interpolant", itp, i);
      }

      interpolants.add(itp);

      // TODO wellScopedPredicates have been disabled

      // TODO the following code relies on the fact that there is always an abstraction on function call and return

      // If we are entering or exiting a function, update the stack
      // of entry points
      // TODO checking if the abstraction node is a new function
//        if (wellScopedPredicates && e.getAbstractionLocation() instanceof FunctionEntryNode) {
//          entryPoints.push(i);
//        }
        // TODO check we are returning from a function
//        if (wellScopedPredicates && e.getAbstractionLocation().getEnteringSummaryEdge() != null) {
//          entryPoints.pop();
//        }
    }

    return interpolants;
  }

  private <T> void verifyInterpolants(List<BooleanFormula> interpolants, List<BooleanFormula> formulas, InterpolatingTheoremProver<T> prover) throws SolverException, InterruptedException {
    interpolantVerificationTimer.start();
    try {

      final int n = interpolants.size();
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
        BooleanFormula conjunct = bfmgr.and(interpolants.get(i-1), formulas.get(i));

        if (!checkImplication(conjunct, interpolants.get(i), prover)) {
          throw new SolverException("Interpolant " + interpolants.get(i) + " is not implied by previous part of the path");
        }
      }

      // Check (C).
      BooleanFormula conjunct = bfmgr.and(interpolants.get(n-1), formulas.get(n));
      if (!checkImplication(conjunct, bfmgr.makeBoolean(false), prover)) {
        throw new SolverException("Last interpolant fails to prove infeasibility of the path");
      }


      // Furthermore, check if the interpolants contains only the allowed variables
      List<Set<String>> variablesInFormulas = Lists.newArrayListWithExpectedSize(formulas.size());
      for (BooleanFormula f : formulas) {
        variablesInFormulas.add(fmgr.extractVariables(f));
      }

      for (int i = 0; i < interpolants.size(); i++) {

        Set<String> variablesInA = new HashSet<>();
        for (int j = 0; j <= i; j++) {
          // formula i is in group A
          variablesInA.addAll(variablesInFormulas.get(j));
        }

        Set<String> variablesInB = new HashSet<>();
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
      interpolantVerificationTimer.stop();
    }
  }

  private <T> boolean checkImplication(BooleanFormula a, BooleanFormula b, InterpolatingTheoremProver<T> prover) throws InterruptedException {
    // check unsatisfiability of negation of (a => b),
    // i.e., check unsatisfiability of (a & !b)
    BooleanFormula f = bfmgr.and(a, bfmgr.not(b));
    prover.reset();
    prover.init();

    prover.addFormula(f);
    boolean unsat = prover.isUnsat();
    return unsat;
  }

  /**
   * Get information about the error path from the solver after the formulas
   * have been proved to be satisfiable.
   *
   * @param f The list of formulas on the path.
   * @param pItpProver The solver.
   * @param elementsOnPath The ARGElements of the paths represented by f.
   * @return Information about the error path, including a satisfying assignment.
   * @throws CPATransferException
   * @throws InterruptedException
   */
  private <T> CounterexampleTraceInfo getErrorPath(List<BooleanFormula> f,
      InterpolatingTheoremProver<T> pItpProver, Set<ARGState> elementsOnPath)
      throws CPATransferException, InterruptedException {

    // get the branchingFormula
    // this formula contains predicates for all branches we took
    // this way we can figure out which branches make a feasible path
    BooleanFormula branchingFormula = pmgr.buildBranchingFormula(elementsOnPath);

    if (bfmgr.isTrue(branchingFormula)) {
      return CounterexampleTraceInfo.feasible(f, getModel(pItpProver), ImmutableMap.<Integer, Boolean>of());
    }

    // add formula to solver environment
    pItpProver.addFormula(branchingFormula);

    // need to ask solver for satisfiability again,
    // otherwise model doesn't contain new predicates
    boolean stillSatisfiable = !pItpProver.isUnsat();

    if (stillSatisfiable) {
      Model model = getModel(pItpProver);
      return CounterexampleTraceInfo.feasible(f, model, pmgr.getBranchingPredicateValuesFromModel(model));

    } else {
      // this should not happen
      logger.log(Level.WARNING, "Could not get precise error path information because of inconsistent reachingPathsFormula!");

      dumpInterpolationProblem(f);
      dumpFormulaToFile("formula", branchingFormula, f.size());

      return CounterexampleTraceInfo.feasible(f, new Model(fmgr), ImmutableMap.<Integer, Boolean>of());
    }
  }

  private <T> Model getModel(InterpolatingTheoremProver<T> pItpProver) {
    try {
      return pItpProver.getModel();
    } catch (SolverException e) {
      logger.log(Level.WARNING, "Solver could not produce model, variable assignment of error path can not be dumped.");
      logger.logDebugException(e);
      return new Model(fmgr); // return empty model
    }
  }

  public CounterexampleTraceInfo checkPath(List<CFAEdge> pPath) throws CPATransferException {
    BooleanFormula f = pmgr.makeFormulaForPath(pPath).getFormula();

    try (ProverEnvironment thmProver = solver.getTheoremProver()) {
      thmProver.push(f);
      if (thmProver.isUnsat()) {
        return CounterexampleTraceInfo.infeasible(ImmutableList.<BooleanFormula>of());
      } else {
        return CounterexampleTraceInfo.feasible(ImmutableList.of(f), getModel(thmProver), ImmutableMap.<Integer, Boolean>of());
      }
    }
  }

  private <T> Model getModel(ProverEnvironment thmProver) {
    try {
      return thmProver.getModel();
    } catch (SolverException e) {
      logger.log(Level.WARNING, "Solver could not produce model, variable assignment of error path can not be dumped.");
      logger.logDebugException(e);
      return new Model(fmgr); // return empty model
    }
  }


  /**
   * Helper method to dump a list of formulas to files.
   */
  private void dumpInterpolationProblem(List<BooleanFormula> f) {
    int k = 0;
    for (BooleanFormula formula : f) {
      File dumpFile = formatFormulaOutputFile("formula", k++);
      fmgr.dumpFormulaToFile(formula, dumpFile);
    }
  }

  private void dumpFormulaToFile(String name, BooleanFormula f, int i) {
    File dumpFile = formatFormulaOutputFile(name, i);
    fmgr.dumpFormulaToFile(f, dumpFile);
  }

  private File formatFormulaOutputFile(String formula, int index) {
    return fmgr.formatFormulaOutputFile("interpolation", cexAnalysisTimer.getNumberOfIntervals(), formula, index);
  }
}