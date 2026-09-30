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
import com.google.common.collect.ImmutableList;
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
import java.util.function.Function;
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
import org.sosy_lab.cpachecker.cpa.induction.InductionState;
import org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaStrategy.BlockFormulas;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
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
  private int refinementNumber = 0;

  private final StatTimer refinementTime = new StatTimer("Time for refinements");
  private final StatTimer satCheckTime = new StatTimer("Time for batch SAT checks");
  private final StatCounter removedStates = new StatCounter("Unreachable states removed");
  private final StatCounter deferredRefinements = new StatCounter("Deferred refinements");
  private final StatCounter reexploredSubtrees = new StatCounter("Re-explored subtrees");

  protected InductionRefiner(ConfigurableProgramAnalysis pCpa, LogManager pLogger)
      throws InvalidConfigurationException {
    argCpa = CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, getClass());
    predicateCpa = CPAs.retrieveCPAOrFail(pCpa, PredicateCPA.class, getClass());
    CPAs.retrieveCPAOrFail(pCpa, InductionCPA.class, getClass());
    logger = pLogger;
    solver = predicateCpa.getSolver();
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    Configuration config = predicateCpa.getConfiguration();
    CFA cfa = predicateCpa.getCfa();
    interpolationManager =
        new InterpolationManager(
            predicateCpa.getPathFormulaManager(),
            solver,
            cfa.getLoopStructure(),
            cfa.getVarClassification(),
            config,
            predicateCpa.getShutdownNotifier(),
            logger,
            /* pEnableCounterexampleAnalysis= */ true);
    pathChecker =
        new PathChecker(
            config,
            logger,
            predicateCpa.getShutdownNotifier(),
            cfa.getMachineModel(),
            predicateCpa.getPathFormulaManager(),
            solver);
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

  /** Whether to continue the exploration before this refinement. */
  protected boolean deferRefinement(ReachedSet pReached) {
    return false;
  }

  @Override
  public final boolean performRefinement(ReachedSet pReached)
      throws CPAException, InterruptedException {
    refinementTime.start();
    try {
      if (deferRefinement(pReached)) {
        deferredRefinements.inc();
        return true;
      }
      ARGReachedSet reached = new ARGReachedSet(pReached, argCpa, refinementNumber++);
      List<ARGState> targets = new ArrayList<>();
      List<ARGState> cutOffs = new ArrayList<>();
      for (ARGState state : from(pReached).filter(ARGState.class).filter(ARGState::isTarget)) {
        (isPropertyViolation(state) ? targets : cutOffs).add(state);
      }

      Optional<ARGState> counterexample = refine(reached, targets, cutOffs);
      if (counterexample.isPresent()) {
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

  /** The abstraction states on the path after its first state, which is the root of the ARG. */
  protected static ImmutableList<ARGState> abstractionStatesOnPath(ARGPath pPath) {
    return from(pPath.asStatesList())
        .skip(1)
        .filter(s -> getPredicateState(s).isAbstractionState())
        .toList();
  }

  protected static ImmutableList<BooleanFormula> blockFormulas(List<ARGState> pAbstractionStates) {
    return from(pAbstractionStates).transform(s -> getBlockFormula(getPredicateState(s))).toList();
  }

  /** The formula of all paths from the root to the given abstraction state. */
  protected BooleanFormula concretePathFormula(ARGState pAbstractionState) {
    return bfmgr.and(
        blockFormulas(abstractionStatesOnPath(ARGUtils.getOnePathTo(pAbstractionState))));
  }

  /**
   * The formula of the block that ends in the given abstraction state, starting in the region of
   * the abstraction before it.
   */
  protected BooleanFormula lastBlockFromRegion(ARGState pAbstractionState) {
    ARGPath path = ARGUtils.getOnePathTo(pAbstractionState);
    List<ARGState> abstractions = abstractionStatesOnPath(path);
    ARGState previous =
        abstractions.size() < 2 ? path.getFirstState() : abstractions.get(abstractions.size() - 2);
    return bfmgr.and(
        getPredicateState(previous).getAbstractionFormula().asInstantiatedFormula(),
        getBlockFormula(getPredicateState(pAbstractionState)));
  }

  /**
   * Whether every return on the path matches its call. After a restart of the callstack at an
   * induction start, the path can return to another caller, which the path formula does not show.
   */
  private static boolean hasMatchingCallsAndReturns(ARGPath pPath) {
    Deque<CFANode> callSites = new ArrayDeque<>();
    for (CFAEdge edge : pPath.getFullPath()) {
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
      if (hasMatchingCallsAndReturns(ARGUtils.getOnePathTo(target))) {
        pathFormulas.put(target, concretePathFormula(target));
      }
    }
    if (pathFormulas.isEmpty()) {
      return Optional.empty();
    }

    satCheckTime.start();
    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.push(bfmgr.or(pathFormulas.values()));
      if (prover.isUnsat()) {
        return Optional.empty();
      }
      try (Model model = prover.getModel()) {
        for (Map.Entry<ARGState, BooleanFormula> entry : pathFormulas.entrySet()) {
          if (Boolean.TRUE.equals(model.evaluate(entry.getValue()))) {
            return Optional.of(entry.getKey());
          }
        }
      }
    } finally {
      satCheckTime.stop();
    }
    // the model does not decide every path, so check one path after the other
    for (Map.Entry<ARGState, BooleanFormula> entry : pathFormulas.entrySet()) {
      if (!solver.isUnsat(entry.getValue())) {
        return Optional.of(entry.getKey());
      }
    }
    return Optional.empty();
  }

  private void reportCounterexample(ARGState pTarget) throws CPAException, InterruptedException {
    ARGPath path = ARGUtils.getOnePathTo(pTarget);
    CounterexampleTraceInfo trace =
        interpolationManager.buildCounterexampleTraceWithoutInterpolation(
            new BlockFormulas(blockFormulas(abstractionStatesOnPath(path))), path);
    if (trace.isSpurious()) {
      throw new CPAException("Feasible counterexample became infeasible when replayed");
    }
    pTarget.addCounterexampleInformation(pathChecker.handleFeasibleCounterexample(trace, path));
  }

  /**
   * Remove the given states with their subtrees if the formulas of all of them are unsatisfiable,
   * checked with one SAT query.
   *
   * @return whether the states were removed
   */
  protected boolean removeIfNoneReachable(
      ARGReachedSet pReached,
      Collection<ARGState> pStates,
      Function<ARGState, BooleanFormula> pReachability)
      throws SolverException, InterruptedException {
    satCheckTime.start();
    try {
      if (!solver.isUnsat(bfmgr.or(from(pStates).transform(pReachability::apply).toList()))) {
        return false;
      }
    } finally {
      satCheckTime.stop();
    }
    removeUnreachable(pReached, pStates);
    return true;
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

  protected static InductionState inductionStateOf(ARGState pState) {
    return AbstractStates.extractStateByType(pState, InductionState.class);
  }

  /** The first abstraction at a loop head on the path to the given state. */
  protected static Optional<ARGState> firstLoopHeadAbstraction(ARGState pState) {
    return from(ARGUtils.getOnePathTo(pState).asStatesList())
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
            .put(deferredRefinements)
            .put(reexploredSubtrees);
    printRefinerStatistics(writer);
  }

  protected void printRefinerStatistics(StatisticsWriter pWriter) {}

  @Override
  public String getName() {
    return getClass().getSimpleName();
  }
}
