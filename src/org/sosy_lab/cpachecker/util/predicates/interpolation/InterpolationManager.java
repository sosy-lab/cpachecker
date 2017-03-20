/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsUtils.div;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
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
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.predicates.interpolation.strategy.ITPStrategy;
import org.sosy_lab.cpachecker.util.predicates.interpolation.strategy.NestedInterpolation;
import org.sosy_lab.cpachecker.util.predicates.interpolation.strategy.SequentialInterpolation;
import org.sosy_lab.cpachecker.util.predicates.interpolation.strategy.SequentialInterpolationWithSolver;
import org.sosy_lab.cpachecker.util.predicates.interpolation.strategy.TreeInterpolation;
import org.sosy_lab.cpachecker.util.predicates.interpolation.strategy.TreeInterpolationWithSolver;
import org.sosy_lab.cpachecker.util.predicates.interpolation.strategy.WellScopedInterpolation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.java_smt.api.BasicProverEnvironment;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;


@Options(prefix="cpa.predicate.refinement")
public final class InterpolationManager {

  private final Timer cexAnalysisTimer = new Timer();
  private final Timer satCheckTimer = new Timer();
  private final Timer getInterpolantTimer = new Timer();
  private final Timer cexAnalysisGetUsefulBlocksTimer = new Timer();
  private final Timer interpolantVerificationTimer = new Timer();
  private int reusedFormulasOnSolverStack = 0;

  public void printStatistics(StatisticsWriter w0) {
    w0.put("Counterexample analysis", cexAnalysisTimer + " (Max: " + cexAnalysisTimer.getMaxTime().formatAs(TimeUnit.SECONDS) + ", Calls: " + cexAnalysisTimer.getNumberOfIntervals() + ")");
    StatisticsWriter w1 = w0.beginLevel();
    if (cexAnalysisGetUsefulBlocksTimer.getNumberOfIntervals() > 0) {
      w1.put("Cex.focusing", cexAnalysisGetUsefulBlocksTimer + " (Max: " + cexAnalysisGetUsefulBlocksTimer.getMaxTime().formatAs(TimeUnit.SECONDS) + ")");
    }
    w1.put("Refinement sat check", satCheckTimer);
    if (reuseInterpolationEnvironment && satCheckTimer.getNumberOfIntervals() > 0) {
      w1.put("Reused formulas on solver stack", reusedFormulasOnSolverStack + " (Avg: " + div(reusedFormulasOnSolverStack, satCheckTimer.getNumberOfIntervals()) + ")");
    }
    w1.put("Interpolant computation", getInterpolantTimer);
    if (interpolantVerificationTimer.getNumberOfIntervals() > 0) {
      w1.put("Interpolant verification", interpolantVerificationTimer);
    }
  }


  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final PathFormulaManager pmgr;
  private final Solver solver;

  private final Interpolator<?> interpolator;

  @Option(secure=true, description="apply deletion-filter to the abstract counterexample, to get "
    + "a minimal set of blocks, before applying interpolation-based refinement")
  private boolean getUsefulBlocks = false;

  @Option(secure=true, name="incrementalCexTraceCheck",
      description="use incremental search in counterexample analysis, "
        + "to find the minimal infeasible prefix")
  private boolean incrementalCheck = false;

  @Option(secure=true, name="cexTraceCheckDirection",
      description="Direction for doing counterexample analysis: from start of trace, from end of trace, or alternatingly from start and end of the trace towards the middle")
  private CexTraceAnalysisDirection direction = CexTraceAnalysisDirection.FORWARDS;

  @Option(secure=true, description="Strategy how to interact with the intepolating prover. " +
          "The analysis must support the strategy, otherwise the result will be useless!" +
          "\n- SEQ_CPACHECKER: We simply return each interpolant for i={0..n-1} for the partitions A=[0 .. i] and B=[i+1 .. n]. " +
          "The result is similar to INDUCTIVE_SEQ, but we do not guarantee the 'inductiveness', " +
          "i.e. the solver has to generate nice interpolants itself. Supported by all solvers!" +
          "\n- INDUCTIVE_SEQ: Generate an inductive sequence of interpolants the partitions [1,...n]. " +
          "\n- TREE: use the tree-interpolation-feature of a solver to get interpolants" +
          "\n- TREE_WELLSCOPED: We return each interpolant for i={0..n-1} for the partitions " +
          "A=[lastFunctionEntryIndex .. i] and B=[0 .. lastFunctionEntryIndex-1 , i+1 .. n]. Based on a tree-like scheme." +
          "\n- TREE_NESTED: use callstack and previous interpolants for next interpolants (see 'Nested Interpolants')," +
          "\n- TREE_CPACHECKER: similar to TREE_NESTED, but the algorithm is taken from 'Tree Interpolation in Vampire'.")
  private InterpolationStrategy strategy = InterpolationStrategy.SEQ_CPACHECKER;
  private static enum InterpolationStrategy {
    SEQ, SEQ_CPACHECKER,
    TREE, TREE_WELLSCOPED, TREE_NESTED, TREE_CPACHECKER}

  @Option(secure=true, description="dump all interpolation problems")
  private boolean dumpInterpolationProblems = false;

  @Option(secure=true, description="verify if the interpolants fulfill the interpolant properties")
  private boolean verifyInterpolants = false;

  @Option(secure=true, name="timelimit",
      description="time limit for refinement (use milliseconds or specify a unit; 0 for infinite)")
  @TimeSpanOption(codeUnit=TimeUnit.MILLISECONDS,
      defaultUserUnit=TimeUnit.MILLISECONDS,
      min=0)
  private TimeSpan itpTimeLimit = TimeSpan.ofMillis(0);

  @Option(secure=true, description="skip refinement if input formula is larger than "
    + "this amount of bytes (ignored if 0)")
  private int maxRefinementSize = 0;

  @Option(secure=true, description="Use a single SMT solver environment for several interpolation queries")
  private boolean reuseInterpolationEnvironment = false;

  private final ExecutorService executor;
  private final LoopStructure loopStructure;
  private final VariableClassification variableClassification;

  public InterpolationManager(
      PathFormulaManager pPmgr,
      Solver pSolver,
      Optional<LoopStructure> pLoopStructure,
      Optional<VariableClassification> pVarClassification,
      Configuration config,
      ShutdownNotifier pShutdownNotifier,
      LogManager pLogger) throws InvalidConfigurationException {
    config.inject(this, InterpolationManager.class);

    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    fmgr = pSolver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    pmgr = pPmgr;
    solver = pSolver;
    loopStructure = pLoopStructure.orElse(null);
    variableClassification = pVarClassification.orElse(null);

    if (itpTimeLimit.isEmpty()) {
      executor = null;
    } else {
      // important to use daemon threads here, because we never have the chance to stop the executor
      executor =
          Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).build());
    }

    if (reuseInterpolationEnvironment) {
      interpolator = new Interpolator<>();
    } else {
      interpolator = null;
    }
  }

  /**
   * Counterexample analysis. This method is just an helper to delegate the actual work This is used
   * to detect timeouts for interpolation
   *
   * @param pFormulas the formulas for the path
   * @param pAbstractionStates the abstraction states between the formulas and the last state of the
   *     path. The first state (root) of the path is missing, because it is always TRUE. (can be
   *     empty, if well-scoped interpolation is disabled or not required)
   * @param elementsOnPath the ARGElements on the path (may be empty if no branching information is
   *     required)
   */
  public CounterexampleTraceInfo buildCounterexampleTrace(
      final List<BooleanFormula> pFormulas,
      final List<AbstractState> pAbstractionStates,
      final Set<ARGState> elementsOnPath)
      throws CPAException, InterruptedException {
    assert pAbstractionStates.isEmpty() || pFormulas.size() == pAbstractionStates.size();

    return callWithTimelimit(
        () -> buildCounterexampleTrace0(pFormulas, pAbstractionStates, elementsOnPath));
  }

  private CounterexampleTraceInfo callWithTimelimit(Callable<CounterexampleTraceInfo> callable)
      throws CPAException, InterruptedException {

    // if we don't want to limit the time given to the solver
    if (itpTimeLimit.isEmpty()) {
      try {
        return callable.call();
      } catch (Exception e) {
        Throwables.propagateIfPossible(e, CPAException.class, InterruptedException.class);
        throw new UnexpectedCheckedException("refinement", e);
      }
    }

    assert executor != null;

    Future<CounterexampleTraceInfo> future = executor.submit(callable);

    try {
      // here we get the result of the post computation but there is a time limit
      // given to complete the task specified by timeLimit
      return future.get(itpTimeLimit.asNanos(), TimeUnit.NANOSECONDS);

    } catch (TimeoutException e) {
      logger.log(Level.SEVERE, "SMT-solver timed out during interpolation process");
      throw new RefinementFailedException(Reason.TIMEOUT, null);

    } catch (ExecutionException e) {
      Throwable t = e.getCause();
      Throwables.propagateIfPossible(t, CPAException.class, InterruptedException.class);

      throw new UnexpectedCheckedException("interpolation", t);
    }
  }

  public CounterexampleTraceInfo buildCounterexampleTrace(
          final List<BooleanFormula> pFormulas) throws CPAException, InterruptedException {
    return buildCounterexampleTrace(
        pFormulas, Collections.<AbstractState>emptyList(), Collections.<ARGState>emptySet());
  }

  private CounterexampleTraceInfo buildCounterexampleTrace0(
      final List<BooleanFormula> pFormulas,
      final List<AbstractState> pAbstractionStates,
      final Set<ARGState> elementsOnPath)
      throws CPAException, InterruptedException {

    cexAnalysisTimer.start();
    try {
      final List<BooleanFormula> f = prepareCounterexampleFormulas(pFormulas);

      final Interpolator<?> currentInterpolator;
      if (reuseInterpolationEnvironment) {
        currentInterpolator = checkNotNull(interpolator);
      } else {
        currentInterpolator = new Interpolator<>();
      }

      try {
        try {
          return currentInterpolator.buildCounterexampleTrace(
              f, pAbstractionStates, elementsOnPath);
        } finally {
          if (!reuseInterpolationEnvironment) {
            currentInterpolator.close();
          }
        }
      } catch (SolverException itpException) {
        logger.logUserException(
            Level.FINEST,
            itpException,
            "Interpolation failed, attempting to solve without interpolation");
        return fallbackWithoutInterpolation(elementsOnPath, f, itpException);
      }

    } finally {
      cexAnalysisTimer.stop();
    }
  }

  /**
   * Counterexample analysis without interpolation. Use this method if you want to check a
   * counterexample for feasibility and in case of a feasible counterexample want the proper path
   * information, but in case of an infeasible counterexample you do not need interpolants.
   *
   * @param pFormulas the formulas for the path
   * @param statesOnPath the ARGStates on the path (may be empty if no branching information is
   *     required)
   */
  public CounterexampleTraceInfo buildCounterexampleTraceWithoutInterpolation(
      final List<BooleanFormula> pFormulas, final Set<ARGState> statesOnPath)
      throws CPAException, InterruptedException {

    return callWithTimelimit(
        () -> buildCounterexampleTraceWithoutInterpolation0(pFormulas, statesOnPath));
  }

  private CounterexampleTraceInfo buildCounterexampleTraceWithoutInterpolation0(
      final List<BooleanFormula> pFormulas, final Set<ARGState> statesOnPath)
      throws CPAException, InterruptedException {

    cexAnalysisTimer.start();
    try {
      final List<BooleanFormula> f = prepareCounterexampleFormulas(pFormulas);

      try {
        return solveCounterexample(f, statesOnPath);
      } catch (SolverException e) {
        throw new RefinementFailedException(Reason.InterpolationFailed, null, e);
      }

    } finally {
      cexAnalysisTimer.stop();
    }
  }

  /** Prepare the list of formulas for a counterexample for the solving/interpolation step. */
  private List<BooleanFormula> prepareCounterexampleFormulas(final List<BooleanFormula> pFormulas)
      throws RefinementFailedException {
    logger.log(Level.FINEST, "Building counterexample trace");

    // Final adjustments to the list of formulas
    List<BooleanFormula> f = new ArrayList<>(pFormulas); // copy because we will change the list

    if (fmgr.useBitwiseAxioms()) {
      addBitwiseAxioms(f);
    }

    f = Collections.unmodifiableList(f);
    logger.log(Level.ALL, "Counterexample trace formulas:", f);

    // now f is the DAG formula which is satisfiable iff there is a
    // concrete counterexample

    // Check if refinement problem is not too big
    if (maxRefinementSize > 0) {
      int size = fmgr.dumpFormula(bfmgr.and(f)).toString().length();
      if (size > maxRefinementSize) {
        logger.log(
            Level.FINEST, "Skipping refinement because input formula is", size, "bytes large.");
        throw new RefinementFailedException(Reason.TooMuchUnrolling, null);
      }
    }
    return f;
  }

  /**
   * Attempt to check feasibility of the current counterexample without interpolation
   * in case of a failure with interpolation.
   * Maybe the solver can handle the formulas if we do not attempt to interpolate
   * (this happens for example for MathSAT).
   * If solving works but creating the model for the error path not,
   * we at least return an empty model.
   */
  private CounterexampleTraceInfo fallbackWithoutInterpolation(
      final Set<ARGState> elementsOnPath, List<BooleanFormula> f, SolverException itpException)
      throws InterruptedException, CPATransferException, RefinementFailedException {
    try {
      CounterexampleTraceInfo counterexample = solveCounterexample(f, elementsOnPath);
      if (!counterexample.isSpurious()) {
        return counterexample;
      }
    } catch (SolverException solvingException) {
      // in case of exception throw original one below but do not forget e2
      itpException.addSuppressed(solvingException);
    }
    throw new RefinementFailedException(Reason.InterpolationFailed, null, itpException);
  }

  /** Analyze a counterexample for feasibility without computing interpolants. */
  private CounterexampleTraceInfo solveCounterexample(
      List<BooleanFormula> f, Set<ARGState> elementsOnPath)
      throws CPATransferException, SolverException, InterruptedException {
    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      for (BooleanFormula block : f) {
        prover.push(block);
      }
      if (!prover.isUnsat()) {
        try {
          return getErrorPath(f, prover, elementsOnPath);
        } catch (SolverException modelException) {
          logger.log(
              Level.WARNING,
              "Solver could not produce model, variable assignment of error path can not be dumped.");
          logger.logDebugException(modelException);
          return CounterexampleTraceInfo.feasible(
              f, ImmutableList.<ValueAssignment>of(), ImmutableMap.<Integer, Boolean>of());
        }
      } else {
        return CounterexampleTraceInfo.infeasibleNoItp();
      }
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
    BooleanFormula bitwiseAxioms = bfmgr.makeTrue();

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
  private List<BooleanFormula> getUsefulBlocks(List<BooleanFormula> f) throws SolverException, InterruptedException {

    cexAnalysisGetUsefulBlocksTimer.start();

    // try to find a minimal-unsatisfiable-core of the trace (as Blast does)

    try (ProverEnvironment thmProver = solver.newProverEnvironment()) {

    logger.log(Level.ALL, "DEBUG_1", "Calling getUsefulBlocks on path",
            "of length:", f.size());

    final BooleanFormula[] needed = new BooleanFormula[f.size()];
    for (int i = 0; i < needed.length; ++i) {
      needed[i] =  bfmgr.makeTrue();
    }
    final boolean backwards = direction == CexTraceAnalysisDirection.BACKWARDS;
    final int start = backwards ? f.size()-1 : 0;
    final int increment = backwards ? -1 : 1;
    int toPop = 0;

    while (true) {
      boolean consistent = true;
      // 1. assert all the needed constraints
      for (BooleanFormula aNeeded : needed) {
        if (!bfmgr.isTrue(aNeeded)) {
          thmProver.push(aNeeded);
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
   * @return The same list of formulas in different order,
   *         and each formula has its position in the original list as third element of the pair.
   */
  private List<Triple<BooleanFormula, AbstractState, Integer>> orderFormulas(
          final List<BooleanFormula> traceFormulas, final List<AbstractState> pAbstractionStates) {

    // In this list are all formulas together with their position in the original list
    ImmutableList<Triple<BooleanFormula, AbstractState, Integer>> result = direction.orderFormulas(traceFormulas,
                                                                                                   pAbstractionStates,
                                                                                                   variableClassification,
                                                                                                   loopStructure,
                                                                                                   fmgr);
    assert traceFormulas.size() == result.size();
    assert ImmutableMultiset.copyOf(from(result).transform(Triple::getFirst))
            .equals(ImmutableMultiset.copyOf(traceFormulas))
        : "Ordered list does not contain the same formulas with the same count";
    return result;
  }

  /**
   * Get the interpolants from the solver after the formulas have been proved
   * to be unsatisfiable.
   *
   * @param pInterpolator The references to the interpolation groups, sorting depends on the solver-stack.
   * @param formulasWithStatesAndGroupdIds list of (F,A,T) of path formulae F
   *        with their interpolation group I and the abstract state A,
   *        where a path formula F is the path before/until the abstract state A.
   *        The list is sorted in the "correct" order along the counterexample.
   * @return A list of (N-1) interpolants for N formulae.
   */
  private <T> List<BooleanFormula> getInterpolants(Interpolator<T> pInterpolator,
      List<Triple<BooleanFormula, AbstractState, T>> formulasWithStatesAndGroupdIds)
          throws SolverException, InterruptedException {
    // TODO replace with Config-Class-Constructor-Injection?
    final  ITPStrategy<T> itpStrategy;
    switch (strategy) {
      case SEQ_CPACHECKER:
        itpStrategy = new SequentialInterpolation<>(logger, shutdownNotifier, fmgr, bfmgr);
        break;
      case SEQ:
        itpStrategy = new SequentialInterpolationWithSolver<>(logger, shutdownNotifier, fmgr, bfmgr);
        break;
      case TREE_WELLSCOPED:
        itpStrategy = new WellScopedInterpolation<>(logger, shutdownNotifier, fmgr, bfmgr);
        break;
      case TREE_NESTED:
        itpStrategy = new NestedInterpolation<>(logger, shutdownNotifier, fmgr, bfmgr);
        break;
      case TREE_CPACHECKER:
        itpStrategy = new TreeInterpolation<>(logger, shutdownNotifier, fmgr, bfmgr);
        break;
      case TREE:
        itpStrategy = new TreeInterpolationWithSolver<>(logger, shutdownNotifier, fmgr, bfmgr);
        break;
      default:
        throw new AssertionError("unknown interpolation strategy");
    }

    final List<BooleanFormula> interpolants = itpStrategy.getInterpolants(pInterpolator, formulasWithStatesAndGroupdIds);

    assert formulasWithStatesAndGroupdIds.size() - 1 == interpolants.size() : "we should return N-1 interpolants for N formulas.";

    if (verifyInterpolants) {
      itpStrategy.checkInterpolants(solver, formulasWithStatesAndGroupdIds, interpolants);
    }

    return interpolants;
  }

  /**
   * Get information about the error path from the solver after the formulas
   * have been proved to be satisfiable.
   *
   * @param f The list of formulas on the path.
   * @param pProver The solver.
   * @param elementsOnPath The ARGElements of the paths represented by f.
   * @return Information about the error path, including a satisfying assignment.
   */
  private CounterexampleTraceInfo getErrorPath(List<BooleanFormula> f,
      BasicProverEnvironment<?> pProver, Set<ARGState> elementsOnPath)
      throws CPATransferException, SolverException, InterruptedException {

    // get the branchingFormula
    // this formula contains predicates for all branches we took
    // this way we can figure out which branches make a feasible path
    BooleanFormula branchingFormula = pmgr.buildBranchingFormula(elementsOnPath);

    if (bfmgr.isTrue(branchingFormula)) {
      return CounterexampleTraceInfo.feasible(
          f, pProver.getModelAssignments(), ImmutableMap.<Integer, Boolean>of());
    }

    // add formula to solver environment
    pProver.push(branchingFormula);

    // need to ask solver for satisfiability again,
    // otherwise model doesn't contain new predicates
    boolean stillSatisfiable = !pProver.isUnsat();

    if (stillSatisfiable) {
      List<ValueAssignment> model = pProver.getModelAssignments();
      return CounterexampleTraceInfo.feasible(
          f, model, pmgr.getBranchingPredicateValuesFromModel(model));

    } else {
      // this should not happen
      logger.log(Level.WARNING, "Could not get precise error path information because of inconsistent reachingPathsFormula!");

      dumpInterpolationProblem(f);
      dumpFormulaToFile("formula", branchingFormula, f.size());

      return CounterexampleTraceInfo.feasible(
          f, ImmutableList.<ValueAssignment>of(), ImmutableMap.<Integer, Boolean>of());
    }
  }


  /**
   * Helper method to dump a list of formulas to files.
   */
  private void dumpInterpolationProblem(List<BooleanFormula> f) {
    int k = 0;
    for (BooleanFormula formula : f) {
      dumpFormulaToFile("formula", formula, k++);
    }
  }

  private void dumpFormulaToFile(String name, BooleanFormula f, int i) {
    Path dumpFile = formatFormulaOutputFile(name, i);
    fmgr.dumpFormulaToFile(f, dumpFile);
  }

  private Path formatFormulaOutputFile(String formula, int index) {
    return fmgr.formatFormulaOutputFile("interpolation", cexAnalysisTimer.getNumberOfIntervals(), formula, index);
  }

  /**
   * This class encapsulates the used SMT solver for interpolation,
   * and keeps track of the formulas that are currently on the solver stack.
   *
   * An instance of this class can be used for several interpolation queries
   * in a row, and it will try to keep as many formulas as possible in the
   * SMT solver between those queries (so that the solver may reuse information
   * from previous queries, and hopefully might even return similar interpolants).
   *
   * When an instance won't be used anymore, call {@link #close()}.
   *
   */
  public class Interpolator<T> {

    public InterpolatingProverEnvironment<T> itpProver;
    private final List<Triple<BooleanFormula, AbstractState, T>> currentlyAssertedFormulas = new ArrayList<>();

    Interpolator() {
      itpProver = newEnvironment();
    }

    @SuppressWarnings("unchecked")
    public InterpolatingProverEnvironment<T> newEnvironment() {
      // This is safe because we don't actually care about the value of T,
      // only the InterpolatingProverEnvironment itself cares about it.
      return (InterpolatingProverEnvironment<T>)solver.newProverEnvironmentWithInterpolation();
    }

    /**
     * Counterexample analysis and predicate discovery.
     *
     * @param f the formulas for the path
     * @param elementsOnPath the ARGElements on the path (may be empty if no branching information
     *     is required)
     * @return counterexample info with predicated information
     */
    private CounterexampleTraceInfo buildCounterexampleTrace(
        List<BooleanFormula> f,
        List<AbstractState> pAbstractionStates,
        Set<ARGState> elementsOnPath)
        throws SolverException, CPATransferException, InterruptedException {

      // Check feasibility of counterexample
      shutdownNotifier.shutdownIfNecessary();
      logger.log(Level.FINEST, "Checking feasibility of counterexample trace");
      satCheckTimer.start();

      boolean spurious;

      /* we use two lists, that contain identical formulas
       * with different additional information and (maybe) in different order:
       * 1) formulasWithStatesAndGroupdIds contains elements (F,A,T)
       *      with a path formula F that represents the path until the abstract state A and has the ITP-group T.
       *      It is sorted 'forwards' along the counterexample and is the basis for getting interpolants.
       * 2) orderedFormulas contains elements (F,A,I)
       *      with a path formula F that represents the path until the abstract state A.
       *      It is sorted depending on the {@link CexTraceAnalysisDirection} and
       *      is only used to check the counterexample for satisfiability.
       *      Depending on different directions, different interpolants
       *      might be computed from the solver's proof for unsatisfiability.
       */
      List<Triple<BooleanFormula, AbstractState, T>> formulasWithStatesAndGroupdIds;
      List<Triple<BooleanFormula, AbstractState, Integer>> orderedFormulas;

      if (pAbstractionStates.isEmpty()) {
        pAbstractionStates = new ArrayList<>(Collections.<AbstractState>nCopies(f.size(), null));
      }
      assert pAbstractionStates.size() == f.size() : "each pathFormula must end with an abstract State";

      try {

        if (getUsefulBlocks) {
          f = Collections.unmodifiableList(getUsefulBlocks(f));
        }

        if (dumpInterpolationProblems) {
          dumpInterpolationProblem(f);
        }

        // re-order formulas if needed
        orderedFormulas = orderFormulas(f, pAbstractionStates);
        assert orderedFormulas.size() == f.size();

        // initialize all interpolation group ids with "null"
        formulasWithStatesAndGroupdIds = new ArrayList<>(Collections.<Triple<BooleanFormula, AbstractState, T>>nCopies(f.size(), null));

        // ask solver for satisfiability
        spurious = checkInfeasabilityOfTrace(orderedFormulas, formulasWithStatesAndGroupdIds);
        assert formulasWithStatesAndGroupdIds.size() == f.size();
        assert !formulasWithStatesAndGroupdIds.contains(null); // has to be filled completely

      } finally {
        satCheckTimer.stop();
      }

      logger.log(Level.FINEST, "Counterexample trace is", (spurious ? "infeasible" : "feasible"));


      // Get either interpolants or error path information
      CounterexampleTraceInfo info;
      if (spurious) {

        final List<BooleanFormula> interpolants =
            getInterpolants(this, formulasWithStatesAndGroupdIds);
        if (logger.wouldBeLogged(Level.ALL)) {
          int i = 1;
          for (BooleanFormula itp : interpolants) {
            logger.log(Level.ALL, "For step", i++, "got:", "interpolant", itp);
          }
        }

        info = CounterexampleTraceInfo.infeasible(interpolants);

      } else {
        // this is a real bug
        info = getErrorPath(f, itpProver, elementsOnPath);
      }

      logger.log(Level.ALL, "Counterexample information:", info);

      return info;
    }

    /**
     * Check the satisfiability of a list of formulas, using them in the given order.
     * This method honors the {@link #incrementalCheck} configuration option.
     * It also updates the SMT solver stack and the {@link #currentlyAssertedFormulas}
     * list that is used if {@link #reuseInterpolationEnvironment} is enabled.
     *
     * @param traceFormulas The list of formulas to check, each formula with its index of where it should be added in the list of interpolation groups.
     * @param formulasWithStatesAndGroupdIds The list where to store the references to the interpolation groups. This is just a list of 'identifiers' for the formulas.
     * @return True if the formulas are unsatisfiable.
     */
    private boolean checkInfeasabilityOfTrace(
        final List<Triple<BooleanFormula, AbstractState, Integer>> traceFormulas,
        final List<Triple<BooleanFormula, AbstractState, T>> formulasWithStatesAndGroupdIds)
            throws InterruptedException, SolverException {

      // first identify which formulas are already on the solver stack,
      // which formulas need to be removed from the solver stack,
      // and which formulas need to be added to the solver stack
      ListIterator<Triple<BooleanFormula, AbstractState, Integer>> todoIterator = traceFormulas.listIterator();
      ListIterator<Triple<BooleanFormula, AbstractState, T>> assertedIterator = currentlyAssertedFormulas.listIterator();

      int firstBadIndex = -1; // index of first mis-matching formula in both lists

      while (assertedIterator.hasNext()) {
        Triple<BooleanFormula, AbstractState, T> assertedFormula = assertedIterator.next();

        if (!todoIterator.hasNext()) {
          firstBadIndex = assertedIterator.previousIndex();
          break;
        }

        Triple<BooleanFormula, AbstractState, Integer> todoFormula = todoIterator.next();

        if (todoFormula.getFirst().equals(assertedFormula.getFirst())) {
          // formula is already in solver stack in correct location
          formulasWithStatesAndGroupdIds.set(todoFormula.getThird(), assertedFormula);

        } else {
          firstBadIndex = assertedIterator.previousIndex();
          // rewind iterator by one so that todoFormula will be added to stack
          todoIterator.previous();
          break;
        }
      }

      // now remove the formulas from the solver stack where necessary
      if (firstBadIndex == -1) {
        // solver stack was already empty, nothing do to

      } else if (firstBadIndex == 0) {
        // Create a new environment instead of cleaning up the old one
        // if no formulas need to be reused.
        itpProver.close();
        itpProver = newEnvironment();
        currentlyAssertedFormulas.clear();

      } else {
        assert firstBadIndex > 0;
        // list with all formulas on solver stack that we need to remove
        // (= remaining formulas in currentlyAssertedFormulas list)
        List<Triple<BooleanFormula, AbstractState, T>> toDeleteFormulas =
            currentlyAssertedFormulas.subList(firstBadIndex,
                                              currentlyAssertedFormulas.size());

        // remove formulas from solver stack
        for (int i = 0; i < toDeleteFormulas.size(); i++) {
          itpProver.pop();
        }
        toDeleteFormulas.clear(); // this removes from currentlyAssertedFormulas

        reusedFormulasOnSolverStack += currentlyAssertedFormulas.size();
      }

      boolean isStillFeasible = true;

      // we do only need this unsat call here if we are using the incremental
      // checking option, otherwise it is anyway done later on
      if (incrementalCheck && !currentlyAssertedFormulas.isEmpty()) {
        isStillFeasible = !itpProver.isUnsat();
      }

      // add remaining formulas to the solver stack
      while (todoIterator.hasNext()) {
        Triple<BooleanFormula, AbstractState, Integer> p = todoIterator.next();
        BooleanFormula f = p.getFirst();
        AbstractState state = p.getSecond();
        int index = p.getThird();

        assert formulasWithStatesAndGroupdIds.get(index) == null;
        T itpGroupId = itpProver.push(f);
        final Triple<BooleanFormula, AbstractState, T> assertedFormula = Triple.of(f, state, itpGroupId);
        formulasWithStatesAndGroupdIds.set(index, assertedFormula);
        currentlyAssertedFormulas.add(assertedFormula);


        // We need to iterate through the full loop
        // to add all formulas, but this prevents us from doing further sat checks.
        if (incrementalCheck && isStillFeasible && !bfmgr.isTrue(f)) {
          isStillFeasible = !itpProver.isUnsat();
        }
      }

      assert Iterables.elementsEqual(
          from(traceFormulas).transform(Triple::getFirst),
          from(currentlyAssertedFormulas).transform(Triple::getFirst));

      // we have to do the sat check every time, as it could be that also
      // with incremental checking it was missing (when the path is infeasible
      // and formulas get pushed afterwards)
      return itpProver.isUnsat();
    }

    private void close() {
      itpProver.close();
      itpProver = null;
      currentlyAssertedFormulas.clear();
    }
  }
}
