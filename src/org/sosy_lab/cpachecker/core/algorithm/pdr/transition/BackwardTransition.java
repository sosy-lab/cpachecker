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
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
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
import org.sosy_lab.cpachecker.cpa.blockcount.BlockCountCPA;
import org.sosy_lab.cpachecker.cpa.blockcount.BlockCountState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public class BackwardTransition {

  private static final Predicate<AbstractState> IS_BLOCK_START =
      pInput -> AbstractStates.extractStateByType(pInput, BlockCountState.class).isStopState();

  private final Algorithm algorithm;

  private final ConfigurableProgramAnalysis cpa;

  private final ReachedSetFactory reachedSetFactory;

  private final PathFormulaManager pathFormulaManager;

  private final LoadingCache<CFANode, Iterable<Block>> cache =
      CacheBuilder.newBuilder()
          .weakKeys()
          .weakValues()
          .<CFANode, Iterable<Block>>build(
              new CacheLoader<CFANode, Iterable<Block>>() {

                @Override
                public Iterable<Block> load(CFANode pCacheKey) throws CPAException, InterruptedException {
                  return getBlocksTo0(pCacheKey);
                }
              });

  public BackwardTransition(
      ReachedSetFactory pReachedSetFactory, ConfigurableProgramAnalysis pCPA, Algorithm pAlgorithm)
      throws InvalidConfigurationException {

    reachedSetFactory = pReachedSetFactory;

    cpa = pCPA;

    PredicateCPA predicateCPA = CPAs.retrieveCPA(cpa, PredicateCPA.class);
    if (predicateCPA == null) {
      throw new InvalidConfigurationException(
          "PredicateCPA required for transitions in the PDRAlgorithm");
    }
    pathFormulaManager = predicateCPA.getPathFormulaManager();

    BlockCountCPA blockCountCPA = CPAs.retrieveCPA(cpa, BlockCountCPA.class);
    if (blockCountCPA == null) {
      throw new InvalidConfigurationException(
          "BlockCountCPA required for transitions in the PDRAlgorithm");
    }

    ARGCPA argCPA = CPAs.retrieveCPA(cpa, ARGCPA.class);
    if (argCPA == null) {
      throw new InvalidConfigurationException(
          "ARGCPA required for transitions in the PDRAlgorithm");
    }

    algorithm = pAlgorithm;
  }

  /**
   * Gets all blocks from predecessors to the given successor location.
   *
   * A cache will be used to store results and retrieve previously computed
   * blocks from.
   *
   * @param pSuccessorLocation the successor location of the resulting blocks.
   * @return all blocks from predecessors to the given successor location.
   * @throws CPAException if the analysis creating the blocks encounters an
   * exception.
   * @throws InterruptedException if block creation was interrupted.
   */
  public FluentIterable<Block> getBlocksTo(CFANode pSuccessorLocation)
      throws CPAException, InterruptedException {
    return getBlocksTo(pSuccessorLocation, true);
  }

  /**
   * Gets all blocks from predecessors to the given successor location.
   *
   * @param pSuccessorLocation the successor location of the resulting blocks.
   * @param pUseCache whether to store the results in or retrieve them from a
   * cache.
   *
   * @return all blocks from predecessors to the given successor location.
   * @throws CPAException if the analysis creating the blocks encounters an
   * exception.
   * @throws InterruptedException if block creation was interrupted.
   */
  public FluentIterable<Block> getBlocksTo(CFANode pSuccessorLocation, boolean pUseCache)
      throws CPAException, InterruptedException {
    if (!pUseCache) {
      return getBlocksTo0(pSuccessorLocation);
    }
    try {
      return FluentIterable.from(cache.get(pSuccessorLocation));
    } catch (ExecutionException e) {
      Throwables.propagateIfPossible(e.getCause(), CPAException.class, InterruptedException.class);
      Throwables.propagate(e.getCause());
      throw new AssertionError("The above statement should always throw and never return.");
    }
  }

  private FluentIterable<Block> getBlocksTo0(CFANode pSuccessorLocation)
      throws CPAException, InterruptedException {
    ReachedSet reachedSet = reachedSetFactory.create();
    initializeFor(reachedSet, pSuccessorLocation);
    AbstractState initialState = reachedSet.getFirstState();
    algorithm.run(reachedSet);
    return FluentIterable.from(reachedSet)
        .filter(IS_BLOCK_START)
        .transform(
            (blockStartState) ->
                new BlockImpl(
                    initialState,
                    blockStartState,
                    AnalysisDirection.BACKWARD,
                    getReachedSet(
                        initialState, blockStartState, reachedSet, asAbstractState(reachedSet))));
  }

  public FluentIterable<Block> getBlocksTo(Iterable<CFANode> pSuccessorLocations)
      throws CPAException, InterruptedException {
    return getBlocksTo(pSuccessorLocations, Predicates.alwaysTrue());
  }

  public FluentIterable<Block> getBlocksTo(
      Iterable<CFANode> pSuccessorLocations, Predicate<AbstractState> pFilterPredecessors)
      throws CPAException, InterruptedException {

    Map<CFANode, Iterable<Block>> cached = cache.getAllPresent(pSuccessorLocations);

    Iterable<CFANode> uncachedSuccessorLocations = Iterables.filter(pSuccessorLocations, node -> !cached.containsKey(node));
    Iterator<CFANode> successorLocationIterator = uncachedSuccessorLocations.iterator();
    if (!successorLocationIterator.hasNext()) {
      return FluentIterable.from(Collections.emptyList());
    }
    ReachedSet reachedSet;
    // If there is only one successor location,
    // the initial state is unambiguous and can be created with less effort
    CFANode firstSuccessorLocation = successorLocationIterator.next();
    if (!successorLocationIterator.hasNext()) {
      return getBlocksTo(firstSuccessorLocation).filter(Blocks.applyToPredecessor(pFilterPredecessors));
    }
    reachedSet = reachedSetFactory.create();
    initializeFor(reachedSet, firstSuccessorLocation);
    while (successorLocationIterator.hasNext()) {
      CFANode successorLocation = successorLocationIterator.next();
      initializeFor(reachedSet, successorLocation);
    }

    Set<AbstractState> allInitialStates = FluentIterable.from(reachedSet).toSet();

    algorithm.run(reachedSet);
    Function<ARGState, AbstractState> asAbstractState = asAbstractState(reachedSet);

    Set<Block> computedBlocks =
        FluentIterable.from(reachedSet)
            // Only consider abstract states where a block starts
            .filter(IS_BLOCK_START)
            // Apply the client-provided filter
            .filter(pFilterPredecessors)
            .transformAndConcat(
                (blockStartState) ->
                    FluentIterable.from(
                            getInitialStates(blockStartState, allInitialStates, asAbstractState))
                        .transform(
                            (initialState) ->
                                (Block)
                                    new BlockImpl(
                                        initialState,
                                        blockStartState,
                                        AnalysisDirection.BACKWARD,
                                        getReachedSet(
                                            initialState,
                                            blockStartState,
                                            reachedSet,
                                            asAbstractState))))
            .toSet();

    for (Map.Entry<CFANode, Collection<Block>> entry : Multimaps.index(computedBlocks, block -> block.getSuccessorLocation()).asMap().entrySet()) {
      cache.put(entry.getKey(), entry.getValue());
      assert !cached.keySet().contains(entry.getKey());
    }

    return FluentIterable.from(Iterables.concat(
        Iterables.concat(cached.values()),
        computedBlocks));
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

  private Iterable<AbstractState> getInitialStates(
      AbstractState pBlockStartState,
      Set<AbstractState> pAllInitialStates,
      Function<ARGState, AbstractState> pAsAbstractState) {
    Set<AbstractState> visited = Sets.newHashSet();
    visited.add(pBlockStartState);
    Deque<AbstractState> waitlist = Queues.newArrayDeque();
    waitlist.push(pBlockStartState);
    List<AbstractState> initialStates = Lists.newArrayListWithExpectedSize(1);
    while (!waitlist.isEmpty()) {
      AbstractState currentState = waitlist.pop();
      if (pAllInitialStates.contains(currentState)) {
        initialStates.add(currentState);
        assert !IS_BLOCK_START.apply(currentState);
      } else {
        ARGState currentARGState = AbstractStates.extractStateByType(currentState, ARGState.class);
        for (ARGState parentARGState : currentARGState.getParents()) {
          AbstractState parentAbstractState = pAsAbstractState.apply(parentARGState);
          assert parentAbstractState != null;
          if (visited.add(parentAbstractState)) {
            waitlist.push(parentAbstractState);
          }
        }
      }
    }
    return initialStates;
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
      return direction == AnalysisDirection.FORWARD ? firstState : lastState;
    }

    @Override
    public AbstractState getSuccessor() {
      return direction == AnalysisDirection.FORWARD ? lastState : firstState;
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
    public int hashCode() {
      return Objects.hash(direction, firstState, lastState, reachedSet);
    }

    @Override
    public String toString() {
      return BackwardTransition.toString(this);
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
      reachedSet.add(state, pOriginalReachedSet.getPrecision(state));
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
