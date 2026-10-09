// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.induction.refiner;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.getPredicateState;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.SetMultimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.induction.InductionPrecision;
import org.sosy_lab.cpachecker.cpa.induction.InductionPrecision.LoopPrecision;
import org.sosy_lab.cpachecker.cpa.induction.InductionState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionRefinementStrategy;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPARefinerFactory;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager.IfThenElseParts;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * The refiner of PredK. It refines one counterexample like native predicate analysis, and then
 * decides per loop head with the heuristic of the paper: if the predicates mostly hold at single
 * visits, the abstractions move further apart; if almost no abstraction closes its loop, or if the
 * interpolation learned nothing, an induction step takes over. Predicates that occur in no
 * abstraction are dropped.
 *
 * <p>The refiner prefers targets whose paths contain no induction start. Targets in induction steps
 * pass the queries of k-induction first, so that spurious induction steps cannot hide a real
 * counterexample of the base case. Options let the refiner check counterexamples without
 * abstraction as BMC does, make spurious induction steps longer as k-induction does instead of
 * interpolating them, and only refine where the predicates describe single visits.
 */
@Options(prefix = "cpa.induction.refinement.predk")
public final class PredKRefiner extends InductionRefiner {

  @Option(
      secure = true,
      description =
          "check the targets whose paths contain no abstraction at a loop head with one SAT query,"
              + " as BMC does, and remove the unreachable ones without interpolation")
  private boolean checkConcreteTargetsAsBmc = false;

  @Option(
      secure = true,
      description =
          "refine a spurious target of an induction step by interpolation. Otherwise, the"
              + " induction step gets longer, as in k-induction.")
  private boolean interpolateInductionSteps = true;

  @Option(
      secure = true,
      description =
          "raise k at a loop head whose predicates mostly describe single visits, which moves its"
              + " abstractions apart. Otherwise, the refinement only adds the predicates.")
  private boolean moveApartForSingleVisits = true;

  private final PredKStrategy strategy;
  private final ARGBasedRefiner predicateRefiner;
  private final AbstractionManager abstractionManager;
  private final LoopStructure loopStructure;

  /** The predicates that were dropped at a loop head once, and stay if they are learned again. */
  private final SetMultimap<CFANode, AbstractionPredicate> droppedBefore = HashMultimap.create();

  private final StatCounter raisedK = new StatCounter("Loop heads with a larger k");
  private final StatCounter startedInductions = new StatCounter("Loop heads that start induction");
  private final StatCounter droppedPredicates = new StatCounter("Dropped predicates");
  private final StatCounter refinementsWithoutPredicates =
      new StatCounter("Refinements that learned no predicate");

  private PredKRefiner(ConfigurableProgramAnalysis pCpa, LogManager pLogger, Configuration pConfig)
      throws InvalidConfigurationException {
    super(pCpa, pLogger, pConfig);
    pConfig.inject(this);
    abstractionManager = predicateCpa.getAbstractionManager();
    loopStructure = predicateCpa.getCfa().getLoopStructure().orElseThrow();
    strategy = new PredKStrategy(pConfig, pLogger, predicateCpa.getPredicateManager(), solver);
    predicateRefiner = new PredicateCPARefinerFactory(pCpa).create(strategy);
  }

  public static Refiner create(ConfigurableProgramAnalysis pCpa, LogManager pLogger)
      throws InvalidConfigurationException {
    return new PredKRefiner(pCpa, pLogger, configurationOf(pCpa, PredKRefiner.class));
  }

  private static boolean hasInductionStart(ARGState pState) {
    return from(abstractionChain(pState)).anyMatch(s -> inductionStateOf(s).isInductionStart());
  }

  @Override
  protected Optional<ARGState> refine(
      ARGReachedSet pReached, List<ARGState> pTargets, List<ARGState> pCutOffs)
      throws CPAException, InterruptedException, SolverException {
    if (!pCutOffs.isEmpty()) {
      throw new CPAException("PredK needs a precision that describes all abstractions");
    }
    List<ARGState> targets = pTargets;
    if (checkConcreteTargetsAsBmc) {
      // without an abstraction on the path, an interpolant could only rule out this path
      List<ARGState> concreteTargets =
          from(targets).filter(t -> firstLoopHeadAbstraction(t).isEmpty()).toList();
      if (!concreteTargets.isEmpty()) {
        Optional<ARGState> counterexample = findFeasibleTarget(concreteTargets);
        if (counterexample.isPresent()) {
          return counterexample;
        }
        removeConcreteTargets(pReached, concreteTargets);
        targets = from(targets).filter(t -> !t.isDestroyed()).toList();
      }
    }
    Optional<ARGState> baseCaseTarget =
        from(targets).firstMatch(t -> !hasInductionStart(t)).toJavaUtil();
    if (baseCaseTarget.isPresent()) {
      return refinePath(pReached, baseCaseTarget.get());
    }

    // Only induction steps are left, which are checked as k-induction does before a refinement.
    // The bounding assertion holds only after a concrete path, since an abstraction on the path
    // hides the paths that its coverage folds.
    Set<ARGState> inductionStarts = new LinkedHashSet<>();
    for (ARGState target : targets) {
      ARGState inductionStart =
          from(abstractionChain(target))
              .firstMatch(s -> inductionStateOf(s).isInductionStart())
              .get();
      if (firstLoopHeadAbstraction(inductionStart).orElseThrow() == inductionStart) {
        inductionStarts.add(inductionStart);
      }
    }
    if (!inductionStarts.isEmpty() && removeIfNoneReachable(pReached, inductionStarts)) {
      return Optional.empty();
    }
    for (ARGState target : targets) {
      if (!target.isDestroyed() && !solver.isUnsat(lastBlockFromRegion(target))) {
        if (interpolateInductionSteps) {
          return refinePath(pReached, target);
        }
        // as k-induction: a counterexample if the path is feasible, otherwise a longer step
        Optional<ARGState> counterexample = findFeasibleTarget(ImmutableList.of(target));
        if (counterexample.isPresent() && forgetsMemory(target)) {
          counterexample = replayedCounterexample(target);
        }
        if (counterexample.isEmpty()) {
          strategy.refineWithoutPredicates(pReached, target);
        }
        return counterexample;
      }
    }
    removeUnreachable(pReached, targets);
    return Optional.empty();
  }

  /**
   * Whether the exception is a failure that MathSAT reports, e.g., in non-linear arithmetic, before
   * the refinement changed anything.
   */
  private static boolean isSolverFailure(IllegalArgumentException pException) {
    StackTraceElement[] trace = pException.getStackTrace();
    return trace.length > 0 && trace[0].getClassName().startsWith("org.sosy_lab.java_smt.solvers.");
  }

  /**
   * Remove the given unreachable targets. Their parents get the predicate false at the location of
   * the target, as from the interpolants of predicate analysis, so that a parent that the
   * exploration expands again does not reach the target again.
   */
  private void removeConcreteTargets(ARGReachedSet pReached, List<ARGState> pTargets)
      throws InterruptedException {
    SetMultimap<ARGState, CFANode> parents = LinkedHashMultimap.create();
    for (ARGState target : pTargets) {
      for (ARGState parent : target.getParents()) {
        parents.put(parent, AbstractStates.extractLocation(target));
      }
    }
    removeUnreachable(pReached, pTargets);
    AbstractionPredicate falsePredicate = abstractionManager.makeFalsePredicate();
    for (Map.Entry<ARGState, Collection<CFANode>> entry : parents.asMap().entrySet()) {
      ARGState parent = entry.getKey();
      if (!parent.isDestroyed()) {
        pReached.updatePrecisionForState(
            parent,
            predicatePrecisionOf(pReached, parent)
                .addLocalPredicates(
                    from(entry.getValue()).transform(l -> Map.entry(l, falsePredicate))),
            Predicates.instanceOf(PredicatePrecision.class));
      }
    }
  }

  /** Refine the path to the given target as predicate analysis does, followed by the heuristic. */
  private Optional<ARGState> refinePath(ARGReachedSet pReached, ARGState pTarget)
      throws CPAException, InterruptedException, SolverException {
    if (forgetsMemory(pTarget) && !solver.isUnsat(concretePathFormula(pTarget))) {
      // the path formula describes no memory of the program, so it cannot be interpolated either
      Optional<ARGState> counterexample = replayedCounterexample(pTarget);
      if (counterexample.isEmpty()) {
        strategy.refineWithoutPredicates(pReached, pTarget);
      }
      return counterexample;
    }
    if (!isRealPath(pTarget)) {
      // a path formula that returns to another caller than the one of the call
      strategy.refineWithoutPredicates(pReached, pTarget);
      return Optional.empty();
    }
    CounterexampleInfo counterexample;
    try {
      counterexample =
          predicateRefiner.performRefinementForPath(pReached, ARGUtils.getOnePathTo(pTarget));
    } catch (RefinementFailedException e) {
      // the interpolation failed or learned nothing, so only the induction can make progress
      logger.logDebugException(e, "Predicate refinement failed");
      strategy.refineWithoutPredicates(pReached, pTarget);
      return Optional.empty();
    } catch (IllegalArgumentException e) {
      if (!isSolverFailure(e)) {
        throw e;
      }
      logger.logDebugException(e, "Interpolation failed");
      strategy.refineWithoutPredicates(pReached, pTarget);
      return Optional.empty();
    }
    if (counterexample.isSpurious()) {
      return Optional.empty();
    }
    pTarget.addCounterexampleInformation(counterexample);
    return Optional.of(pTarget);
  }

  /**
   * The refinement strategy of native predicate analysis, whose update of the ARG applies the
   * heuristic of PredK to the new precision.
   */
  private final class PredKStrategy extends PredicateAbstractionRefinementStrategy {

    /** The abstraction states of the path that is refined. */
    private @Nullable List<ARGState> trace = null;

    /** The predicates of the interpolants of that path. */
    private PredicatePrecision interpolantPredicates = PredicatePrecision.empty();

    private PredKStrategy(
        Configuration pConfig,
        LogManager pLogger,
        PredicateAbstractionManager pPredicateManager,
        Solver pSolver)
        throws InvalidConfigurationException {
      super(pConfig, pLogger, pPredicateManager, pSolver);
    }

    @Override
    public boolean performRefinement(
        ARGReachedSet pReached,
        List<ARGState> pTrace,
        List<BooleanFormula> pInterpolants,
        boolean pRepeatedCounterexample)
        throws CPAException, InterruptedException {
      trace = pTrace;
      try {
        return super.performRefinement(pReached, pTrace, pInterpolants, pRepeatedCounterexample);
      } finally {
        trace = null;
      }
    }

    @Override
    protected PredicatePrecision addPredicatesToPrecision(PredicatePrecision pBasePrecision) {
      interpolantPredicates = super.addPredicatesToPrecision(PredicatePrecision.empty());
      return super.addPredicatesToPrecision(pBasePrecision);
    }

    @Override
    protected AbstractState getRefinedTargetState(
        UnmodifiableReachedSet pReached, List<ARGState> pTrace) {
      return pTrace.getLast();
    }

    @Override
    protected void updateARG(
        PredicatePrecision pNewPrecision, ARGState pRefinementRoot, ARGReachedSet pReached)
        throws InterruptedException {
      List<ARGState> path = checkNotNull(trace);
      ARGState target = path.getLast();
      applyHeuristic(
          pReached,
          target,
          pRefinementRoot,
          predicatePrecisionOf(pReached, target),
          pNewPrecision,
          interpolantPredicates,
          learnedPredicate(pReached, path));
    }

    /**
     * Whether an abstraction of the path gets a predicate of the interpolants that it was not
     * computed with. The abstractions above the refinement root of an earlier refinement keep their
     * older precision.
     */
    private boolean learnedPredicate(ARGReachedSet pReached, List<ARGState> pPath) {
      for (ARGState state : pPath.subList(0, pPath.size() - 1)) {
        if (inductionStateOf(state).isAbstraction()) {
          CFANode loopHead = inductionStateOf(state).getLoopHead();
          int instance =
              checkNotNull(getPredicateState(state).getAbstractionLocationsOnPath().get(loopHead));
          if (!predicatePrecisionOf(pReached, state)
              .getPredicates(loopHead, instance)
              .containsAll(interpolantPredicates.getPredicates(loopHead, instance))) {
            return true;
          }
        }
      }
      return false;
    }

    /** Refine for a path that taught no predicate, which only the induction can answer. */
    private void refineWithoutPredicates(ARGReachedSet pReached, ARGState pTarget)
        throws InterruptedException {
      refinementsWithoutPredicates.inc();
      PredicatePrecision precision = predicatePrecisionOf(pReached, pTarget);
      applyHeuristic(
          pReached, pTarget, null, precision, precision, PredicatePrecision.empty(), false);
    }
  }

  /**
   * Decide k and the induction of every loop head (Alg. predk-heuristic), drop the predicates that
   * occur in no abstraction, and explore again from the earliest state the change concerns.
   */
  private void applyHeuristic(
      ARGReachedSet pReached,
      ARGState pTarget,
      @Nullable ARGState pRefinementRoot,
      PredicatePrecision pOldPrecision,
      PredicatePrecision pNewPrecision,
      PredicatePrecision pInterpolantPredicates,
      boolean pLearned)
      throws InterruptedException {
    UnmodifiableReachedSet reached = pReached.asReachedSet();
    InductionPrecision oldInduction = inductionPrecisionOf(pReached, pTarget);
    Exploration exploration = new Exploration(pReached, oldInduction);

    InductionPrecision induction = oldInduction;
    Set<Loop> changedLoops = new HashSet<>();
    for (CFANode loopHead : exploration.abstractions.elementSet()) {
      for (Loop loop : loopStructure.getLoopsForLoopHead(loopHead)) {
        LoopPrecision precision = induction.get(loop);
        Set<AbstractionPredicate> predicates = pOldPrecision.getLocalPredicates().get(loopHead);
        Multiset<AbstractionPredicate> holding = exploration.holdingAt(loopHead);
        long aboutOneVisit = from(predicates).filter(q -> holding.count(q) == 1).size();
        long aboutTheLoop = from(predicates).filter(q -> holding.count(q) > 1).size();
        int abstractions = exploration.abstractions.count(loopHead);
        int notClosing = exploration.notClosing.count(loopHead);
        boolean base = exploration.unrolledInBaseCase.contains(loopHead);

        LoopPrecision changed = precision;
        if (!pLearned) {
          // nothing learned: only the induction can make progress
          changed = precision.withK(precision.k() + 1).withInduction(precision.induction() || base);
        } else if (aboutOneVisit > Math.max(aboutTheLoop, precision.k() + 1)) {
          // the predicates describe single visits: move the abstractions apart
          if (moveApartForSingleVisits) {
            changed = precision.withK(precision.k() + 1);
          }
        } else if (abstractions > precision.k() + 3 && notClosing >= abstractions - 1 && base) {
          // no abstraction closes its loop: use the induction
          changed = precision.withK(precision.k() + 1).withInduction(true);
        }
        if (!changed.equals(precision)) {
          logger.logf(
              Level.FINE,
              "Loop head %s changes from %s to %s with %d predicates about one visit, %d about the"
                  + " loop, %d abstractions and %d not closing",
              loopHead,
              precision,
              changed,
              aboutOneVisit,
              aboutTheLoop,
              abstractions,
              notClosing);
          if (changed.k() > precision.k()) {
            raisedK.inc();
          }
          if (changed.induction() && !precision.induction()) {
            startedInductions.inc();
          }
          induction = induction.with(loop, changed);
          changedLoops.add(loop);
        }
      }
    }
    recordK(induction);

    // only a complete exploration shows which predicates hold nowhere
    PredicatePrecision predicates =
        reached.hasWaitingState()
            ? pNewPrecision
            : withoutUnusedPredicates(
                pOldPrecision, pNewPrecision, pInterpolantPredicates, exploration);

    // The root is the earliest of the native one and the first abstraction of a changed loop.
    ARGState root = pRefinementRoot;
    for (ARGState state : abstractionChain(pTarget)) {
      InductionState inductionState = inductionStateOf(state);
      if (inductionState.isAbstraction()
          && !changedLoops.isEmpty()
          && from(loopStructure.getLoopsForLoopHead(inductionState.getLoopHead()))
              .anyMatch(changedLoops::contains)) {
        if (root == null || !isAbove(root, state)) {
          root = state;
        }
        break;
      }
    }
    if (root == null) {
      root = (ARGState) reached.getFirstState();
    }

    ImmutableList<Precision> precisions = ImmutableList.of(predicates, induction);
    ImmutableList<Predicate<? super Precision>> types =
        ImmutableList.of(
            Predicates.instanceOf(PredicatePrecision.class),
            Predicates.instanceOf(InductionPrecision.class));
    if (root.getParents().isEmpty()) {
      pReached.removeAllExceptFirstStateWithPrecision(precisions, types);
    } else {
      reexplore(pReached, root, precisions, types);
    }
  }

  /** Whether the first abstraction state lies on the path to the second one. */
  private static boolean isAbove(ARGState pUpper, ARGState pLower) {
    return abstractionChain(pLower).contains(pUpper);
  }

  /**
   * The new predicates without the old ones of a loop head that its abstractions were computed with
   * and that occur in no abstraction. The predicates of the interpolants stay, as do predicates
   * that were dropped before, since a predicate that only makes abstractions infeasible occurs
   * nowhere.
   */
  private PredicatePrecision withoutUnusedPredicates(
      PredicatePrecision pOldPrecision,
      PredicatePrecision pNewPrecision,
      PredicatePrecision pInterpolantPredicates,
      Exploration pExploration) {
    SetMultimap<CFANode, AbstractionPredicate> local =
        LinkedHashMultimap.create(pNewPrecision.getLocalPredicates());
    for (CFANode loopHead : pExploration.abstractions.elementSet()) {
      for (AbstractionPredicate predicate : pOldPrecision.getLocalPredicates().get(loopHead)) {
        if (pExploration.offered.containsEntry(loopHead, predicate)
            && !pExploration.usedVariables.contains(predicate.getAbstractVariable())
            && !pInterpolantPredicates.getLocalPredicates().containsEntry(loopHead, predicate)
            && !bfmgr.isFalse(predicate.getSymbolicAtom())
            && droppedBefore.put(loopHead, predicate)
            && local.remove(loopHead, predicate)) {
          logger.log(Level.FINE, "Dropping predicate", predicate, "at loop head", loopHead);
          droppedPredicates.inc();
        }
      }
    }
    return new PredicatePrecision(
        pNewPrecision.getLocationInstancePredicates(),
        local,
        pNewPrecision.getFunctionPredicates(),
        pNewPrecision.getGlobalPredicates());
  }

  /** What the heuristic needs to know about the abstractions of the last exploration. */
  private final class Exploration {

    private final Multiset<CFANode> abstractions = HashMultiset.create();
    private final Multiset<CFANode> notClosing = HashMultiset.create();
    private final Map<CFANode, Multiset<AbstractionPredicate>> holding = new HashMap<>();

    /** The predicates that some abstraction of a loop head was computed with. */
    private final SetMultimap<CFANode, AbstractionPredicate> offered = LinkedHashMultimap.create();

    /** The variables of the predicates that some abstraction depends on. */
    private final Set<Region> usedVariables = new HashSet<>();

    private final Set<CFANode> unrolledInBaseCase = new HashSet<>();

    private Exploration(ARGReachedSet pReached, InductionPrecision pInduction)
        throws InterruptedException {
      UnmodifiableReachedSet reached = pReached.asReachedSet();
      Set<ARGState> onCycles = abstractionsOnCycles(reached);
      for (ARGState state : from(reached).filter(ARGState.class)) {
        InductionState inductionState = inductionStateOf(state);
        if (getPredicateState(state).isAbstractionState()) {
          addVariables(getPredicateState(state).getAbstractionFormula().asRegion());
        }
        if (inductionState.isAbstraction()) {
          CFANode loopHead = inductionState.getLoopHead();
          abstractions.add(loopHead);
          if (!onCycles.contains(state)) {
            notClosing.add(loopHead);
          }
          Multiset<AbstractionPredicate> holdingHere =
              holding.computeIfAbsent(loopHead, l -> HashMultiset.create());
          for (AbstractionPredicate predicate :
              predicatePrecisionOf(pReached, state).getLocalPredicates().get(loopHead)) {
            offered.put(loopHead, predicate);
            if (holds(state, predicate)) {
              holdingHere.add(predicate);
            }
          }
        } else if (getPredicateState(state).getAbstractionLocationsOnPath().isEmpty()) {
          CFANode location = AbstractStates.extractLocation(state);
          for (Loop loop : loopStructure.getLoopsForLoopHead(location)) {
            if (inductionState.getIteration(loop) >= pInduction.get(loop).k()) {
              unrolledInBaseCase.add(location);
            }
          }
        }
      }
    }

    private void addVariables(Region pRegion) {
      RegionManager regionManager = abstractionManager.getRegionCreator();
      Set<Region> visited = new HashSet<>();
      Deque<Region> waitlist = new ArrayDeque<>();
      waitlist.push(pRegion);
      while (!waitlist.isEmpty()) {
        Region region = waitlist.pop();
        if (!region.isTrue() && !region.isFalse() && visited.add(region)) {
          IfThenElseParts parts = regionManager.getIfThenElse(region);
          usedVariables.add(parts.condition());
          waitlist.push(parts.thenBranch());
          waitlist.push(parts.elseBranch());
        }
      }
    }

    private Multiset<AbstractionPredicate> holdingAt(CFANode pLoopHead) {
      return holding.getOrDefault(pLoopHead, HashMultiset.create());
    }

    private boolean holds(ARGState pState, AbstractionPredicate pPredicate)
        throws InterruptedException {
      try {
        return abstractionManager.entails(
            getPredicateState(pState).getAbstractionFormula().asRegion(),
            pPredicate.getAbstractVariable());
      } catch (SolverException e) {
        logger.logDebugException(e, "Could not decide whether a predicate holds");
        return false;
      }
    }
  }

  /**
   * The abstraction states that lie on a cycle of the graph whose edges lead from an abstraction to
   * the next ones and from a covered state to the state that covers it. An abstraction on such a
   * cycle closes its loop.
   */
  private static Set<ARGState> abstractionsOnCycles(UnmodifiableReachedSet pReached) {
    Map<ARGState, List<ARGState>> successors = new HashMap<>();
    for (ARGState state : from(pReached).filter(ARGState.class)) {
      if (getPredicateState(state).isAbstractionState()) {
        successors.put(state, nextAbstractions(state));
      }
    }
    // the covered states are not in the reached set
    for (List<ARGState> next : ImmutableList.copyOf(successors.values())) {
      for (ARGState covered : next) {
        if (covered.isCovered()) {
          successors.put(covered, ImmutableList.of(covered.getCoveringState()));
        }
      }
    }
    return statesInNontrivialComponents(successors);
  }

  /** The abstraction and covered states that the given state reaches without abstraction. */
  private static List<ARGState> nextAbstractions(ARGState pAbstraction) {
    List<ARGState> result = new ArrayList<>();
    Deque<ARGState> waitlist = new ArrayDeque<>(pAbstraction.getChildren());
    Set<ARGState> seen = new HashSet<>(waitlist);
    while (!waitlist.isEmpty()) {
      ARGState state = waitlist.pop();
      if (state.isCovered() || getPredicateState(state).isAbstractionState()) {
        result.add(state);
      } else {
        for (ARGState child : state.getChildren()) {
          if (seen.add(child)) {
            waitlist.push(child);
          }
        }
      }
    }
    return result;
  }

  /** The nodes of the given graph that lie in a strongly connected component with a cycle. */
  private static Set<ARGState> statesInNontrivialComponents(
      Map<ARGState, List<ARGState>> pSuccessors) {
    // iterative Tarjan
    Map<ARGState, Integer> index = new HashMap<>();
    Map<ARGState, Integer> lowlink = new HashMap<>();
    Deque<ARGState> stack = new ArrayDeque<>();
    Set<ARGState> onStack = new HashSet<>();
    Set<ARGState> result = new HashSet<>();
    int nextIndex = 0;
    for (ARGState start : pSuccessors.keySet()) {
      if (index.containsKey(start)) {
        continue;
      }
      Deque<Map.Entry<ARGState, Integer>> work = new ArrayDeque<>();
      work.push(Map.entry(start, 0));
      while (!work.isEmpty()) {
        Map.Entry<ARGState, Integer> frame = work.pop();
        ARGState node = frame.getKey();
        int position = frame.getValue();
        if (position == 0) {
          index.put(node, nextIndex);
          lowlink.put(node, nextIndex);
          nextIndex++;
          stack.push(node);
          onStack.add(node);
        }
        List<ARGState> next = pSuccessors.getOrDefault(node, ImmutableList.of());
        if (position < next.size()) {
          work.push(Map.entry(node, position + 1));
          ARGState successor = next.get(position);
          if (!index.containsKey(successor)) {
            work.push(Map.entry(successor, 0));
          } else if (onStack.contains(successor)) {
            lowlink.put(node, Math.min(lowlink.get(node), index.get(successor)));
          }
          continue;
        }
        if (lowlink.get(node).equals(index.get(node))) {
          List<ARGState> component = new ArrayList<>();
          ARGState member;
          do {
            member = stack.pop();
            onStack.remove(member);
            component.add(member);
          } while (member != node);
          if (component.size() > 1 || next.contains(node)) {
            result.addAll(component);
          }
        }
        if (!work.isEmpty()) {
          ARGState parent = work.peek().getKey();
          lowlink.put(parent, Math.min(lowlink.get(parent), lowlink.get(node)));
        }
      }
    }
    return result;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    super.collectStatistics(pStatsCollection);
    if (predicateRefiner instanceof StatisticsProvider provider) {
      provider.collectStatistics(pStatsCollection);
    }
  }

  @Override
  protected void printRefinerStatistics(StatisticsWriter pWriter) {
    pWriter
        .put(raisedK)
        .put(startedInductions)
        .put(droppedPredicates)
        .put(refinementsWithoutPredicates);
  }
}
