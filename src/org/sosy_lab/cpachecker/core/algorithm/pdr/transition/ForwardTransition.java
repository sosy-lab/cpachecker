/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.pdr.transition;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class ForwardTransition {

  private static final Predicate<AbstractState> IS_BLOCK_START =
      PredicateAbstractState.CONTAINS_ABSTRACTION_STATE;

  private final Algorithm algorithm;

  private final ConfigurableProgramAnalysis cpa;

  private final ReachedSetFactory reachedSetFactory;

  private final PathFormulaManager pathFormulaManager;

  private final Map<CFANode, Iterable<Block>> blocks = Maps.newHashMap();

  private final CFA cfa;

  public ForwardTransition(
      ReachedSetFactory pReachedSetFactory,
      ConfigurableProgramAnalysis pCPA,
      Algorithm pAlgorithm,
      CFA pCFA)
      throws InvalidConfigurationException {

    reachedSetFactory = pReachedSetFactory;

    cpa = pCPA;

    PredicateCPA predicateCPA = CPAs.retrieveCPA(cpa, PredicateCPA.class);
    if (predicateCPA == null) {
      throw new InvalidConfigurationException(
          "PredicateCPA required for transitions in the PDRAlgorithm");
    }
    pathFormulaManager = predicateCPA.getPathFormulaManager();

    ARGCPA argCPA = CPAs.retrieveCPA(cpa, ARGCPA.class);
    if (argCPA == null) {
      throw new InvalidConfigurationException(
          "ARGCPA required for transitions in the PDRAlgorithm");
    }

    algorithm = pAlgorithm;
    cfa = pCFA;
  }

  /**
   * Gets all blocks from the given predecessor to successor locations.
   *
   * <p>A cache will be used to store results and retrieve previously computed blocks from.
   *
   * @param pPredecessorLocation the predecessor location of the resulting blocks.
   * @return all blocks from the given predecessor to successor locations.
   * @throws CPAException if the analysis creating the blocks encounters an exception.
   * @throws InterruptedException if block creation was interrupted.
   */
  public FluentIterable<Block> getBlocksFrom(CFANode pPredecessorLocation)
      throws CPAException, InterruptedException {
    if (blocks.isEmpty()) {
      computeBlocks();
    }
    Iterable<Block> result = blocks.get(pPredecessorLocation);
    if (result == null) {
      return FluentIterable.of();
    }
    return FluentIterable.from(result);
  }

  private void computeBlocks() throws CPAException, InterruptedException {
    ReachedSet reachedSet = reachedSetFactory.create();
    initializeFor(reachedSet, cfa.getMainFunction());
    AbstractState initialState = reachedSet.getFirstState();
    while (reachedSet.hasWaitingState()) {
      algorithm.run(reachedSet);
    }

    Function<ARGState, AbstractState> asAbstractState = asAbstractState(reachedSet);

    Multimap<CFANode, Block> blocks = HashMultimap.create();

    Set<AbstractState> visitedPredecessorStates = Sets.newHashSet();
    Deque<AbstractState> predecessorQueue = Queues.newArrayDeque();
    Deque<AbstractState> currentStateQueue = Queues.newArrayDeque();
    visitedPredecessorStates.add(initialState);
    predecessorQueue.offer(initialState);
    currentStateQueue.offer(initialState);

    while (!currentStateQueue.isEmpty()) {
      assert currentStateQueue.size() == predecessorQueue.size();
      AbstractState currentState = currentStateQueue.poll();
      AbstractState currentPredecessor = predecessorQueue.poll();
      ARGState currentStateAsARGState =
          AbstractStates.extractStateByType(currentState, ARGState.class);

      for (ARGState childARGState : currentStateAsARGState.getChildren()) {
        AbstractState child = asAbstractState.apply(childARGState);
        if (child != null) {
          boolean isBlockStart = IS_BLOCK_START.apply(child);
          boolean isBlockEnd = isBlockStart || childARGState.getChildren().isEmpty();
          if (isBlockEnd) {
            for (CFANode predecessorLocation :
                AbstractStates.extractLocations(currentPredecessor)) {
              blocks.put(
                  predecessorLocation,
                  new BlockImpl(
                      currentPredecessor,
                      child,
                      AnalysisDirection.FORWARD,
                      getReachedSet(currentPredecessor, child, reachedSet, asAbstractState)));
            }
          }
          if (!isBlockStart || visitedPredecessorStates.add(child)) {
            predecessorQueue.offer(isBlockStart ? child : currentPredecessor);
            currentStateQueue.offer(child);
          }
        }
      }
    }
    this.blocks.putAll(blocks.asMap());
  }

  private Function<ARGState, AbstractState> asAbstractState(ReachedSet reachedSet) {
    Function<ARGState, AbstractState> asAbstractState;
    if (!reachedSet.isEmpty() && reachedSet.getFirstState() instanceof ARGState) {
      asAbstractState = a -> a;
    } else {
      Map<ARGState, AbstractState> statesByARGState =
          FluentIterable.from(reachedSet).uniqueIndex(AbstractStates.toState(ARGState.class));
      asAbstractState = a -> statesByARGState.get(a);
    }
    return asAbstractState;
  }

  private void initializeFor(ReachedSet pReachedSet, CFANode pInitialLocation)
      throws InterruptedException {
    StateSpacePartition defaultPartition = StateSpacePartition.getDefaultPartition();
    AbstractState initialState = cpa.getInitialState(pInitialLocation, defaultPartition);
    Precision initialPrecision = cpa.getInitialPrecision(pInitialLocation, defaultPartition);
    pReachedSet.add(initialState, initialPrecision);
  }

  private class BlockImpl implements Block {

    private final AbstractState firstState;

    private final AbstractState lastState;

    private final AnalysisDirection direction;

    private final ReachedSet reachedSet;

    private BlockImpl(
        AbstractState pFirstState,
        AbstractState pLastState,
        AnalysisDirection pDirection,
        ReachedSet pReachedSet) {
      firstState = Objects.requireNonNull(pFirstState);
      lastState = Objects.requireNonNull(pLastState);
      direction = Objects.requireNonNull(pDirection);
      reachedSet = Objects.requireNonNull(pReachedSet);
      assert AbstractStates.extractStateByType(pFirstState, PredicateAbstractState.class) != null
              && AbstractStates.extractStateByType(pLastState, PredicateAbstractState.class) != null
          : "PredicateAbstractState required for extracting the block formula.";
    }

    @Override
    public AbstractState getPredecessor() {
      return direction == AnalysisDirection.BACKWARD ? lastState : firstState;
    }

    @Override
    public AbstractState getSuccessor() {
      return direction == AnalysisDirection.BACKWARD ? firstState : lastState;
    }

    private PredicateAbstractState getLastPredicateAbstractState() {
      return AbstractStates.extractStateByType(lastState, PredicateAbstractState.class);
    }

    private PredicateAbstractState getPredecessorPredicateAbstractState() {
      return AbstractStates.extractStateByType(getPredecessor(), PredicateAbstractState.class);
    }

    private PredicateAbstractState getSuccessorPredicateAbstractState() {
      return AbstractStates.extractStateByType(getSuccessor(), PredicateAbstractState.class);
    }

    private PathFormula getPathFormula() {
      return getLastPredicateAbstractState().getAbstractionFormula().getBlockFormula();
    }

    @Override
    public BooleanFormula getFormula() {
      return getPathFormula().getFormula();
    }

    @Override
    public PathFormula getUnprimedContext() {
      PredicateAbstractState pas = getPredecessorPredicateAbstractState();
      return asContext(pas.getAbstractionFormula().getBlockFormula());
    }

    @Override
    public PathFormula getPrimedContext() {
      PredicateAbstractState pas = getSuccessorPredicateAbstractState();
      return asContext(pas.getAbstractionFormula().getBlockFormula());
    }

    private PathFormula asContext(PathFormula pPathFormula) {
      PathFormula fullFormulaWithDefault =
          new PathFormula(
              pPathFormula.getFormula(),
              pPathFormula.getSsa().withDefault(1),
              pPathFormula.getPointerTargetSet(),
              pPathFormula.getLength());
      return pathFormulaManager.makeEmptyPathFormula(fullFormulaWithDefault);
    }

    @Override
    public AnalysisDirection getDirection() {
      return direction;
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }
      if (pObj instanceof BlockImpl) {
        BlockImpl other = (BlockImpl) pObj;
        return direction == other.direction
            && firstState.equals(other.firstState)
            && lastState.equals(other.lastState)
            && reachedSet.equals(other.reachedSet);
      }
      return false;
    }

    @Override
    public boolean equalsIgnoreReachedSet(Object pObj) {
      if (this == pObj) {
        return true;
      }
      if (pObj instanceof BlockImpl) {
        BlockImpl other = (BlockImpl) pObj;
        return direction == other.direction
            && firstState.equals(other.firstState)
            && lastState.equals(other.lastState);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(direction, firstState, lastState, reachedSet);
    }

    @Override
    public String toString() {
      return ForwardTransition.toString(this);
    }

    @Override
    public CFANode getPredecessorLocation() {
      return AbstractStates.extractLocation(getPredecessor());
    }

    @Override
    public CFANode getSuccessorLocation() {
      return AbstractStates.extractLocation(getSuccessor());
    }

    @Override
    public ReachedSet getReachedSet() {
      return reachedSet;
    }
  }

  private ReachedSet getReachedSet(
      AbstractState pInitialState,
      AbstractState pTargetState,
      ReachedSet pOriginalReachedSet,
      Function<ARGState, AbstractState> pAsAbstractState) {
    ReachedSet reachedSet = reachedSetFactory.create();

    for (AbstractState state : getBlockStates(pInitialState, pTargetState, pAsAbstractState)) {
      Precision precision =
          pOriginalReachedSet.contains(state)
              ? pOriginalReachedSet.getPrecision(state)
              : pOriginalReachedSet.getPrecision(pOriginalReachedSet.getFirstState());
      reachedSet.add(state, precision);
      reachedSet.removeOnlyFromWaitlist(state);
    }

    return reachedSet;
  }

  private static String toString(Block pBlock) {
    return String.format(
        "Block from %s to %s with %s formula %s",
        pBlock.getPredecessorLocation(),
        pBlock.getSuccessorLocation(),
        pBlock.getDirection() == AnalysisDirection.BACKWARD ? "backward" : "forward",
        pBlock.getFormula());
  }

  private static Set<AbstractState> getBlockStates(
      AbstractState pInitialState,
      AbstractState pBlockStartState,
      Function<ARGState, AbstractState> pAsAbstractState) {
    Deque<AbstractState> waitlist = Queues.newArrayDeque();
    waitlist.push(pInitialState);

    Set<AbstractState> forward = Sets.newLinkedHashSet();
    forward.add(pInitialState);

    while (!waitlist.isEmpty()) {
      AbstractState currentState = waitlist.pop();
      if (pBlockStartState != currentState) {
        ARGState currentARGState = AbstractStates.extractStateByType(currentState, ARGState.class);
        for (ARGState childARGState : currentARGState.getChildren()) {
          AbstractState childAbstractState = pAsAbstractState.apply(childARGState);
          assert childAbstractState != null;
          if (forward.add(childAbstractState)) {
            waitlist.push(childAbstractState);
          }
        }
      }
    }

    waitlist.clear();
    waitlist.push(pBlockStartState);

    Set<AbstractState> backward = Sets.newHashSet();
    backward.add(pBlockStartState);

    while (!waitlist.isEmpty()) {
      AbstractState currentState = waitlist.pop();
      if (pInitialState != currentState) {
        ARGState currentARGState = AbstractStates.extractStateByType(currentState, ARGState.class);
        for (ARGState parentARGState : currentARGState.getParents()) {
          AbstractState parentAbstractState = pAsAbstractState.apply(parentARGState);
          assert parentAbstractState != null;
          if (backward.add(parentAbstractState)) {
            waitlist.push(parentAbstractState);
          }
        }
      }
    }

    return Sets.intersection(forward, backward);
  }
}
