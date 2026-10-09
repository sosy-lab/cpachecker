// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.induction.refiner;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.getBlockFormula;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.getPredicateState;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.induction.InductionPrecision;
import org.sosy_lab.cpachecker.cpa.induction.InductionPrecision.LoopPrecision;
import org.sosy_lab.cpachecker.cpa.induction.InductionState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision.LocationInstance;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Makes the CEGAR loop behave like PDR. Every visit of a loop head abstracts with its own frame,
 * the location-instance predicates of the visit, and the exploration cuts a path off after the last
 * frame. A spurious counterexample is blocked property-directed: the abstraction closest to the
 * error whose interpolant against the concrete suffix is new gets the interpolant, and so do the
 * frames in front of it. If the exploration reaches no target, the refiner pushes the frontier: the
 * next visit of a cut-off loop head gets a copy of the last frame, and its abstraction keeps what
 * still holds one step later, which is the clause propagation of PDR. As in PDR, the frames only
 * grow, and every exploration uses all of them up to the frontier. The loops close when an
 * abstraction is covered by one in front of it.
 */
@Options(prefix = "cpa.induction.refinement.pdr")
public final class PdrRefiner extends InductionRefiner {

  @Option(
      secure = true,
      description =
          "continue as BMC if no spurious counterexample can be blocked because an interpolation"
              + " failed, as the refiners of IMC and ISMC do")
  private boolean bmcFallback = false;

  private final PredicateAbstractionManager predicateManager;
  private final LoopStructure loopStructure;
  private boolean fallenBackToBmc = false;

  /** The frames of all loop heads, which the refinements extend. */
  private final SetMultimap<LocationInstance, AbstractionPredicate> frames =
      LinkedHashMultimap.create();

  /** The number of frames per loop that the pushes reached. */
  private final Map<Loop, Integer> frontiers = new HashMap<>();

  private final StatCounter blockedCounterexamples = new StatCounter("Blocked counterexamples");
  private final StatCounter pushedFrontiers = new StatCounter("Pushed frontiers");
  private final StatInt frameCount = new StatInt(StatKind.MAX, "Frames per loop head");

  private PdrRefiner(ConfigurableProgramAnalysis pCpa, LogManager pLogger, Configuration pConfig)
      throws InvalidConfigurationException {
    super(pCpa, pLogger, pConfig);
    pConfig.inject(this);
    predicateManager = predicateCpa.getPredicateManager();
    loopStructure = predicateCpa.getCfa().getLoopStructure().orElseThrow();
  }

  public static Refiner create(ConfigurableProgramAnalysis pCpa, LogManager pLogger)
      throws InvalidConfigurationException {
    return new PdrRefiner(pCpa, pLogger, configurationOf(pCpa, PdrRefiner.class));
  }

  @Override
  protected Optional<ARGState> refine(
      ARGReachedSet pReached, List<ARGState> pTargets, List<ARGState> pCutOffs)
      throws CPAException, InterruptedException, SolverException {
    if (fallenBackToBmc) {
      return refineAsBmc(pReached, pTargets, pCutOffs);
    }
    Optional<ARGState> counterexample = findFeasibleTarget(pTargets);
    if (counterexample.isPresent()) {
      return counterexample;
    }

    long failures = failedInterpolations();
    boolean blocked = false;
    boolean unblocked = false;
    for (ARGState target : pTargets) {
      if (target.isDestroyed()) {
        continue;
      }
      if (firstLoopHeadAbstraction(target).isEmpty()
          || solver.isUnsat(lastBlockFromRegion(target))) {
        // concrete, or unreachable from the region in front of it
        removeUnreachable(pReached, ImmutableList.of(target));
      } else if (block(pReached, abstractionChain(target))) {
        blocked = true;
      } else {
        unblocked = true;
      }
    }
    if (blocked) {
      return Optional.empty();
    }
    if (unblocked) {
      if (bmcFallback && failedInterpolations() > failures) {
        logger.log(Level.INFO, "Falling back to BMC, because an interpolation failed");
        restartAsBmc(pReached, 1);
        fallenBackToBmc = true;
        return Optional.empty();
      }
      throw new RefinementFailedException(Reason.InterpolationFailed, null);
    }

    for (ARGState cutOff : pCutOffs) {
      if (!cutOff.isDestroyed()) {
        pushFrontier(pReached, cutOff);
      }
    }
    return Optional.empty();
  }

  /**
   * Strengthen the abstraction closest to the error whose interpolant against the concrete suffix
   * is not in its frame yet, together with the frames in front of it, and explore it again.
   *
   * @return whether an abstraction could be strengthened
   */
  private boolean block(ARGReachedSet pReached, List<ARGState> pChain)
      throws CPAException, InterruptedException {
    for (int i = pChain.size() - 2; i > 0; i--) {
      ARGState abstraction = pChain.get(i);
      if (!inductionStateOf(abstraction).isAbstraction()) {
        continue;
      }
      Optional<BooleanFormula> interpolant = interpolate(pChain, i);
      if (interpolant.isEmpty()) {
        continue;
      }
      CFANode loopHead = inductionStateOf(abstraction).getLoopHead();
      int visit = visitOf(abstraction, loopHead);
      PredicatePrecision precision = predicatePrecisionOf(pReached, abstraction);
      AbstractionPredicate predicate = predicateManager.getPredicateFor(interpolant.orElseThrow());
      if (precision.getPredicates(loopHead, visit).contains(predicate)) {
        continue;
      }
      for (int frame = 1; frame <= visit; frame++) {
        frames.put(new LocationInstance(loopHead, frame), predicate);
      }
      reexploreWithFrames(pReached, abstraction);
      blockedCounterexamples.inc();
      return true;
    }
    return false;
  }

  /**
   * The interpolant between the path from the region in front of the abstraction at the given
   * position of the chain to that abstraction and the concrete suffix to the target.
   */
  private Optional<BooleanFormula> interpolate(List<ARGState> pChain, int pPosition)
      throws CPAException, InterruptedException {
    BooleanFormula prefix =
        bfmgr.and(
            getPredicateState(pChain.get(pPosition - 1))
                .getAbstractionFormula()
                .asInstantiatedFormula(),
            getBlockFormula(getPredicateState(pChain.get(pPosition))));
    BooleanFormula suffix =
        bfmgr.and(
            Lists.transform(
                pChain.subList(pPosition + 1, pChain.size()),
                s -> getBlockFormula(getPredicateState(s))));
    // false blocks an abstraction whose block is infeasible, which an empty frame does not check
    return interpolate(ImmutableList.of(prefix, suffix))
        .map(ImmutableList::getFirst)
        .filter(itp -> !bfmgr.isTrue(itp));
  }

  /**
   * Describe one more visit of the loop head that the given cut-off stopped at, with a copy of the
   * frame of the visit in front of it, and explore the cut-off again.
   */
  private void pushFrontier(ARGReachedSet pReached, ARGState pCutOff) throws InterruptedException {
    InductionState cutOffState = inductionStateOf(pCutOff);
    CFANode loopHead = cutOffState.getLoopHead();
    int visit = cutOffState.getAbstractionIndex(loopHead) + 1;
    frames.putAll(
        new LocationInstance(loopHead, visit),
        ImmutableList.copyOf(frames.get(new LocationInstance(loopHead, visit - 1))));
    for (Loop loop : loopStructure.getLoopsForLoopHead(loopHead)) {
      frontiers.merge(loop, visit, Math::max);
    }
    frameCount.setNextValue(visit);
    reexploreWithFrames(pReached, pCutOff);
    pushedFrontiers.inc();
  }

  /** Explore the given state again with all frames up to the frontiers. */
  private void reexploreWithFrames(ARGReachedSet pReached, ARGState pState)
      throws InterruptedException {
    ARGState parent = pState.getParents().getFirst();
    InductionPrecision inductionPrecision = inductionPrecisionOf(pReached, parent);
    for (Map.Entry<Loop, Integer> frontier : frontiers.entrySet()) {
      LoopPrecision loopPrecision = inductionPrecision.get(frontier.getKey());
      if (loopPrecision.describedAbstractions() != LoopPrecision.ALL_ABSTRACTIONS
          && loopPrecision.describedAbstractions() < frontier.getValue()) {
        inductionPrecision =
            inductionPrecision.with(
                frontier.getKey(), loopPrecision.withDescribedAbstractions(frontier.getValue()));
      }
    }
    reexplore(
        pReached,
        pState,
        ImmutableList.of(
            inductionPrecision, withFrames(predicatePrecisionOf(pReached, parent), frames)),
        ImmutableList.of(
            Predicates.instanceOf(InductionPrecision.class),
            Predicates.instanceOf(PredicatePrecision.class)));
  }

  private static int visitOf(ARGState pAbstraction, CFANode pLoopHead) {
    return checkNotNull(
        getPredicateState(pAbstraction).getAbstractionLocationsOnPath().get(pLoopHead));
  }

  private static PredicatePrecision withFrames(
      PredicatePrecision pPrecision, SetMultimap<LocationInstance, AbstractionPredicate> pFrames) {
    return new PredicatePrecision(
        pFrames,
        pPrecision.getLocalPredicates(),
        pPrecision.getFunctionPredicates(),
        ImmutableSet.copyOf(pPrecision.getGlobalPredicates()));
  }

  @Override
  protected void printRefinerStatistics(StatisticsWriter pWriter) {
    pWriter.put(blockedCounterexamples).put(pushedFrontiers).put(frameCount);
  }
}
