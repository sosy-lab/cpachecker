// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.LoopIterationReportingState;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AdjustableConditionCPA;
import org.sosy_lab.cpachecker.core.interfaces.conditions.ReachedSetAdjustingCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.Automata;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

public final class BMCHelper {

  public static boolean isEndState(AbstractState s) {
    ARGState argState = AbstractStates.extractStateByType(s, ARGState.class);
    return argState != null && argState.getChildren().isEmpty();
  }

  private BMCHelper() {}

  public static BooleanFormula assertAt(
      Iterable<AbstractState> pStates,
      final CandidateInvariant pInvariant,
      final FormulaManagerView pFMGR,
      final PathFormulaManager pPFMGR)
      throws CPATransferException, InterruptedException {
    return assertAt(pStates, pInvariant, pFMGR, pPFMGR, false);
  }

  public static BooleanFormula assertAt(
      Iterable<AbstractState> pStates,
      final CandidateInvariant pInvariant,
      final FormulaManagerView pFMGR,
      final PathFormulaManager pPFMGR,
      boolean pForce)
      throws CPATransferException, InterruptedException {
    return assertAt(
        pStates, pContext -> pInvariant.getFormula(pFMGR, pPFMGR, pContext), pFMGR, pForce);
  }

  public static BooleanFormula assertAt(
      Iterable<AbstractState> pStates, FormulaInContext pInvariant, FormulaManagerView pFMGR)
      throws CPATransferException, InterruptedException {
    return assertAt(pStates, pInvariant, pFMGR, false);
  }

  public static BooleanFormula assertAt(
      Iterable<AbstractState> pStates,
      FormulaInContext pInvariant,
      FormulaManagerView pFMGR,
      boolean pForce)
      throws CPATransferException, InterruptedException {
    List<BooleanFormula> result = new ArrayList<>();
    for (AbstractState abstractState : pStates) {
      result.add(assertAt(abstractState, pInvariant, pFMGR, pForce));
    }
    return pFMGR.getBooleanFormulaManager().and(result);
  }

  private static BooleanFormula assertAt(
      AbstractState pState, FormulaInContext pInvariant, FormulaManagerView pFMGR, boolean pForce)
      throws CPATransferException, InterruptedException {
    PredicateAbstractState pas =
        AbstractStates.extractStateByType(pState, PredicateAbstractState.class);
    PathFormula pathFormula = pas.getPathFormula();
    BooleanFormulaManager bfmgr = pFMGR.getBooleanFormulaManager();
    BooleanFormula stateFormula = pathFormula.getFormula();
    if (bfmgr.isFalse(stateFormula)) {
      return bfmgr.makeTrue();
    }
    SSAMap ssaMap = pathFormula.getSsa().withDefault(1);
    BooleanFormula uninstantiatedFormula = pInvariant.getFormulaInContext(pathFormula);
    BooleanFormula instantiatedFormula = pFMGR.instantiate(uninstantiatedFormula, ssaMap);
    if (pForce) {
      return instantiatedFormula;
    }
    return bfmgr.or(bfmgr.not(stateFormula), instantiatedFormula);
  }

  /**
   * Create a disjunctive formula of all the path formulas in the supplied iterable.
   *
   * @throws InterruptedException if the shutdown notifier signals a shutdown request.
   */
  public static BooleanFormula createFormulaFor(
      Iterable<AbstractState> states, BooleanFormulaManager pBFMGR) throws InterruptedException {
    return createFormulaFor(states, pBFMGR, Optional.empty());
  }

  /**
   * Create a disjunctive formula of all the path formulas in the supplied iterable.
   *
   * @throws InterruptedException if the shutdown notifier signals a shutdown request.
   */
  public static BooleanFormula createFormulaFor(
      Iterable<AbstractState> states,
      BooleanFormulaManager pBFMGR,
      Optional<ShutdownNotifier> pShutdownNotifier)
      throws InterruptedException {

    List<BooleanFormula> pathFormulas = new ArrayList<>();
    for (PredicateAbstractState e :
        AbstractStates.projectToType(states, PredicateAbstractState.class)) {
      if (pShutdownNotifier.isPresent()) {
        pShutdownNotifier.orElseThrow().shutdownIfNecessary();
      }
      // Conjuncting block formula of last abstraction and current path formula
      // works regardless of state is an abstraction state or not.
      BooleanFormula pathFormula =
          pBFMGR.and(
              e.getAbstractionFormula().getBlockFormula().getFormula(),
              e.getPathFormula().getFormula());
      pathFormulas.add(pathFormula);
    }

    return pBFMGR.or(pathFormulas);
  }

  /**
   * Unrolls the given reached set using the algorithm provided to this instance of the bounded
   * model checking algorithm.
   *
   * @param pReachedSet the reached set to unroll.
   * @return {@code true} if the unrolling was sound, {@code false} otherwise.
   * @throws CPAException if an exception occurred during unrolling the reached set.
   * @throws InterruptedException if the unrolling is interrupted.
   */
  public static AlgorithmStatus unroll(
      LogManager pLogger,
      ReachedSet pReachedSet,
      Algorithm pAlgorithm,
      ConfigurableProgramAnalysis pCPA)
      throws CPAException, InterruptedException {
    adjustReachedSet(pLogger, pReachedSet, pCPA);
    return pAlgorithm.run(pReachedSet);
  }

  /**
   * Adjusts the given reached set so that the involved adjustable condition CPAs are able to
   * operate properly without being negatively influenced by states generated earlier under
   * different conditions while trying to retain as many states as possible.
   *
   * @param pReachedSet the reached set to be adjusted.
   */
  public static void adjustReachedSet(
      LogManager pLogger, ReachedSet pReachedSet, ConfigurableProgramAnalysis pCPA)
      throws InterruptedException {
    Preconditions.checkArgument(!pReachedSet.isEmpty());
    CFANode initialLocation = extractLocation(pReachedSet.getFirstState());
    for (AdjustableConditionCPA conditionCPA :
        CPAs.asIterable(pCPA).filter(AdjustableConditionCPA.class)) {
      if (conditionCPA instanceof ReachedSetAdjustingCPA) {
        ((ReachedSetAdjustingCPA) conditionCPA).adjustReachedSet(pReachedSet);
      } else {
        pReachedSet.clear();
        pLogger.log(
            Level.WARNING,
            "Completely clearing the reached set after condition adjustment due to "
                + conditionCPA.getClass()
                + ". This may drastically impede the efficiency of iterative deepening. Implement"
                + " ReachedSetAdjustingCPA to avoid this problem.");
        break;
      }
    }
    if (pReachedSet.isEmpty()) {
      pReachedSet.add(
          pCPA.getInitialState(initialLocation, StateSpacePartition.getDefaultPartition()),
          pCPA.getInitialPrecision(initialLocation, StateSpacePartition.getDefaultPartition()));
    }
  }

  public static Set<CFANode> getLoopHeads(
      CFA pCFA, TargetLocationProvider pTargetLocationProvider) {
    if (pCFA.getLoopStructure().isPresent()
        && pCFA.getLoopStructure().orElseThrow().getAllLoops().isEmpty()) {
      return ImmutableSet.of();
    }
    final Set<CFANode> loopHeads =
        pTargetLocationProvider.tryGetAutomatonTargetLocations(
            pCFA.getMainFunction(),
            Specification.fromAutomata(ImmutableList.of(Automata.getLoopHeadTargetAutomaton())));
    if (!pCFA.getLoopStructure().isPresent()) {
      return loopHeads;
    }
    LoopStructure loopStructure = pCFA.getLoopStructure().orElseThrow();
    return from(loopStructure.getAllLoops())
        .transformAndConcat(
            pLoop -> {
              if (Sets.intersection(pLoop.getLoopNodes(), loopHeads).isEmpty()) {
                return ImmutableSet.of();
              }
              return pLoop.getLoopHeads();
            })
        .toSet();
  }

  public static FluentIterable<AbstractState> filterIterationsBetween(
      Iterable<AbstractState> pStates, int pMinIt, int pMaxIt, Set<CFANode> pLoopHeads) {
    Objects.requireNonNull(pLoopHeads);
    if (pMinIt > pMaxIt) {
      throw new IllegalArgumentException(
          String.format("Minimum (%d) not lower than maximum (%d)", pMinIt, pMaxIt));
    }
    return FluentIterable.from(pStates)
        .filter(
            state -> {
              if (state == null) {
                return false;
              }
              LoopIterationReportingState ls =
                  AbstractStates.extractStateByType(state, LoopIterationReportingState.class);
              if (ls == null) {
                return false;
              }
              int minIt = convertIteration(pMinIt, state, pLoopHeads);
              int maxIt = convertIteration(pMaxIt, state, pLoopHeads);
              int actualIt = ls.getDeepestIteration();
              return minIt <= actualIt && actualIt <= maxIt;
            });
  }

  public static FluentIterable<AbstractState> filterIterationsUpTo(
      Iterable<AbstractState> pStates, int pIteration, Set<CFANode> pLoopHeads) {
    return filterIterationsBetween(pStates, 0, pIteration, pLoopHeads);
  }

  public static FluentIterable<AbstractState> filterIteration(
      Iterable<AbstractState> pStates, int pIteration, Set<CFANode> pLoopHeads) {
    return filterIterationsBetween(pStates, pIteration, pIteration, pLoopHeads);
  }

  private static int convertIteration(
      int pIteration, AbstractState state, Set<CFANode> pLoopHeads) {
    if (pIteration == Integer.MAX_VALUE) {
      throw new IllegalArgumentException(
          String.format(
              "The highest supported value for an iteration count is %d, which is exceeded by %d",
              Integer.MAX_VALUE - 1, pIteration));
    }
    /*
     * We want to consider as an "iteration" i
     * all states with loop-iteration counter i that are
     * - either target states or
     * - not at a loop head
     * and all states with loop-iteration counter i+1
     * that are at a loop head.
     *
     * Reason:
     * 1) A target state that is also a loop head
     * does not count as a loop-head for our purposes,
     * because the error "exits" the loop.
     * 2) It is more convenient to make a loop-head state "belong"
     * to the previous iteration instead of the one it starts.
     */

    return !AbstractStates.isTargetState(state) && hasMatchingLocation(state, pLoopHeads)
        ? pIteration + 1
        : pIteration;
  }

  public static boolean hasMatchingLocation(AbstractState state, Set<CFANode> pLocations) {
    return from(AbstractStates.extractLocations(state)).anyMatch(pLocations::contains);
  }

  public static Set<ARGState> filterAncestors(
      Iterable<ARGState> pStates, Predicate<? super AbstractState> pDescendant) {
    Multimap<ARGState, ARGState> parentToTarget = HashMultimap.create();
    for (ARGState state : FluentIterable.from(pStates).filter(pDescendant::test)) {
      if (state.getChildren().isEmpty()) {
        Collection<ARGState> parents = state.getParents();
        for (ARGState parent : parents) {
          parentToTarget.put(parent, state);
        }
      }
    }
    Set<ARGState> redundantStates = new HashSet<>();
    for (Map.Entry<ARGState, Collection<ARGState>> family : parentToTarget.asMap().entrySet()) {
      ARGState parent = family.getKey();
      Collection<ARGState> children = family.getValue();
      Set<CFAEdge> edges =
          FluentIterable.from(children).transformAndConcat(parent::getEdgesToChild).toSet();
      if (edges.size() == 1 && !(edges.iterator().next() instanceof AssumeEdge)) {
        Iterables.addAll(redundantStates, Iterables.skip(children, 1));
      }
    }
    return redundantStates;
  }

  public static boolean isTrivialSelfLoop(Loop pLoop) {
    Set<CFANode> loopHeads = pLoop.getLoopHeads();
    if (loopHeads.size() != 1) {
      return false;
    }
    CFANode loopHead = loopHeads.iterator().next();
    class TrivialSelfLoopVisitor implements CFAVisitor {

      private boolean valid = true;

      @Override
      public TraversalProcess visitEdge(CFAEdge pEdge) {
        if (!pEdge.getEdgeType().equals(CFAEdgeType.BlankEdge)
            || !pLoop.getLoopNodes().contains(pEdge.getSuccessor())) {
          valid = false;
          return TraversalProcess.ABORT;
        }
        if (pEdge.getSuccessor().equals(loopHead)) {
          return TraversalProcess.SKIP;
        }
        return TraversalProcess.CONTINUE;
      }

      @Override
      public TraversalProcess visitNode(CFANode pNode) {
        return TraversalProcess.CONTINUE;
      }
    }

    TrivialSelfLoopVisitor visitor = new TrivialSelfLoopVisitor();
    CFATraversal.dfs().traverseOnce(loopHead, visitor);
    return visitor.valid;
  }

  public static BooleanFormula disjoinStateViolationAssertions(
      BooleanFormulaManager pBfmgr,
      Multimap<BooleanFormula, BooleanFormula> pSuccessorViolationAssertions) {
    List<BooleanFormula> assertions = new ArrayList<>();
    for (Map.Entry<BooleanFormula, Collection<BooleanFormula>> stateWithViolations :
        pSuccessorViolationAssertions.asMap().entrySet()) {
      assertions.add(
          pBfmgr.and(stateWithViolations.getKey(), pBfmgr.and(stateWithViolations.getValue())));
    }
    return pBfmgr.or(assertions);
  }

  static FluentIterable<AbstractState> filterBmcChecked(
      Iterable<AbstractState> pStates, Set<Object> pCheckedKeys) {
    return FluentIterable.from(pStates)
        .filter(
            pArg0 -> {
              if (pArg0 == null) {
                return false;
              }
              LoopIterationReportingState ls =
                  AbstractStates.extractStateByType(pArg0, LoopIterationReportingState.class);
              return ls != null && pCheckedKeys.contains(ls.getPartitionKey());
            });
  }

  /**
   * Compute all states whose loop-bound state is contained within the checked range of the base
   * case. For example, if we have two loops and loop-bound state (1,0) was checked by BMC. Then
   * loop-bound states (0,-1), (1,0), and (0,0) are contained in the checked range, but (0,1) falls
   * out of the range.
   *
   * @param pStates the set of states to consider.
   * @param pCheckedKeys the loop-bound states checked in the base case.
   * @param pLoops the loops of the CFA.
   * @return all states whose loop-bound state is contained within the checked range of the base
   *     case.
   */
  static FluentIterable<AbstractState> filterBmcCheckedWithin(
      Iterable<AbstractState> pStates, Set<Object> pCheckedKeys, Iterable<Loop> pLoops) {
    return from(pStates).filter(pState -> isWithinBmcCheckedRange(pState, pCheckedKeys, pLoops));
  }

  private static boolean isWithinBmcCheckedRange(
      AbstractState pState, Set<Object> pCheckedKeys, Iterable<Loop> pLoops) {
    LoopIterationReportingState loopState =
        AbstractStates.extractStateByType(pState, LoopIterationReportingState.class);
    if (loopState == null) {
      return false;
    }
    for (Object key : pCheckedKeys) {
      LoopIterationReportingState checkedState = (LoopIterationReportingState) key;
      boolean withinCheckedRange = true;
      for (Loop loop : pLoops) {
        if (loopState.getIteration(loop) > checkedState.getIteration(loop)) {
          withinCheckedRange = false;
          break;
        }
      }
      if (withinCheckedRange) {
        return true;
      }
    }
    return false;
  }

  public interface FormulaInContext {

    BooleanFormula getFormulaInContext(PathFormula pContext)
        throws CPATransferException, InterruptedException;
  }
}
