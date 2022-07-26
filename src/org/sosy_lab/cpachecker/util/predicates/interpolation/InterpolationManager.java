// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.interpolation;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsUtils.div;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.primitives.ImmutableIntArray;
import com.google.common.primitives.Ints;
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
import java.util.stream.IntStream;
import org.checkerframework.checker.nullness.qual.Nullable;
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
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaStrategy.BlockFormulas;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Triple;
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
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;
import org.sosy_lab.java_smt.api.BasicProverEnvironment;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "cpa.predicate.refinement")
public final class InterpolationManager {

  private final Timer cexAnalysisTimer = new Timer();
  private final Timer satCheckTimer = new Timer();
  private final Timer getInterpolantTimer = new Timer();
  private final Timer cexAnalysisGetUsefulBlocksTimer = new Timer();
  private final Timer interpolantVerificationTimer = new Timer();
  private final Timer errorPathCreationTimer = new Timer();
  private int reusedFormulasOnSolverStack = 0;

  public void printStatistics(StatisticsWriter w0) {
    if (cexAnalysisTimer.getNumberOfIntervals() == 0) {
      return;
    }
    w0.put(
        "Counterexample analysis",
        cexAnalysisTimer
            + " (Max: "
            + cexAnalysisTimer.getMaxTime().formatAs(TimeUnit.SECONDS)
            + ", Calls: "
            + cexAnalysisTimer.getNumberOfIntervals()
            + ")");
    StatisticsWriter w1 = w0.beginLevel();
    if (cexAnalysisGetUsefulBlocksTimer.getNumberOfIntervals() > 0) {
      w1.put(
          "Cex.focusing",
          cexAnalysisGetUsefulBlocksTimer
              + " (Max: "
              + cexAnalysisGetUsefulBlocksTimer.getMaxTime().formatAs(TimeUnit.SECONDS)
              + ")");
    }
    w1.put("Refinement sat check", satCheckTimer);
    if (reuseInterpolationEnvironment && satCheckTimer.getNumberOfIntervals() > 0) {
      w1.put(
          "Reused formulas on solver stack",
          reusedFormulasOnSolverStack
              + " (Avg: "
              + div(reusedFormulasOnSolverStack, satCheckTimer.getNumberOfIntervals())
              + ")");
    }
    w1.put("Interpolant computation", getInterpolantTimer);
    if (interpolantVerificationTimer.getNumberOfIntervals() > 0) {
      w1.put("Interpolant verification", interpolantVerificationTimer);
    }
    if (errorPathCreationTimer.getNumberOfIntervals() > 0) {
      w1.put("Error-path creation", errorPathCreationTimer);
    }
  }

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final PathFormulaManager pmgr;
  private final Solver solver;

  private final Interpolator<?> interpolator;

  @Option(
      secure = true,
      description =
          "apply deletion-filter to the abstract counterexample, to get "
              + "a minimal set of blocks, before applying interpolation-based refinement")
  private boolean getUsefulBlocks = false;

  @Option(
      secure = true,
      name = "incrementalCexTraceCheck",
      description =
          "Use incremental search in counterexample analysis to find a minimal infeasible part of"
              + " the trace. This will typically lead to interpolants that refer to this part"
              + " only. The option cexTraceCheckDirection defines in which order the blocks of the"
              + " trace are looked at.")
  private boolean incrementalCheck = true;

  @Option(
      secure = true,
      name = "cexTraceCheckDirection",
      description =
          "Direction for doing counterexample analysis: from start of trace, from end of trace, or"
              + " in more complex patterns. In combination with incrementalCexTraceCheck=true the"
              + " generated interpolants will refer to the minimal infeasible part of the trace"
              + " according to this strategy (e.g., with FORWARDS a minimal infeasible prefix is"
              + " found).")
  private CexTraceAnalysisDirection direction = CexTraceAnalysisDirection.ZIGZAG;

  @Option(
      secure = true,
      description =
          "Strategy how to interact with the intepolating prover. The analysis must support the"
              + " strategy, otherwise the result will be useless!\n"
              + "- SEQ_CPACHECKER: Generate an inductive sequence of interpolants by asking the"
              + " solver individually for each of them. This allows us to fine-tune the queries"
              + " with the option sequentialStrategy and is supported by all solvers.\n"
              + "- SEQ: Generate an inductive sequence of interpolants by asking the solver for"
              + " the whole sequence at once.\n"
              + "- TREE: Use the tree-interpolation feature of the solver to get interpolants.\n"
              + "- TREE_WELLSCOPED: Return each interpolant for i={0..n-1} for the partitions"
              + " A=[lastFunctionEntryIndex..i] and B=[0..lastFunctionEntryIndex-1]+[i+1..n]."
              + " Based on a tree-like scheme.\n"
              + "- TREE_NESTED: Use callstack and previous interpolants for next interpolants (cf."
              + " 'Nested Interpolants').\n"
              + "- TREE_CPACHECKER: similar to TREE_NESTED, but the algorithm is taken from 'Tree"
              + " Interpolation in Vampire'")
  private InterpolationStrategy strategy = InterpolationStrategy.SEQ_CPACHECKER;

  private enum InterpolationStrategy {
    SEQ,
    SEQ_CPACHECKER,
    TREE,
    TREE_WELLSCOPED,
    TREE_NESTED,
    TREE_CPACHECKER,
  }

  @Option(secure = true, description = "dump all interpolation problems")
  private boolean dumpInterpolationProblems = false;

  @Option(
      secure = true,
      description = "verify if the interpolants fulfill the interpolant properties")
  private boolean verifyInterpolants = false;

  @Option(
      secure = true,
      name = "timelimit",
      description =
          "time limit for refinement (use milliseconds or specify a unit; 0 for infinite)")
  @TimeSpanOption(
      codeUnit = TimeUnit.MILLISECONDS,
      defaultUserUnit = TimeUnit.MILLISECONDS,
      min = 0)
  private TimeSpan itpTimeLimit = TimeSpan.ofMillis(0);

  @Option(
      secure = true,
      description =
          "skip refinement if input formula is larger than "
              + "this amount of bytes (ignored if 0)")
  private int maxRefinementSize = 0;

  @Option(
      secure = true,
      description =
          "Use a single SMT solver environment for all interpolation queries and keep formulas"
              + " pushed on solver stack between interpolation queries.")
  // Evaluation showed that it is not useful together with cexTraceCheckDirection=ZIGZAG.
  private boolean reuseInterpolationEnvironment = false;

  @Option(
      secure = true,
      description =
          "After a failed interpolation query, try to solve the formulas again with different"
              + " options instead of giving up immediately.")
  private boolean tryAgainOnInterpolationError = true;

  private final ITPStrategy itpStrategy;

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
      LogManager pLogger)
      throws InvalidConfigurationException {
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

    switch (strategy) {
      case SEQ_CPACHECKER:
        itpStrategy = new SequentialInterpolation(pLogger, pShutdownNotifier, fmgr, config);
        break;
      case SEQ:
        itpStrategy = new SequentialInterpolationWithSolver(pLogger, pShutdownNotifier, fmgr);
        break;
      case TREE_WELLSCOPED:
        itpStrategy = new WellScopedInterpolation(pLogger, pShutdownNotifier, fmgr);
        break;
      case TREE_NESTED:
        itpStrategy = new NestedInterpolation(pLogger, pShutdownNotifier, fmgr);
        break;
      case TREE_CPACHECKER:
        itpStrategy = new TreeInterpolation(pLogger, pShutdownNotifier, fmgr);
        break;
      case TREE:
        itpStrategy = new TreeInterpolationWithSolver(pLogger, pShutdownNotifier, fmgr);
        break;
      default:
        throw new AssertionError("unknown interpolation strategy");
    }
  }

  /**
   * Perform counterexample analysis: solve block formulas and either compute an inductive sequence
   * of interpolants or a precise counterexample depending on whether the formulas are satisfiable.
   * This is the main public method of this class, but one of the other methods can be used if only
   * a part of the features is required.
   *
   * <p>Note that if solving the formulas fails or if interpolants cannot be computed, an exception
   * will be thrown, but if only computing a precise counterexample fails, an imprecise
   * counterexample is returned (no exception is thrown).
   *
   * @param pFormulas the formulas for the path
   * @param pAbstractionStates the abstraction states between the formulas and the last state of the
   *     path. The first state (root) of the path is missing, because it is always TRUE. This is
   *     optionally used for heuristics for getting better interpolants by reordering formulas.
   * @param pImprecisePath A (potentially wrong) ARG path that ends in the target state and is used
   *     as base for computing the precise path. If no path is passed, no attempt at computing a
   *     precise path is made and one can likely call {@link #interpolate(List, List)} instead.
   */
  public CounterexampleTraceInfo buildCounterexampleTrace(
      final BlockFormulas pFormulas,
      final List<AbstractState> pAbstractionStates,
      final Optional<ARGPath> pImprecisePath)
      throws CPAException, InterruptedException {
    assert pAbstractionStates.isEmpty() || pFormulas.getSize() == pAbstractionStates.size();

    return callWithTimelimit(
        () -> buildCounterexampleTrace0(pFormulas, pAbstractionStates, pImprecisePath));
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

  /**
   * Compute an inductive sequence of interpolants for a given list of formulas. The conjunction of
   * the formulas is first checked for satisfiability, it is not necessary to do this before calling
   * this method. Note that if the formulas represent a potential error path, calling {@link
   * #buildCounterexampleTrace(BlockFormulas, List, Optional)} instead and passing an {@link
   * ARGPath} instance would give additional information in case the path is feasible. If
   * abstraction states are available, please call {@link #interpolate(List, List)} to enable more
   * heuristics.
   *
   * @return <code>Optional.empty()</code> if the conjunction of the given formulas is satisfiable,
   *     interpolants otherwise (as list with n-1 elements)
   * @throws CPAException If solving or computing interpolants fails.
   */
  public Optional<ImmutableList<BooleanFormula>> interpolate(final List<BooleanFormula> pFormulas)
      throws CPAException, InterruptedException {
    return interpolate(pFormulas, ImmutableList.of());
  }

  /**
   * Compute an inductive sequence of interpolants for a given list of formulas. The conjunction of
   * the formulas is first checked for satisfiability, it is not necessary to do this before calling
   * this method. Note that if the formulas represent a potential error path, calling {@link
   * #buildCounterexampleTrace(BlockFormulas, List, Optional)} instead and passing an {@link
   * ARGPath} instance would give additional information in case the path is feasible.
   *
   * @param pAbstractionStates the abstraction states between the formulas and the last state of the
   *     path. The first state (root) of the path is missing, because it is always TRUE. This is
   *     optionally used for heuristics for getting better interpolants by reordering formulas.
   * @return <code>Optional.empty()</code> if the conjunction of the given formulas is satisfiable,
   *     interpolants otherwise (as list with n-1 elements)
   * @throws CPAException If solving or computing interpolants fails.
   */
  public Optional<ImmutableList<BooleanFormula>> interpolate(
      final List<BooleanFormula> pFormulas, final List<AbstractState> pAbstractionStates)
      throws CPAException, InterruptedException {
    CounterexampleTraceInfo cexInfo =
        buildCounterexampleTrace(
            new BlockFormulas(pFormulas), pAbstractionStates, Optional.empty());
    if (cexInfo.isSpurious()) {
      return Optional.of(cexInfo.getInterpolants());
    } else {
      return Optional.empty();
    }
  }

  private CounterexampleTraceInfo buildCounterexampleTrace0(
      final BlockFormulas pFormulas,
      final List<AbstractState> pAbstractionStates,
      final Optional<ARGPath> imprecisePath)
      throws RefinementFailedException, InterruptedException {

    cexAnalysisTimer.start();
    try {
      final BlockFormulas f = prepareCounterexampleFormulas(pFormulas);

      final Interpolator<?> currentInterpolator;
      if (reuseInterpolationEnvironment) {
        currentInterpolator = checkNotNull(interpolator);
      } else {
        currentInterpolator = new Interpolator<>();
      }

      try {
        try {
          return currentInterpolator.buildCounterexampleTrace(f, pAbstractionStates, imprecisePath);
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
        return fallbackWithoutInterpolation(f, imprecisePath, itpException);
      }

    } finally {
      cexAnalysisTimer.stop();
    }
  }

  /**
   * Perform counterexample analysis: solve block formulas and compute a precise counterexample if
   * possible, but do not bother computing interpolants. This method exists even though it does not
   * really fit into a class named {@link InterpolationManager} because it is useful for refiners
   * that want the same kind of counterexample analysis as {@link
   * #buildCounterexampleTrace(BlockFormulas, List, Optional)} provides, but do not need
   * interpolants, and it is easy to reuse the code here because we need it anyway as fallback if
   * interpolation fails.
   *
   * <p>Note that if solving the formulas fails, an exception will be thrown, but if only computing
   * a precise counterexample fails, an imprecise counterexample is returned (no exception is
   * thrown).
   *
   * @param pFormulas the formulas for the path
   * @param imprecisePath A (potentially wrong) ARG path that ends in the target state and is used
   *     as base for computing the precise path.
   */
  public CounterexampleTraceInfo buildCounterexampleTraceWithoutInterpolation(
      final BlockFormulas pFormulas, Optional<ARGPath> imprecisePath)
      throws CPAException, InterruptedException {

    return callWithTimelimit(
        () -> buildCounterexampleTraceWithoutInterpolation0(pFormulas, imprecisePath));
  }

  private CounterexampleTraceInfo buildCounterexampleTraceWithoutInterpolation0(
      final BlockFormulas pFormulas, Optional<ARGPath> imprecisePath)
      throws CPAException, InterruptedException {

    cexAnalysisTimer.start();
    try {
      final BlockFormulas f = prepareCounterexampleFormulas(pFormulas);

      try {
        return solveCounterexample(f, imprecisePath);
      } catch (SolverException e) {
        throw new RefinementFailedException(Reason.InterpolationFailed, null, e);
      }

    } finally {
      cexAnalysisTimer.stop();
    }
  }

  /** Prepare the list of formulas for a counterexample for the solving/interpolation step. */
  private BlockFormulas prepareCounterexampleFormulas(final BlockFormulas pFormulas)
      throws RefinementFailedException {
    logger.log(Level.FINEST, "Building counterexample trace");

    // Final adjustments to the list of formulas
    ImmutableList<BooleanFormula> f = pFormulas.getFormulas();

    if (fmgr.useBitwiseAxioms()) {
      f = addBitwiseAxioms(f);
    }

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
    return new BlockFormulas(f, pFormulas.getBranchingFormulas());
  }

  /**
   * Attempt to check feasibility of the current counterexample without interpolation in case of a
   * failure with interpolation. Maybe the solver can handle the formulas if we do not attempt to
   * interpolate (this happens for example for MathSAT).
   */
  private CounterexampleTraceInfo fallbackWithoutInterpolation(
      BlockFormulas f, Optional<ARGPath> imprecisePath, SolverException itpException)
      throws InterruptedException, RefinementFailedException {
    try {
      CounterexampleTraceInfo counterexample = solveCounterexample(f, imprecisePath);
      if (!counterexample.isSpurious()) {
        return counterexample;
      }
    } catch (SolverException solvingException) {
      // in case of exception throw original one below but do not forget e2
      itpException.addSuppressed(solvingException);
    }
    throw new RefinementFailedException(Reason.InterpolationFailed, null, itpException);
  }

  /**
   * Analyze a counterexample for feasibility without computing interpolants.
   *
   * @throws SolverException If the solver fails to solve the formulas. (If it solves successfully
   *     but fails to compute a model, an imprecise counterexample is returned instead.)
   */
  private CounterexampleTraceInfo solveCounterexample(
      BlockFormulas f, Optional<ARGPath> imprecisePath)
      throws SolverException, InterruptedException {

    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      final boolean isSat;

      satCheckTimer.start();
      try {
        for (BooleanFormula block : f.getFormulas()) {
          prover.push(block);
        }
        isSat = !prover.isUnsat();
      } finally {
        satCheckTimer.stop();
      }

      if (isSat) {
        try {
          return getErrorPath(f, prover, imprecisePath);
        } catch (SolverException modelException) {
          logger.logUserException(
              Level.WARNING, modelException, "Could not create model for error path!");
          return CounterexampleTraceInfo.feasibleImprecise(f.getFormulas());
        }
      } else {
        return CounterexampleTraceInfo.infeasibleNoItp();
      }
    }
  }

  /**
   * Add axioms about bitwise operations to a list of formulas, if such operations are used. This is
   * probably not that helpful currently, we would have to the tell the solver that these are
   * axioms.
   *
   * <p>The axioms are added to the last part of the list of formulas.
   *
   * @param f The list of formulas to scan for bitwise operations.
   */
  private ImmutableList<BooleanFormula> addBitwiseAxioms(ImmutableList<BooleanFormula> f) {
    BooleanFormula bitwiseAxioms = bfmgr.makeTrue();

    for (BooleanFormula fm : f) {
      BooleanFormula a = fmgr.getBitwiseAxioms(fm);
      if (!bfmgr.isTrue(a)) {
        bitwiseAxioms = fmgr.getBooleanFormulaManager().and(bitwiseAxioms, a);
      }
    }

    if (!bfmgr.isTrue(bitwiseAxioms)) {
      logger.log(
          Level.ALL, "DEBUG_3", "ADDING BITWISE AXIOMS TO THE", "LAST GROUP: ", bitwiseAxioms);
      return ImmutableList.<BooleanFormula>builderWithExpectedSize(f.size())
          .addAll(f.subList(0, f.size() - 1))
          .add(bfmgr.and(f.get(f.size() - 1), bitwiseAxioms))
          .build();
    }

    return f;
  }

  /**
   * Try to find out which formulas out of a list of formulas are relevant for making the
   * conjunction unsatisfiable. This method honors the {@link #direction} configuration option.
   *
   * @param f The list of formulas to check.
   * @return A sublist of f that contains the useful formulas.
   */
  private List<BooleanFormula> getUsefulBlocks(List<BooleanFormula> f)
      throws SolverException, InterruptedException {

    cexAnalysisGetUsefulBlocksTimer.start();

    // try to find a minimal-unsatisfiable-core of the trace (as Blast does)

    try (ProverEnvironment thmProver = solver.newProverEnvironment()) {

      logger.log(Level.ALL, "DEBUG_1", "Calling getUsefulBlocks on path", "of length:", f.size());

      final BooleanFormula[] needed = new BooleanFormula[f.size()];
      for (int i = 0; i < needed.length; ++i) {
        needed[i] = bfmgr.makeTrue();
      }
      final boolean backwards = direction == CexTraceAnalysisDirection.BACKWARDS;
      final int start = backwards ? f.size() - 1 : 0;
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
          int e = f.size() - 1;
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
              logger.log(Level.ALL, "DEBUG_1", "Found needed block: ", i, ", term: ", t);
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
          for (int i = start; backwards ? i >= 0 : i < f.size(); i += increment) {
            BooleanFormula t = f.get(i);
            thmProver.push(t);
            ++toPop;
            if (thmProver.isUnsat()) {
              // add this block to the needed ones, and repeat
              needed[i] = t;
              logger.log(Level.ALL, "DEBUG_1", "Found needed block: ", i, ", term: ", t);
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
   * Put the list of formulas into the order in which they should be given to the solver, as defined
   * by the {@link #direction} configuration option.
   *
   * @param traceFormulas The list of formulas to check.
   * @return The same list of formulas in different order, and each formula has its position in the
   *     original list as third element of the pair.
   */
  private ImmutableIntArray orderFormulas(
      final List<BooleanFormula> traceFormulas, final List<AbstractState> pAbstractionStates) {

    // In this list are all formulas together with their position in the original list
    ImmutableIntArray result =
        direction.orderFormulas(
            traceFormulas, pAbstractionStates, variableClassification, loopStructure, fmgr);
    assert Iterators.elementsEqual(
            result.stream().sorted().boxed().iterator(),
            IntStream.range(0, traceFormulas.size()).boxed().iterator())
        : "Not a valid permutation: " + result;
    return result;
  }

  /**
   * Get the interpolants from the solver after the formulas have been proved to be unsatisfiable.
   *
   * @param pInterpolator The references to the interpolation groups, sorting depends on the
   *     solver-stack.
   * @param formulasWithStatesAndGroupdIds list of (F,A,T) of path formulae F with their
   *     interpolation group I and the abstract state A, where a path formula F is the path
   *     before/until the abstract state A. The list is sorted in the "correct" order along the
   *     counterexample.
   * @return A list of (N-1) interpolants for N formulae.
   */
  private <T> List<BooleanFormula> getInterpolants(
      Interpolator<T> pInterpolator,
      List<Triple<BooleanFormula, AbstractState, T>> formulasWithStatesAndGroupdIds)
      throws SolverException, InterruptedException {

    final List<BooleanFormula> interpolants;
    try {
      getInterpolantTimer.start();
      interpolants = itpStrategy.getInterpolants(pInterpolator, formulasWithStatesAndGroupdIds);
    } finally {
      getInterpolantTimer.stop();
    }

    assert formulasWithStatesAndGroupdIds.size() - 1 == interpolants.size()
        : "we should return N-1 interpolants for N formulas.";

    if (verifyInterpolants) {
      try {
        interpolantVerificationTimer.start();
        itpStrategy.checkInterpolants(solver, formulasWithStatesAndGroupdIds, interpolants);
      } finally {
        interpolantVerificationTimer.stop();
      }
    }
    return interpolants;
  }

  /**
   * Get information about the error path from the solver after the formulas have been proved to be
   * satisfiable.
   *
   * @param formulas The list of formulas on the path.
   * @param pProver The solver.
   * @param pImprecisePath A optional (potentially infeasible) path to the target state. If given,
   *     the model of the prover environment is used to determine a feasible path through the ARG to
   *     the same target state.
   * @return Information about the error path, including a satisfying assignment.
   * @throws SolverException If the solver fails to produce a model.
   */
  private CounterexampleTraceInfo getErrorPath(
      BlockFormulas formulas, BasicProverEnvironment<?> pProver, Optional<ARGPath> pImprecisePath)
      throws SolverException, InterruptedException {

    if (pImprecisePath.isEmpty()) {
      return CounterexampleTraceInfo.feasibleImprecise(formulas.getFormulas());
    }

    errorPathCreationTimer.start();
    try {
      ARGPath imprecisePath = pImprecisePath.orElseThrow();
      Set<ARGState> pathElements = ARGUtils.getAllStatesOnPathsTo(imprecisePath.getLastState());
      try (Model model = pProver.getModel()) {
        ARGPath precisePath =
            pmgr.getARGPathFromModel(
                model,
                imprecisePath.getFirstState(),
                pathElements::contains,
                formulas.getBranchingFormulas());

        if (!precisePath.getLastState().equals(imprecisePath.getLastState())) {
          logger.log(
              Level.WARNING,
              "Could not create error path: ARG target path reached the wrong state!");
          return CounterexampleTraceInfo.feasibleImprecise(formulas.getFormulas());
        }

        return CounterexampleTraceInfo.feasible(formulas.getFormulas(), precisePath);
      } catch (IllegalArgumentException | CPATransferException e) {
        // Note that we do not catch SolverException here, although we could in principle also
        // return an imprecise counterexample in this case. Instead we let the exception propagate
        // because the caller might want to fall back to fallbackWithoutInterpolation().
        logger.logUserException(Level.WARNING, e, "Could not create error path");
        return CounterexampleTraceInfo.feasibleImprecise(formulas.getFormulas());
      }
    } finally {
      errorPathCreationTimer.stop();
    }
  }

  /** Helper method to dump a list of formulas to files. */
  private void dumpInterpolationProblem(List<BooleanFormula> f) {
    int k = 0;
    for (BooleanFormula formula : f) {
      dumpFormulaToFile("formula", formula, k++);
    }
  }

  private void dumpFormulaToFile(String name, BooleanFormula f, int i) {
    @Nullable Path dumpFile = formatFormulaOutputFile(name, i);
    fmgr.dumpFormulaToFile(f, dumpFile);
  }

  private @Nullable Path formatFormulaOutputFile(String formula, int index) {
    return fmgr.formatFormulaOutputFile(
        "interpolation", cexAnalysisTimer.getNumberOfIntervals(), formula, index);
  }

  /**
   * This class encapsulates the used SMT solver for interpolation, and keeps track of the formulas
   * that are currently on the solver stack.
   *
   * <p>An instance of this class can be used for several interpolation queries in a row, and it
   * will try to keep as many formulas as possible in the SMT solver between those queries (so that
   * the solver may reuse information from previous queries, and hopefully might even return similar
   * interpolants).
   *
   * <p>When an instance won't be used anymore, call {@link #close()}.
   */
  public class Interpolator<T> {

    public InterpolatingProverEnvironment<T> itpProver;
    private final List<Pair<BooleanFormula, T>> currentlyAssertedFormulas = new ArrayList<>();

    Interpolator() {
      itpProver = newEnvironment();
    }

    @SuppressWarnings("unchecked")
    public InterpolatingProverEnvironment<T> newEnvironment() {
      // This is safe because we don't actually care about the value of T,
      // only the InterpolatingProverEnvironment itself cares about it.
      return (InterpolatingProverEnvironment<T>)
          solver.newProverEnvironmentWithInterpolation(ProverOptions.GENERATE_MODELS);
    }

    /**
     * Counterexample analysis and predicate discovery.
     *
     * @param formulas the formulas for the path
     * @return counterexample info with predicated information
     */
    private CounterexampleTraceInfo buildCounterexampleTrace(
        BlockFormulas formulas,
        List<AbstractState> pAbstractionStates,
        Optional<ARGPath> imprecisePath)
        throws SolverException, InterruptedException {

      // Check feasibility of counterexample
      shutdownNotifier.shutdownIfNecessary();
      logger.log(Level.FINEST, "Checking feasibility of counterexample trace");
      satCheckTimer.start();

      boolean spurious;

      if (pAbstractionStates.isEmpty()) {
        pAbstractionStates = new ArrayList<>(Collections.nCopies(formulas.getSize(), null));
      }
      assert pAbstractionStates.size() == formulas.getSize()
          : "each pathFormula must end with an abstract State";

      // This list will contain tuples (formula, abstraction state, interpolation-group id)
      // for every block of the counterexample (in correct order).
      // It is initialized with {@code null} and filled while the formulas are pushed on the solver
      // stack.
      final List<Triple<BooleanFormula, AbstractState, T>> formulasWithStatesAndGroupdIds =
          new ArrayList<>(Collections.nCopies(formulas.getSize(), null));

      ImmutableIntArray formulaPermutation;
      try {

        if (getUsefulBlocks) {
          formulas =
              new BlockFormulas(
                  getUsefulBlocks(formulas.getFormulas()), formulas.getBranchingFormulas());
        }

        if (dumpInterpolationProblems) {
          dumpInterpolationProblem(formulas.getFormulas());
        }

        // compute permutation in which formulas should be added
        formulaPermutation = orderFormulas(formulas.getFormulas(), pAbstractionStates);
        logger.log(Level.ALL, "Permutation of blocks for interpolation query:", formulaPermutation);

        // ask solver for satisfiability
        spurious =
            checkInfeasabilityOfTrace(
                formulas.getFormulas(),
                pAbstractionStates,
                formulaPermutation,
                formulasWithStatesAndGroupdIds);
        assert Lists.transform(formulasWithStatesAndGroupdIds, Triple::getFirst)
            .equals(formulas.getFormulas());

      } finally {
        satCheckTimer.stop();
      }

      logger.log(Level.FINEST, "Counterexample trace is", (spurious ? "infeasible" : "feasible"));

      // Get either interpolants or error path information
      CounterexampleTraceInfo info;
      if (spurious) {

        List<BooleanFormula> interpolants;
        try {
          interpolants = getInterpolants(this, formulasWithStatesAndGroupdIds);
        } catch (SolverException e) {
          if (!tryAgainOnInterpolationError) {
            throw e;
          }
          // Interpolation failed. There are at least some tasks where MathSAT succeeds if we try
          // again with different options. We invert incrementalCheck and we reverse the formula
          // order. Note that a new solver environment will be created automatically, because due to
          // the reversed order there will be no change of reusing something on the solver stack,
          // and cleanupSolverStack creates a new environment in this case.
          logger.logfDebugException(
              e,
              "Interpolation failed, trying again in reverse order and with%s incremental check.",
              incrementalCheck ? "out" : "");
          Collections.fill(formulasWithStatesAndGroupdIds, null); // reset state

          int[] permutation = formulaPermutation.toArray();
          Ints.reverse(permutation);
          formulaPermutation = ImmutableIntArray.copyOf(permutation); // reverse permutation

          final boolean originalIncrementalCheck = incrementalCheck;
          try {
            // A little bit ugly to invert the field, but we reset in finally and this at least
            // makes sure that it will definitely be used in all relevant places.
            incrementalCheck = !incrementalCheck; // invert incrementalCheck

            satCheckTimer.start();
            spurious = // solve again
                checkInfeasabilityOfTrace(
                    formulas.getFormulas(),
                    pAbstractionStates,
                    formulaPermutation,
                    formulasWithStatesAndGroupdIds);
            assert Lists.transform(formulasWithStatesAndGroupdIds, Triple::getFirst)
                .equals(formulas.getFormulas());
            verify(spurious, "Counterexample formulas became satisfiable on second try");

          } finally {
            incrementalCheck = originalIncrementalCheck;
            satCheckTimer.stop();
          }

          interpolants = getInterpolants(this, formulasWithStatesAndGroupdIds);
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
        info = getErrorPath(formulas, itpProver, imprecisePath);
      }

      logger.log(Level.ALL, "Counterexample information:", info);

      return info;
    }

    /**
     * Check the satisfiability of a list of formulas, using them in the given order. This method
     * honors the {@link #incrementalCheck} configuration option. It also updates the SMT solver
     * stack and the {@link #currentlyAssertedFormulas} list that is used if {@link
     * #reuseInterpolationEnvironment} is enabled.
     *
     * @param formulaPermutation The order (by indices) in which the formulas should be checked.
     * @param formulasWithStatesAndGroupdIds The list where to store the references (in original
     *     order) to the interpolation groups. This is just a list of 'identifiers' for the
     *     formulas.
     * @return True if the formulas are unsatisfiable.
     */
    private boolean checkInfeasabilityOfTrace(
        final List<BooleanFormula> traceFormulas,
        final List<AbstractState> traceStates,
        final ImmutableIntArray formulaPermutation,
        final List<Triple<BooleanFormula, AbstractState, T>> formulasWithStatesAndGroupdIds)
        throws InterruptedException, SolverException {

      // first identify which formulas are already on the solver stack,
      // which formulas need to be removed from the solver stack,
      // and which formulas need to be added to the solver stack
      ListIterator<Integer> todoIterator = formulaPermutation.asList().listIterator();
      int firstBadIndex =
          getIndexOfFirstNonReusableFormula(
              traceFormulas, traceStates, todoIterator, formulasWithStatesAndGroupdIds);

      // now remove the formulas from the solver stack where necessary
      cleanupSolverStack(firstBadIndex);

      // push new formulas onto the solver stack
      addNewFormulasToStack(
          traceFormulas, traceStates, todoIterator, formulasWithStatesAndGroupdIds);

      assert ImmutableMultiset.copyOf(traceFormulas)
          .equals(from(currentlyAssertedFormulas).transform(Pair::getFirst).toMultiset());

      // we have to do the sat check every time, as it could be that also
      // with incremental checking it was missing (when the path is infeasible
      // and formulas get pushed afterwards)
      return itpProver.isUnsat();
    }

    /**
     * For optimization we try to share the solver stack between different solver calls. Before
     * pushing a new set of formulas, we need to determine all old formulas that need to be popped
     * from the solver stack.
     *
     * @param formulasWithStatesAndGroupdIds the new sorted collection of formulas, with indizes
     * @param todoIterator iterator producing the indices of formulas to be used, afterwards it is
     *     positioned at the new starting point
     */
    private int getIndexOfFirstNonReusableFormula(
        final List<BooleanFormula> traceFormulas,
        final List<AbstractState> traceStates,
        final ListIterator<Integer> todoIterator,
        final List<Triple<BooleanFormula, AbstractState, T>> formulasWithStatesAndGroupdIds) {

      ListIterator<Pair<BooleanFormula, T>> assertedIterator =
          currentlyAssertedFormulas.listIterator();
      int firstBadIndex = -1; // index of first mis-matching formula in both lists

      while (assertedIterator.hasNext()) {
        Pair<BooleanFormula, T> assertedFormula = assertedIterator.next();

        if (!todoIterator.hasNext()) {
          firstBadIndex = assertedIterator.previousIndex();
          break;
        }

        int index = todoIterator.next();
        BooleanFormula todoFormula = traceFormulas.get(index);

        if (todoFormula.equals(assertedFormula.getFirst())) {
          // formula is already in solver stack in correct location
          final Triple<BooleanFormula, AbstractState, T> assertedFormulaWithState =
              Triple.of(
                  assertedFormula.getFirst(), traceStates.get(index), assertedFormula.getSecond());
          formulasWithStatesAndGroupdIds.set(index, assertedFormulaWithState);

        } else {
          firstBadIndex = assertedIterator.previousIndex();
          // rewind iterator by one so that todoFormula will be added to stack
          todoIterator.previous();
          break;
        }
      }
      return firstBadIndex;
    }

    /**
     * Remove some old formulas from the solvers stack.
     *
     * @param firstBadIndex index of level from where to remove all old formulas
     */
    private void cleanupSolverStack(int firstBadIndex) {
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
        List<Pair<BooleanFormula, T>> toDeleteFormulas =
            currentlyAssertedFormulas.subList(firstBadIndex, currentlyAssertedFormulas.size());

        // remove formulas from solver stack
        for (int i = 0; i < toDeleteFormulas.size(); i++) {
          itpProver.pop();
        }
        toDeleteFormulas.clear(); // this removes from currentlyAssertedFormulas

        reusedFormulasOnSolverStack += currentlyAssertedFormulas.size();
      }
    }

    /**
     * Push all new formulas onto the solver stack. If some of them were already pushed earlier,
     * ignore them.
     *
     * @param formulasWithStatesAndGroupdIds the new sorted collection of formulas, with indizes
     * @param todoIterator iterator producing the indices of formulas to be used
     */
    private void addNewFormulasToStack(
        final List<BooleanFormula> traceFormulas,
        final List<AbstractState> traceStates,
        final ListIterator<Integer> todoIterator,
        final List<Triple<BooleanFormula, AbstractState, T>> formulasWithStatesAndGroupdIds)
        throws SolverException, InterruptedException {
      boolean isStillFeasible = true;

      // we do only need this unsat call here if we are using the incremental
      // checking option, otherwise it is anyway done later on
      if (incrementalCheck && !currentlyAssertedFormulas.isEmpty()) {
        isStillFeasible = !itpProver.isUnsat();
      }

      // add remaining formulas to the solver stack
      while (todoIterator.hasNext()) {
        int index = todoIterator.next();
        BooleanFormula f = traceFormulas.get(index);
        AbstractState state = traceStates.get(index);

        assert formulasWithStatesAndGroupdIds.get(index) == null;
        logger.logf(Level.ALL, "Pushing formula for block %s:\n  %s", index, f);
        T itpGroupId = itpProver.push(f);
        formulasWithStatesAndGroupdIds.set(index, Triple.of(f, state, itpGroupId));
        currentlyAssertedFormulas.add(Pair.of(f, itpGroupId));

        // We need to iterate through the full loop
        // to add all formulas, but this prevents us from doing further sat checks.
        if (incrementalCheck && isStillFeasible && !bfmgr.isTrue(f)) {
          isStillFeasible = !itpProver.isUnsat();
        }
      }
    }

    private void close() {
      itpProver.close();
      itpProver = null;
      currentlyAssertedFormulas.clear();
    }
  }
}
