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
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.getBlockFormula;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.getPredicateState;

import com.google.common.base.Predicates;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Makes the CEGAR loop behave like ISMC. The counterexample search unrolls the loops n times with
 * an abstraction to true at every visit of a loop head that covers nothing, so that every iteration
 * is one block, as in the unrolling of native ISMC. The interpolation sequences of the spurious
 * targets over these blocks are conjoined per loop head and visit into the reachability vector. If
 * the vector passes the fixpoint check of native ISMC, the frame phase abstracts every visit of a
 * loop head with all frames as predicates. Boolean abstraction is exact on the frames, so this
 * exploration closes the loops by coverage. Otherwise, the counterexample search continues with
 * bound n+1, which extends the unrolling at its cut-offs.
 */
@Options(prefix = "cpa.induction.refinement.ismc")
public final class IsmcRefiner extends InductionRefiner {

  @Option(
      secure = true,
      description =
          "continue as BMC if the program has more loop heads than bmcFallbackLoopLimit or"
              + " if an interpolation fails, as native ISMC does. Without it, the refiner applies"
              + " ISMC to every loop and ignores failed interpolations.")
  private boolean bmcFallback = true;

  @Option(
      secure = true,
      description = "the number of loop heads up to which the refiner does not fall back to BMC")
  private int bmcFallbackLoopLimit = 1;

  private final PredicateAbstractionManager predicateManager;
  private boolean fallenBackToBmc = false;

  /** The reachability vector per loop head, with the frame of the i-th visit at index i-1. */
  private final Map<CFANode, List<BooleanFormula>> frames = new LinkedHashMap<>();

  /** The number of iterations that the counterexample search unrolls. */
  private int bound = 1;

  private final StatCounter framePhases = new StatCounter("Switches to the frame phase");
  private final StatCounter searches = new StatCounter("Switches to a longer search");
  private final StatInt frameCount = new StatInt(StatKind.MAX, "Frames per loop head");

  private IsmcRefiner(ConfigurableProgramAnalysis pCpa, LogManager pLogger, Configuration pConfig)
      throws InvalidConfigurationException {
    super(pCpa, pLogger, pConfig);
    pConfig.inject(this);
    predicateManager = predicateCpa.getPredicateManager();
  }

  public static Refiner create(ConfigurableProgramAnalysis pCpa, LogManager pLogger)
      throws InvalidConfigurationException {
    return new IsmcRefiner(pCpa, pLogger, configurationOf(pCpa, IsmcRefiner.class));
  }

  @Override
  protected Optional<ARGState> refine(
      ARGReachedSet pReached, List<ARGState> pTargets, List<ARGState> pCutOffs)
      throws CPAException, InterruptedException, SolverException {
    if (fallenBackToBmc) {
      return refineAsBmc(pReached, pTargets, pCutOffs);
    }
    fallenBackToBmc = bmcFallback && fallBackToBmcIfNecessary(pReached, bmcFallbackLoopLimit);
    if (fallenBackToBmc) {
      return Optional.empty();
    }

    Optional<ARGState> counterexample = findFeasibleTarget(pTargets);
    if (counterexample.isPresent()) {
      return counterexample;
    }
    List<ARGState> searchTargets = new ArrayList<>();
    List<ARGState> frameTargets = new ArrayList<>();
    List<ARGState> concreteTargets = new ArrayList<>();
    for (ARGState target : pTargets) {
      Optional<ARGState> first = firstLoopHeadAbstraction(target);
      if (first.isEmpty()) {
        concreteTargets.add(target);
      } else if (inductionStateOf(first.orElseThrow()).getLoopPrecision().unrolling()) {
        searchTargets.add(target);
      } else {
        frameTargets.add(target);
      }
    }
    // the forward condition of native ISMC: if the search reaches no cut-off, it is complete
    if (pCutOffs.isEmpty() || removeIfNoneReachable(pReached, pCutOffs)) {
      removeUnreachable(pReached, searchTargets);
      searchTargets.clear();
    }
    List<ARGState> failedFrameTargets = new ArrayList<>();
    for (ARGState target : frameTargets) {
      if (solver.isUnsat(lastBlockFromRegion(target))) {
        removeUnreachable(pReached, ImmutableList.of(target));
      } else {
        failedFrameTargets.add(target);
      }
    }
    removeUnreachable(pReached, concreteTargets);
    if (searchTargets.isEmpty()
        && failedFrameTargets.isEmpty()
        && from(pCutOffs).allMatch(ARGState::isDestroyed)) {
      return Optional.empty();
    }

    addToFrames(searchTargets);
    if (bmcFallback && hasFailedInterpolation()) {
      logger.log(Level.INFO, "Falling back to BMC, because an interpolation failed");
      restartAsBmc(pReached, bound + 1);
      fallenBackToBmc = true;
      return Optional.empty();
    }
    List<ARGState> searchStates =
        from(searchTargets).append(pCutOffs).filter(state -> !state.isDestroyed()).toList();
    // Only a search changes the frames, so only a search can go on with the frame phase. It does
    // if the fixpoint check of native ISMC succeeds.
    boolean framePhase = !searchStates.isEmpty() && framesClose();
    if (framePhase) {
      framePhases.inc();
      for (ARGState root : rootsOf(searchStates)) {
        reexploreWith(pReached, root, framePrecision(pReached, root), framePredicates());
      }
    }
    if (failedFrameTargets.isEmpty() && (framePhase || searchStates.isEmpty())) {
      return Optional.empty();
    }

    // As native ISMC after a failed fixpoint check, search with a larger bound: extend a search at
    // its cut-offs, and search again where a frame phase did not close.
    bound++;
    searches.inc();
    if (!framePhase) {
      removeUnreachable(pReached, searchTargets);
      for (ARGState cutOff : pCutOffs) {
        if (!cutOff.isDestroyed()) {
          reexploreWith(
              pReached, cutOff, searchPrecision(pReached, cutOff), PredicatePrecision.empty());
        }
      }
    }
    for (ARGState root : rootsOf(failedFrameTargets)) {
      reexploreWith(pReached, root, searchPrecision(pReached, root), PredicatePrecision.empty());
    }
    return Optional.empty();
  }

  /**
   * Conjoin the interpolation sequences of the deepest targets of each search to the frames, as
   * native ISMC interpolates the unrolling of the largest bound. The targets behind the same
   * abstraction share one sequence, whose last formula is the disjunction of their blocks.
   */
  private void addToFrames(List<ARGState> pSearchTargets)
      throws CPAException, InterruptedException, SolverException {
    ListMultimap<ARGState, ARGState> targetsBehind = ArrayListMultimap.create();
    Map<ARGState, Integer> deepest = new LinkedHashMap<>();
    for (ARGState target : pSearchTargets) {
      if (!target.isDestroyed()) {
        List<ARGState> chain = abstractionChain(target);
        ARGState last = chain.get(chain.size() - 2);
        targetsBehind.put(last, target);
        deepest.merge(firstLoopHeadAbstraction(target).orElseThrow(), chain.size(), Math::max);
      }
    }
    for (ARGState last : targetsBehind.keySet()) {
      List<ARGState> chain = abstractionChain(last);
      if (chain.size() + 1 == deepest.get(firstLoopHeadAbstraction(last).orElseThrow())) {
        addToFrames(chain, targetsBehind.get(last));
      }
    }
  }

  private void addToFrames(List<ARGState> pChain, List<ARGState> pTargets)
      throws CPAException, InterruptedException, SolverException {
    List<BooleanFormula> formulas =
        from(pChain)
            .skip(1)
            .transform(s -> getBlockFormula(getPredicateState(s)))
            .append(bfmgr.or(Lists.transform(pTargets, t -> getBlockFormula(getPredicateState(t)))))
            .toList();
    Optional<ImmutableList<BooleanFormula>> interpolants = interpolate(formulas);
    // a false interpolant shows an infeasible path through the loops, whose frames say nothing
    if (interpolants.isEmpty() || interpolants.orElseThrow().stream().anyMatch(bfmgr::isFalse)) {
      return;
    }
    // the interpolant with index i lies behind the abstraction with index i+1 of the chain
    for (int i = 0; i < interpolants.orElseThrow().size(); i++) {
      ARGState abstraction = pChain.get(i + 1);
      if (!inductionStateOf(abstraction).isAbstraction()) {
        continue;
      }
      CFANode loopHead = inductionStateOf(abstraction).getLoopHead();
      int visit =
          checkNotNull(
              getPredicateState(abstraction).getAbstractionLocationsOnPath().get(loopHead));
      BooleanFormula interpolant = fmgr.uninstantiate(interpolants.orElseThrow().get(i));
      List<BooleanFormula> vector = frames.computeIfAbsent(loopHead, l -> new ArrayList<>());
      while (vector.size() < visit) {
        vector.add(bfmgr.makeTrue());
      }
      vector.set(visit - 1, bfmgr.and(vector.get(visit - 1), interpolant));
      frameCount.setNextValue(vector.size());
    }
  }

  /** The first abstractions at a loop head on the paths to the given states. */
  private static Set<ARGState> rootsOf(Iterable<ARGState> pStates) {
    Set<ARGState> roots = new LinkedHashSet<>();
    for (ARGState state : pStates) {
      if (!state.isDestroyed()) {
        roots.add(firstLoopHeadAbstraction(state).orElseThrow());
      }
    }
    return roots;
  }

  /**
   * Whether for every loop head some frame is implied by the disjunction of the frames in front of
   * it, which is the fixpoint check of native ISMC.
   */
  private boolean framesClose() throws SolverException, InterruptedException {
    if (frames.isEmpty()) {
      return false;
    }
    for (List<BooleanFormula> vector : frames.values()) {
      if (!closes(vector)) {
        return false;
      }
    }
    return true;
  }

  private boolean closes(List<BooleanFormula> pVector)
      throws SolverException, InterruptedException {
    for (int j = 1; j < pVector.size(); j++) {
      if (solver.implies(pVector.get(j), bfmgr.or(pVector.subList(0, j)))) {
        return true;
      }
    }
    return false;
  }

  private InductionPrecision framePrecision(ARGReachedSet pReached, ARGState pRoot) {
    return inductionPrecisionOf(pReached, pRoot.getParents().getFirst())
        .withAll(
            p ->
                new LoopPrecision(
                    0,
                    /* induction= */ false,
                    LoopPrecision.ALL_ABSTRACTIONS,
                    /* unrolling= */ false));
  }

  private InductionPrecision searchPrecision(ARGReachedSet pReached, ARGState pRoot) {
    return inductionPrecisionOf(pReached, pRoot.getParents().getFirst())
        .withAll(p -> new LoopPrecision(0, /* induction= */ false, bound, /* unrolling= */ true));
  }

  /** Every frame of a loop head as a predicate of all its visits. */
  private PredicatePrecision framePredicates() {
    SetMultimap<CFANode, AbstractionPredicate> predicates = LinkedHashMultimap.create();
    for (Map.Entry<CFANode, List<BooleanFormula>> entry : frames.entrySet()) {
      for (BooleanFormula frame : entry.getValue()) {
        if (!bfmgr.isTrue(frame)) {
          predicates.put(entry.getKey(), predicateManager.getPredicateFor(frame));
        }
      }
    }
    return PredicatePrecision.empty().addLocalPredicates(predicates.entries());
  }

  private void reexploreWith(
      ARGReachedSet pReached,
      ARGState pRoot,
      InductionPrecision pInductionPrecision,
      PredicatePrecision pPredicatePrecision)
      throws InterruptedException {
    recordK(pInductionPrecision);
    reexplore(
        pReached,
        pRoot,
        ImmutableList.of(pInductionPrecision, pPredicatePrecision),
        ImmutableList.of(
            Predicates.instanceOf(InductionPrecision.class),
            Predicates.instanceOf(PredicatePrecision.class)));
  }

  @Override
  protected void printRefinerStatistics(StatisticsWriter pWriter) {
    pWriter
        .put(framePhases)
        .put(searches)
        .put(frameCount)
        .put("Bound of the counterexample search", bound);
  }
}
