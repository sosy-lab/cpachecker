// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.induction.refiner;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.getBlockFormula;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.getPredicateState;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.induction.InductionCPA;
import org.sosy_lab.cpachecker.cpa.induction.InductionPrecision;
import org.sosy_lab.cpachecker.cpa.induction.InductionPrecision.LoopPrecision;
import org.sosy_lab.cpachecker.cpa.induction.InductionState;
import org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaStrategy.BlockFormulas;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * The common part of the refiners of the PredK framework. The refiner gets everything the
 * exploration reached and checks the targets and cut-offs in batches of SAT queries, as the native
 * algorithms do. Only if neither a counterexample is found nor the states are unreachable, the
 * concrete refiner changes the precision, always lazily.
 */
public abstract class InductionRefiner implements Refiner, StatisticsProvider, Statistics {

  protected final LogManager logger;
  protected final PredicateCPA predicateCpa;
  protected final Solver solver;
  protected final FormulaManagerView fmgr;
  protected final BooleanFormulaManagerView bfmgr;
  protected final InterpolationManager interpolationManager;
  private final ARGCPA argCpa;
  private final PathChecker pathChecker;

  /** One prover for all batch queries, as native BMC uses one for all its queries. */
  private @Nullable ProverEnvironment prover = null;

  /** One prover for all step case queries, if they share it. */
  private @Nullable ProverEnvironment stepProver = null;

  private int refinementNumber = 0;

  private final StatTimer refinementTime = new StatTimer("Time for refinements");
  private final StatTimer satCheckTime = new StatTimer("Time for batch SAT checks");
  private final StatCounter removedStates = new StatCounter("Unreachable states removed");
  private final StatCounter interpolationFailures = new StatCounter("Failed interpolations");
  private final StatCounter reexploredSubtrees = new StatCounter("Re-explored subtrees");
  private final StatInt maxK = new StatInt(StatKind.MAX, "Maximal k");

  protected InductionRefiner(
      ConfigurableProgramAnalysis pCpa, LogManager pLogger, Configuration pConfig)
      throws InvalidConfigurationException {
    argCpa = CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, getClass());
    predicateCpa = CPAs.retrieveCPAOrFail(pCpa, PredicateCPA.class, getClass());
    CPAs.retrieveCPAOrFail(pCpa, InductionCPA.class, getClass());
    logger = pLogger;
    solver = predicateCpa.getSolver();
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    CFA cfa = predicateCpa.getCfa();
    interpolationManager =
        new InterpolationManager(
            predicateCpa.getPathFormulaManager(),
            solver,
            cfa.getLoopStructure(),
            cfa.getVarClassification(),
            pConfig,
            predicateCpa.getShutdownNotifier(),
            logger,
            /* pEnableCounterexampleAnalysis= */ true);
    pathChecker =
        new PathChecker(
            pConfig,
            logger,
            predicateCpa.getShutdownNotifier(),
            cfa.getMachineModel(),
            predicateCpa.getPathFormulaManager(),
            solver);
  }

  /** The configuration of the analysis, which the factory of a refiner does not get. */
  protected static Configuration configurationOf(
      ConfigurableProgramAnalysis pCpa, Class<?> pRefiner) throws InvalidConfigurationException {
    return CPAs.retrieveCPAOrFail(pCpa, PredicateCPA.class, pRefiner).getConfiguration();
  }

  /**
   * Check the targets and cut-offs of the exploration, remove the unreachable ones and change the
   * precision lazily for the others.
   *
   * @return a target that is reachable concretely, if there is one
   * @throws RefinementFailedException if the refinement cannot change the ARG
   */
  protected abstract Optional<ARGState> refine(
      ARGReachedSet pReached, List<ARGState> pTargets, List<ARGState> pCutOffs)
      throws CPAException, InterruptedException, SolverException;

  @Override
  public final boolean performRefinement(ReachedSet pReached)
      throws CPAException, InterruptedException {
    refinementTime.start();
    try {
      ARGReachedSet reached = new ARGReachedSet(pReached, argCpa, refinementNumber++);
      List<ARGState> targets = new ArrayList<>();
      List<ARGState> cutOffs = new ArrayList<>();
      for (ARGState state : from(pReached).filter(ARGState.class).filter(ARGState::isTarget)) {
        (isPropertyViolation(state) ? targets : cutOffs).add(state);
      }

      Optional<ARGState> counterexample = refine(reached, targets, cutOffs);
      if (counterexample.isPresent()) {
        // cut-offs are targets only for the refinement and violate no property
        removeUnreachable(reached, cutOffs);
        reportCounterexample(counterexample.orElseThrow());
        return false;
      }
      return true;

    } catch (SolverException e) {
      throw new CPAException("Solver failure during refinement", e);
    } finally {
      refinementTime.stop();
    }
  }

  /** Whether the state violates the property, in contrast to a cut-off of the InductionCPA. */
  protected static boolean isPropertyViolation(ARGState pState) {
    CompositeState composite = AbstractStates.extractStateByType(pState, CompositeState.class);
    return from(composite.getWrappedStates())
        .anyMatch(s -> !(s instanceof InductionState) && AbstractStates.isTargetState(s));
  }

  /**
   * The abstraction states on one path from the root of the ARG, which is the first of them, to the
   * given state. The path follows the first parent of every state, which is cheaper than an {@link
   * ARGPath}. Merges happen only inside blocks, so every path has the same abstractions.
   */
  protected static ImmutableList<ARGState> abstractionChain(ARGState pState) {
    List<ARGState> chain = new ArrayList<>();
    ARGState state = pState;
    while (true) {
      if (getPredicateState(state).isAbstractionState()) {
        chain.add(state);
      }
      if (state.getParents().isEmpty()) {
        return ImmutableList.copyOf(Lists.reverse(chain));
      }
      state = state.getParents().getFirst();
    }
  }

  /** The block formulas of the abstraction states of the chain after the root. */
  protected static ImmutableList<BooleanFormula> blockFormulas(List<ARGState> pChain) {
    return from(pChain).skip(1).transform(s -> getBlockFormula(getPredicateState(s))).toList();
  }

  /** The formula of all paths from the root to the given abstraction state. */
  protected BooleanFormula concretePathFormula(ARGState pAbstractionState) {
    return bfmgr.and(blockFormulas(abstractionChain(pAbstractionState)));
  }

  /**
   * The formula of the block that ends in the given abstraction state, starting in the region of
   * the abstraction before it.
   */
  protected BooleanFormula lastBlockFromRegion(ARGState pAbstractionState) {
    List<ARGState> chain = abstractionChain(pAbstractionState);
    ARGState previous = chain.get(chain.size() - 2);
    return bfmgr.and(
        getPredicateState(previous).getAbstractionFormula().asInstantiatedFormula(),
        getBlockFormula(getPredicateState(pAbstractionState)));
  }

  /** The interpolants of the formulas, or nothing if they are satisfiable or the solver fails. */
  protected Optional<ImmutableList<BooleanFormula>> interpolate(List<BooleanFormula> pFormulas)
      throws CPAException, InterruptedException {
    try {
      return interpolationManager.interpolate(pFormulas);
    } catch (RefinementFailedException | IllegalArgumentException e) {
      // MathSAT reports some failures, e.g., in non-linear arithmetic, as IllegalArgumentException
      logger.logDebugException(e, "Interpolation failed");
      interpolationFailures.inc();
      return Optional.empty();
    }
  }

  /**
   * Whether the concrete path formula of the given state describes a real path. After a restart of
   * the callstack in an induction step, a path can return to another caller, which the path formula
   * does not show, so then every return on the path must match its call.
   */
  protected static boolean isRealPath(ARGState pState) {
    boolean restartsCallstack =
        from(abstractionChain(pState))
            .transform(InductionRefiner::inductionStateOf)
            .anyMatch(s -> s.isAbstraction() && s.isInInductionStep());
    if (!restartsCallstack) {
      return true;
    }
    Deque<CFANode> callSites = new ArrayDeque<>();
    for (CFAEdge edge : ARGUtils.getOnePathTo(pState).getFullPath()) {
      if (edge instanceof FunctionCallEdge) {
        callSites.push(edge.getPredecessor());
      } else if (edge instanceof FunctionReturnEdge returnEdge) {
        CFANode callSite = returnEdge.getSummaryEdge().getPredecessor();
        if (callSites.isEmpty() || !callSites.pop().equals(callSite)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * A target that is reachable concretely, found with one SAT query for all targets. Paths whose
   * returns do not match their calls are ignored.
   */
  protected Optional<ARGState> findFeasibleTarget(Collection<ARGState> pTargets)
      throws SolverException, InterruptedException {
    Map<ARGState, BooleanFormula> pathFormulas = new LinkedHashMap<>();
    for (ARGState target : pTargets) {
      if (isRealPath(target)) {
        pathFormulas.put(target, concretePathFormula(target));
      }
    }
    if (pathFormulas.isEmpty()) {
      return Optional.empty();
    }

    satCheckTime.start();
    try {
      if (!sharesBatchProver()) {
        try (ProverEnvironment ownProver =
            solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
          ownProver.push(bfmgr.or(pathFormulas.values()));
          return findFeasibleTarget(ownProver, pathFormulas);
        }
      }
      ProverEnvironment batchProver = batchProver();
      // no pop after an exception, which ends the analysis
      batchProver.push(bfmgr.or(pathFormulas.values()));
      Optional<ARGState> feasibleTarget = findFeasibleTarget(batchProver, pathFormulas);
      batchProver.pop();
      return feasibleTarget;
    } finally {
      satCheckTime.stop();
    }
  }

  /**
   * Whether all queries about concrete paths share one prover, as native BMC and k-induction do.
   * Native IMC and ISMC create a prover for every query.
   */
  protected boolean sharesBatchProver() {
    return true;
  }

  private Optional<ARGState> findFeasibleTarget(
      ProverEnvironment pProver, Map<ARGState, BooleanFormula> pPathFormulas)
      throws SolverException, InterruptedException {
    if (pProver.isUnsat()) {
      return Optional.empty();
    }
    try (Model model = pProver.getModel()) {
      for (Map.Entry<ARGState, BooleanFormula> entry : pPathFormulas.entrySet()) {
        if (Boolean.TRUE.equals(evaluate(model, entry.getValue()))) {
          ARGState target = entry.getKey();
          @Nullable ARGPath path = pathFromModel(model, target);
          if (path != null) {
            // the error path of the model, as native BMC creates it without another query
            target.addCounterexampleInformation(
                pathChecker.handleFeasibleCounterexample(
                    CounterexampleTraceInfo.feasible(ImmutableList.of(entry.getValue()), path),
                    path));
          }
          return Optional.of(target);
        }
      }
    }
    // the model does not decide every path, so check one path after the other
    for (Map.Entry<ARGState, BooleanFormula> entry : pPathFormulas.entrySet()) {
      if (!isUnsat(pProver, entry.getValue())) {
        return Optional.of(entry.getKey());
      }
    }
    return Optional.empty();
  }

  private @Nullable Boolean evaluate(Model pModel, BooleanFormula pFormula) {
    try {
      return pModel.evaluate(pFormula);
    } catch (IllegalArgumentException e) {
      // MathSAT fails to evaluate some formulas, e.g., with floats
      logger.logDebugException(e, "Could not evaluate the model");
      return null;
    }
  }

  /** The path to the target that the model describes, or null if the model does not decide it. */
  private @Nullable ARGPath pathFromModel(Model pModel, ARGState pTarget)
      throws InterruptedException {
    ARGState root = pTarget;
    while (!root.getParents().isEmpty()) {
      root = root.getParents().getFirst();
    }
    try {
      ARGPath path =
          predicateCpa
              .getPathFormulaManager()
              .getARGPathFromModel(pModel, root, ARGUtils.getAllStatesOnPathsTo(pTarget)::contains);
      return path.getLastState().equals(pTarget) ? path : null;
    } catch (CPATransferException | IllegalArgumentException e) {
      logger.logDebugException(e, "Could not create the error path from the model");
      return null;
    }
  }

  /**
   * Whether the given path to the target is feasible when it is encoded from the start of the
   * program. Only if an induction step on the path forgot the memory model, this can differ from
   * its concrete path formula.
   */
  protected boolean isFeasibleFromStart(ARGState pTarget, ARGPath pPath)
      throws CPAException, InterruptedException {
    if (!from(abstractionChain(pTarget))
        .anyMatch(s -> inductionStateOf(s).forgetsPointerTargets())) {
      return true;
    }
    PathFormulaManager pathFormulaManager = predicateCpa.getPathFormulaManager();
    PathFormula pathFormula = pathFormulaManager.makeEmptyPathFormula();
    for (CFAEdge edge : pPath.getFullPath()) {
      pathFormula = pathFormulaManager.makeAnd(pathFormula, edge);
    }
    try {
      return !solver.isUnsat(pathFormula.getFormula());
    } catch (SolverException e) {
      throw new CPAException("Solver failure while replaying a counterexample", e);
    }
  }

  private void reportCounterexample(ARGState pTarget) throws CPAException, InterruptedException {
    if (pTarget.getCounterexampleInformation().isPresent()) {
      // the refiner reported it already
      return;
    }
    ARGPath path = ARGUtils.getOnePathTo(pTarget);
    CounterexampleTraceInfo trace =
        interpolationManager.buildCounterexampleTraceWithoutInterpolation(
            new BlockFormulas(blockFormulas(abstractionChain(pTarget))), path);
    if (trace.isSpurious()) {
      throw new CPAException("Feasible counterexample became infeasible when replayed");
    }
    pTarget.addCounterexampleInformation(pathChecker.handleFeasibleCounterexample(trace, path));
  }

  /**
   * Remove the given abstraction states with their subtrees if no concrete path reaches any of
   * them, checked with one SAT query, as the bounding assertions of native BMC.
   *
   * @return whether the states were removed
   */
  protected boolean removeIfNoneReachable(ARGReachedSet pReached, Collection<ARGState> pStates)
      throws SolverException, InterruptedException {
    BooleanFormula reachability =
        bfmgr.or(from(pStates).transform(this::concretePathFormula).toList());
    satCheckTime.start();
    try {
      boolean unsat =
          sharesBatchProver() ? isUnsat(batchProver(), reachability) : solver.isUnsat(reachability);
      if (!unsat) {
        return false;
      }
    } finally {
      satCheckTime.stop();
    }
    removeUnreachable(pReached, pStates);
    return true;
  }

  /**
   * Remove the given abstraction states with their subtrees if none of them is reachable from the
   * region of the abstraction before it, checked with one SAT query, as the step case of native
   * k-induction. As there, the query gets a prover of its own that generates models, unless all
   * such queries share one.
   *
   * @return whether the states were removed
   */
  protected boolean removeIfNoneReachableFromRegion(
      ARGReachedSet pReached, Collection<ARGState> pStates, boolean pSharedProver)
      throws SolverException, InterruptedException {
    BooleanFormula reachability =
        bfmgr.or(from(pStates).transform(this::lastBlockFromRegion).toList());
    satCheckTime.start();
    try {
      boolean unsat;
      if (pSharedProver) {
        if (stepProver == null) {
          stepProver = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS);
        }
        unsat = isUnsat(stepProver, reachability);
      } else {
        try (ProverEnvironment ownProver =
            solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
          unsat = isUnsat(ownProver, reachability);
        }
      }
      if (!unsat) {
        return false;
      }
    } finally {
      satCheckTime.stop();
    }
    removeUnreachable(pReached, pStates);
    return true;
  }

  /** The prover for the queries about concrete paths, as native BMC has one for all bounds. */
  private ProverEnvironment batchProver() {
    if (prover == null) {
      prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS);
    }
    return prover;
  }

  private static boolean isUnsat(ProverEnvironment pProver, BooleanFormula pFormula)
      throws SolverException, InterruptedException {
    // no pop after an exception, which ends the analysis
    pProver.push(pFormula);
    boolean unsat = pProver.isUnsat();
    pProver.pop();
    return unsat;
  }

  /** Remove the given states with their subtrees, because no concrete path reaches them. */
  protected void removeUnreachable(ARGReachedSet pReached, Collection<ARGState> pStates)
      throws InterruptedException {
    for (ARGState state : pStates) {
      if (state.isDestroyed()) {
        continue;
      }
      // Covered states are not in the reached set, but removeInfeasiblePartofARG expects them
      // there. Thus uncover them here: inside the subtree they disappear, elsewhere they are
      // explored again.
      Set<ARGState> subtree = state.getSubgraph().toSet();
      for (ARGState covering : subtree) {
        for (ARGState covered : ImmutableList.copyOf(covering.getCoveredByThis())) {
          if (subtree.contains(covered)) {
            covered.uncover();
          } else {
            pReached.removeSubtree(covered);
          }
        }
      }
      pReached.removeInfeasiblePartofARG(state);
      removedStates.inc();
    }
  }

  protected static InductionPrecision inductionPrecisionOf(
      ARGReachedSet pReached, ARGState pState) {
    return Precisions.extractPrecisionByType(
        pReached.asReachedSet().getPrecision(pState), InductionPrecision.class);
  }

  protected static PredicatePrecision predicatePrecisionOf(
      ARGReachedSet pReached, ARGState pState) {
    return Precisions.extractPrecisionByType(
        pReached.asReachedSet().getPrecision(pState), PredicatePrecision.class);
  }

  protected static InductionState inductionStateOf(ARGState pState) {
    return AbstractStates.extractStateByType(pState, InductionState.class);
  }

  /** The first abstraction at a loop head on the path to the given state. */
  protected static Optional<ARGState> firstLoopHeadAbstraction(ARGState pState) {
    return from(abstractionChain(pState))
        .firstMatch(s -> inductionStateOf(s).isAbstraction())
        .toJavaUtil();
  }

  /**
   * Explore the given state again, together with its siblings at the same location, e.g., the
   * abstraction and the concrete continuation of an induction start. Their parents get the given
   * precisions.
   */
  protected void reexplore(
      ARGReachedSet pReached,
      ARGState pState,
      List<Precision> pPrecisions,
      List<Predicate<? super Precision>> pPrecisionTypes)
      throws InterruptedException {
    if (pState.isDestroyed()) {
      return;
    }
    @Nullable CFANode location = AbstractStates.extractLocation(pState);
    Set<ARGState> siblings = new LinkedHashSet<>();
    for (ARGState parent : pState.getParents()) {
      for (ARGState child : parent.getChildren()) {
        if (location.equals(AbstractStates.extractLocation(child))) {
          siblings.add(child);
        }
      }
    }
    for (ARGState sibling : siblings) {
      if (!sibling.isDestroyed()) {
        pReached.removeSubtree(sibling, pPrecisions, pPrecisionTypes);
        reexploredSubtrees.inc();
      }
    }
  }

  /**
   * Explore the given state again, with k increased by one for all loops. The state must be a
   * successor of a loop-head arrival, e.g., an induction start or a cut-off.
   */
  protected void increaseK(ARGReachedSet pReached, ARGState pState) throws InterruptedException {
    reexploreWith(pReached, pState, p -> p.withK(p.k() + 1));
  }

  /**
   * Explore the given state again, with the given change of the precision of all loops. The state
   * must be a successor of a loop-head arrival, e.g., an induction start or a cut-off.
   */
  protected void reexploreWith(
      ARGReachedSet pReached, ARGState pState, UnaryOperator<LoopPrecision> pChange)
      throws InterruptedException {
    if (pState.isDestroyed()) {
      // inside a subtree that is explored again anyway
      return;
    }
    InductionPrecision precision =
        inductionPrecisionOf(pReached, pState.getParents().getFirst()).withAll(pChange);
    recordK(precision);
    reexplore(
        pReached,
        pState,
        ImmutableList.of(precision),
        ImmutableList.of(Predicates.instanceOf(InductionPrecision.class)));
  }

  protected void recordK(InductionPrecision pPrecision) {
    pPrecision.getLoops().values().forEach(p -> maxK.setNextValue(p.k()));
  }

  /**
   * The refinement of BMC, which IMC and ISMC fall back to: check the targets, then the bounding
   * assertions for the cut-offs, and increment k at every reachable cut-off.
   */
  protected Optional<ARGState> refineAsBmc(
      ARGReachedSet pReached, List<ARGState> pTargets, List<ARGState> pCutOffs)
      throws CPAException, InterruptedException, SolverException {
    Optional<ARGState> counterexample = findFeasibleTarget(pTargets);
    if (counterexample.isPresent()) {
      return counterexample;
    }
    removeUnreachable(pReached, pTargets);
    if (pCutOffs.isEmpty() || removeIfNoneReachable(pReached, pCutOffs)) {
      return Optional.empty();
    }
    for (ARGState cutOff : pCutOffs) {
      increaseK(pReached, cutOff);
    }
    return Optional.empty();
  }

  /** Explore again from the root with the precision of BMC with bound k, at least the given one. */
  protected void restartAsBmc(ARGReachedSet pReached, int pMinimalK) throws InterruptedException {
    ARGState root = (ARGState) pReached.asReachedSet().getFirstState();
    InductionPrecision precision =
        inductionPrecisionOf(pReached, root)
            .withAll(
                p ->
                    new LoopPrecision(
                        Math.max(p.k(), pMinimalK),
                        /* induction= */ false,
                        /* describedAbstractions= */ 0,
                        /* unrolling= */ false));
    recordK(precision);
    pReached.removeAllExceptFirstStateWithPrecision(
        ImmutableList.of(precision, PredicatePrecision.empty()),
        ImmutableList.of(
            Predicates.instanceOf(InductionPrecision.class),
            Predicates.instanceOf(PredicatePrecision.class)));
  }

  /**
   * Explore again as BMC if the program has more loop heads than the limit, as native IMC and ISMC
   * do for more than one loop head.
   *
   * @return whether the refiner continues as BMC
   */
  protected boolean fallBackToBmcIfNecessary(ARGReachedSet pReached, int pLoopLimit)
      throws InterruptedException {
    if (predicateCpa.getCfa().getAllLoopHeads().orElseThrow().size() <= pLoopLimit) {
      return false;
    }
    logger.log(
        Level.INFO,
        "Falling back to BMC, because the program has more than",
        pLoopLimit,
        "loop heads");
    restartAsBmc(pReached, 1);
    return true;
  }

  /** Whether an interpolation failed, after which native IMC and ISMC continue as BMC. */
  protected boolean hasFailedInterpolation() {
    return failedInterpolations() > 0;
  }

  /** The number of interpolations that failed so far. */
  protected long failedInterpolations() {
    return interpolationFailures.getValue();
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(this);
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    StatisticsWriter writer =
        StatisticsWriter.writingStatisticsTo(pOut)
            .put(refinementTime)
            .put(satCheckTime)
            .put(removedStates)
            .put(interpolationFailures)
            .put(reexploredSubtrees)
            .put(maxK);
    printRefinerStatistics(writer);
  }

  protected void printRefinerStatistics(StatisticsWriter pWriter) {}

  @Override
  public String getName() {
    return getClass().getSimpleName();
  }
}
