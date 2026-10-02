// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.induction.refiner;

import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.getBlockFormula;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.getPredicateState;

import com.google.common.base.Predicates;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.sosy_lab.cpachecker.cpa.induction.InductionState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Makes the CEGAR loop behave like IMC. Every abstraction at a loop head uses one predicate φ, the
 * disjunction of the interpolants learned for the loop head, and the exploration closes a loop when
 * an abstraction is covered by the one before it, which is the fixpoint check of IMC. For a
 * spurious target, the refiner widens φ at the abstraction closest to the error whose interpolant
 * of the path in front against the concrete suffixes of the targets below it is not implied by φ.
 * If no abstraction can be widened, k increases by one and φ starts over, as the abort of
 * McMillan's algorithm does.
 */
@Options(prefix = "cpa.induction.refinement.imc")
public final class ImcRefiner extends InductionRefiner {

  @Option(
      secure = true,
      description =
          "continue as BMC if the program has more loop heads than bmcFallbackLoopLimit,"
              + " as native IMC does. Without it, the refiner applies IMC to every loop.")
  private boolean bmcFallback = true;

  @Option(
      secure = true,
      description = "the number of loop heads up to which the refiner does not fall back to BMC")
  private int bmcFallbackLoopLimit = 1;

  private final PredicateAbstractionManager predicateManager;
  private final LoopStructure loopStructure;
  private boolean fallenBackToBmc = false;

  private final StatCounter widenings = new StatCounter("Widened abstractions");
  private final StatCounter restarts = new StatCounter("Restarts with a larger k");

  private ImcRefiner(ConfigurableProgramAnalysis pCpa, LogManager pLogger, Configuration pConfig)
      throws InvalidConfigurationException {
    super(pCpa, pLogger, pConfig);
    pConfig.inject(this);
    predicateManager = predicateCpa.getPredicateManager();
    loopStructure = predicateCpa.getCfa().getLoopStructure().orElseThrow();
  }

  public static Refiner create(ConfigurableProgramAnalysis pCpa, LogManager pLogger)
      throws InvalidConfigurationException {
    return new ImcRefiner(pCpa, pLogger, configurationOf(pCpa, ImcRefiner.class));
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
    if (!pCutOffs.isEmpty()) {
      // As native IMC, BMC and the forward condition up to the bound of the cut-offs, then IMC
      Optional<ARGState> counterexample = findFeasibleTarget(pTargets);
      if (counterexample.isPresent()) {
        return counterexample;
      }
      removeUnreachable(pReached, pTargets);
      if (!removeIfNoneReachable(pReached, pCutOffs)) {
        restartAsImc(pReached);
      }
      return Optional.empty();
    }

    Optional<ARGState> counterexample = findFeasibleTarget(pTargets);
    if (counterexample.isPresent()) {
      return counterexample;
    }
    List<ARGState> abstractTargets = new ArrayList<>();
    List<ARGState> concreteTargets = new ArrayList<>();
    Set<ARGState> firstAbstractions = new LinkedHashSet<>();
    for (ARGState target : pTargets) {
      Optional<ARGState> first = firstLoopHeadAbstraction(target);
      if (first.isPresent()) {
        abstractTargets.add(target);
        firstAbstractions.add(first.orElseThrow());
      } else {
        concreteTargets.add(target);
      }
    }
    removeUnreachable(pReached, concreteTargets);
    // the forward condition of native IMC
    if (abstractTargets.isEmpty() || removeIfNoneReachable(pReached, firstAbstractions)) {
      return Optional.empty();
    }

    Map<ARGState, List<ARGState>> chains = new HashMap<>();
    ListMultimap<ARGState, ARGState> targetsBelow = ArrayListMultimap.create();
    for (ARGState target : abstractTargets) {
      List<ARGState> chain = abstractionChain(target);
      chains.put(target, chain);
      for (ARGState state : chain.subList(1, chain.size() - 1)) {
        targetsBelow.put(state, target);
      }
    }
    for (ARGState target : abstractTargets) {
      if (!target.isDestroyed()) {
        refine(pReached, target, chains, targetsBelow);
      }
    }
    return Optional.empty();
  }

  /**
   * Widen the abstraction closest to the error on the path to the given target whose interpolant is
   * not implied by φ, or explore again with a larger k. As native IMC interpolates against all
   * error paths of its unrolling, the interpolant is taken against the suffixes of all targets
   * below the abstraction, or against the own suffix if one of the others is reachable.
   */
  private void refine(
      ARGReachedSet pReached,
      ARGState pTarget,
      Map<ARGState, List<ARGState>> pChains,
      ListMultimap<ARGState, ARGState> pTargetsBelow)
      throws CPAException, InterruptedException, SolverException {
    if (solver.isUnsat(lastBlockFromRegion(pTarget))) {
      removeUnreachable(pReached, ImmutableList.of(pTarget));
      return;
    }
    List<ARGState> chain = pChains.get(pTarget);
    for (int i = chain.size() - 2; i > 0; i--) {
      ARGState abstraction = chain.get(i);
      if (inductionStateOf(abstraction).isAbstraction()) {
        BooleanFormula ownSuffix = suffixOf(chain, i);
        List<BooleanFormula> suffixes = new ArrayList<>();
        suffixes.add(ownSuffix);
        // all chains through the abstraction share the path to it, so it has the same position
        for (ARGState target : pTargetsBelow.get(abstraction)) {
          if (target != pTarget && !target.isDestroyed()) {
            suffixes.add(suffixOf(pChains.get(target), i));
          }
        }
        Optional<BooleanFormula> interpolant = interpolate(chain, i, bfmgr.or(suffixes));
        if (interpolant.isEmpty() && suffixes.size() > 1) {
          interpolant = interpolate(chain, i, ownSuffix);
        }
        if (interpolant.isPresent() && widen(pReached, abstraction, interpolant.orElseThrow())) {
          return;
        }
      }
    }
    restartWithLargerK(pReached, chain);
  }

  /** The concrete path after the abstraction at the given position of the chain. */
  private BooleanFormula suffixOf(List<ARGState> pChain, int pPosition) {
    return bfmgr.and(
        Lists.transform(
            pChain.subList(pPosition + 1, pChain.size()),
            s -> getBlockFormula(getPredicateState(s))));
  }

  /**
   * The interpolant between the path from the region in front of the abstraction at the given
   * position of the chain to that abstraction and the given suffix, without SSA indices.
   */
  private Optional<BooleanFormula> interpolate(
      List<ARGState> pChain, int pPosition, BooleanFormula pSuffix)
      throws CPAException, InterruptedException {
    BooleanFormula prefix =
        bfmgr.and(
            getPredicateState(pChain.get(pPosition - 1))
                .getAbstractionFormula()
                .asInstantiatedFormula(),
            getBlockFormula(getPredicateState(pChain.get(pPosition))));
    return interpolate(ImmutableList.of(prefix, pSuffix))
        .map(itps -> fmgr.uninstantiate(itps.getFirst()))
        .filter(itp -> !bfmgr.isTrue(itp));
  }

  /**
   * Widen the predicate of the loop head of the given abstraction with the interpolant and explore
   * the abstraction again, unless the predicate implies the interpolant already.
   */
  private boolean widen(ARGReachedSet pReached, ARGState pAbstraction, BooleanFormula pInterpolant)
      throws InterruptedException, SolverException {
    CFANode loopHead = inductionStateOf(pAbstraction).getLoopHead();
    PredicatePrecision precision = predicatePrecisionOf(pReached, pAbstraction);
    Set<AbstractionPredicate> current = precision.getLocalPredicates().get(loopHead);
    BooleanFormula widened;
    if (current.isEmpty()) {
      widened = pInterpolant;
    } else {
      BooleanFormula approximation = Iterables.getOnlyElement(current).getSymbolicAtom();
      if (solver.implies(pInterpolant, approximation)) {
        return false;
      }
      // a flat disjunction, so that the formula does not nest deeper with every widening
      Set<BooleanFormula> disjuncts =
          new LinkedHashSet<>(bfmgr.toDisjunctionArgs(approximation, false));
      disjuncts.add(pInterpolant);
      widened = bfmgr.or(disjuncts);
    }
    SetMultimap<CFANode, AbstractionPredicate> localPredicates =
        LinkedHashMultimap.create(precision.getLocalPredicates());
    localPredicates.replaceValues(
        loopHead, ImmutableSet.of(predicateManager.getPredicateFor(widened)));
    PredicatePrecision widenedPrecision =
        new PredicatePrecision(
            precision.getLocationInstancePredicates(),
            localPredicates,
            precision.getFunctionPredicates(),
            precision.getGlobalPredicates());
    logger.log(Level.FINE, "Widening the abstraction at", loopHead, "to", widened);
    reexplore(
        pReached,
        pAbstraction,
        ImmutableList.of(widenedPrecision),
        ImmutableList.of(Predicates.instanceOf(PredicatePrecision.class)));
    widenings.inc();
    return true;
  }

  /** Explore again from the root with an abstraction at every arrival at a loop head. */
  private void restartAsImc(ARGReachedSet pReached) throws InterruptedException {
    ARGState root = (ARGState) pReached.asReachedSet().getFirstState();
    InductionPrecision precision =
        inductionPrecisionOf(pReached, root)
            .withAll(
                p ->
                    new LoopPrecision(
                        /* k= */ 0,
                        /* induction= */ false,
                        LoopPrecision.ALL_ABSTRACTIONS,
                        /* unrolling= */ false));
    recordK(precision);
    pReached.removeAllExceptFirstStateWithPrecision(
        ImmutableList.of(precision, PredicatePrecision.empty()),
        ImmutableList.of(
            Predicates.instanceOf(InductionPrecision.class),
            Predicates.instanceOf(PredicatePrecision.class)));
  }

  /**
   * Explore again from the first abstraction at a loop head on the path, with k increased for the
   * loops that the path abstracts at and without predicates.
   */
  private void restartWithLargerK(ARGReachedSet pReached, List<ARGState> pChain)
      throws InterruptedException {
    ARGState first = firstLoopHeadAbstraction(pChain.getLast()).orElseThrow();
    InductionPrecision precision = inductionPrecisionOf(pReached, first.getParents().getFirst());
    Set<Loop> loops = new LinkedHashSet<>();
    for (ARGState state : pChain) {
      InductionState inductionState = inductionStateOf(state);
      if (inductionState.isAbstraction()) {
        loops.addAll(loopStructure.getLoopsForLoopHead(inductionState.getLoopHead()));
      }
    }
    for (Loop loop : loops) {
      precision = precision.with(loop, precision.get(loop).withK(precision.get(loop).k() + 1));
    }
    recordK(precision);
    logger.log(Level.FINE, "Restarting at", first, "with", precision);
    reexplore(
        pReached,
        first,
        ImmutableList.of(precision, PredicatePrecision.empty()),
        ImmutableList.of(
            Predicates.instanceOf(InductionPrecision.class),
            Predicates.instanceOf(PredicatePrecision.class)));
    restarts.inc();
  }

  @Override
  protected void printRefinerStatistics(StatisticsWriter pWriter) {
    pWriter.put(widenings).put(restarts);
  }
}
