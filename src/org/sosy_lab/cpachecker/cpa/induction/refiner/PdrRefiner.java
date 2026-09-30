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
import java.util.List;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.induction.InductionPrecision;
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
 * still holds one step later, which is the clause propagation of PDR. The loops close when an
 * abstraction is covered by one in front of it.
 */
public final class PdrRefiner extends InductionRefiner {

  private final PredicateAbstractionManager predicateManager;
  private final LoopStructure loopStructure;

  private final StatCounter blockedCounterexamples = new StatCounter("Blocked counterexamples");
  private final StatCounter pushedFrontiers = new StatCounter("Pushed frontiers");
  private final StatCounter interpolationFailures = new StatCounter("Failed interpolations");
  private final StatInt frameCount = new StatInt(StatKind.MAX, "Frames per loop head");

  private PdrRefiner(ConfigurableProgramAnalysis pCpa, LogManager pLogger, Configuration pConfig)
      throws InvalidConfigurationException {
    super(pCpa, pLogger, pConfig);
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
    Optional<ARGState> counterexample = findFeasibleTarget(pTargets);
    if (counterexample.isPresent()) {
      return counterexample;
    }

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
      SetMultimap<LocationInstance, AbstractionPredicate> frames =
          LinkedHashMultimap.create(precision.getLocationInstancePredicates());
      for (int frame = 1; frame <= visit; frame++) {
        frames.put(new LocationInstance(loopHead, frame), predicate);
      }
      reexplore(
          pReached,
          abstraction,
          ImmutableList.of(withFrames(precision, frames)),
          ImmutableList.of(Predicates.instanceOf(PredicatePrecision.class)));
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
    Optional<ImmutableList<BooleanFormula>> interpolants;
    try {
      interpolants = interpolationManager.interpolate(ImmutableList.of(prefix, suffix));
    } catch (RefinementFailedException e) {
      logger.logDebugException(e, "Interpolation failed");
      interpolationFailures.inc();
      return Optional.empty();
    }
    // false blocks an abstraction whose block is infeasible, which an empty frame does not check
    return interpolants.map(ImmutableList::getFirst).filter(itp -> !bfmgr.isTrue(itp));
  }

  /**
   * Describe one more visit of the loop head that the given cut-off stopped at, with a copy of the
   * frame of the visit in front of it, and explore the cut-off again.
   */
  private void pushFrontier(ARGReachedSet pReached, ARGState pCutOff) throws InterruptedException {
    ARGState parent = pCutOff.getParents().getFirst();
    InductionState cutOffState = inductionStateOf(pCutOff);
    CFANode loopHead = cutOffState.getLoopHead();
    int visit = cutOffState.getAbstractionIndex(loopHead) + 1;

    PredicatePrecision precision = predicatePrecisionOf(pReached, parent);
    SetMultimap<LocationInstance, AbstractionPredicate> frames =
        LinkedHashMultimap.create(precision.getLocationInstancePredicates());
    frames.putAll(
        new LocationInstance(loopHead, visit),
        precision.getLocationInstancePredicates().get(new LocationInstance(loopHead, visit - 1)));

    InductionPrecision inductionPrecision = inductionPrecisionOf(pReached, parent);
    for (Loop loop : loopStructure.getLoopsForLoopHead(loopHead)) {
      inductionPrecision =
          inductionPrecision.with(
              loop,
              inductionPrecision
                  .get(loop)
                  .withDescribedAbstractions(
                      Math.max(visit, inductionPrecision.get(loop).describedAbstractions())));
    }
    frameCount.setNextValue(visit);
    reexplore(
        pReached,
        pCutOff,
        ImmutableList.of(inductionPrecision, withFrames(precision, frames)),
        ImmutableList.of(
            Predicates.instanceOf(InductionPrecision.class),
            Predicates.instanceOf(PredicatePrecision.class)));
    pushedFrontiers.inc();
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
    pWriter
        .put(blockedCounterexamples)
        .put(pushedFrontiers)
        .put(interpolationFailures)
        .put(frameCount);
  }
}
