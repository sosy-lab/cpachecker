/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.bam;

import static org.sosy_lab.cpachecker.util.AbstractStates.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm.CPAAlgorithmFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CPAs;

import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

@Options(prefix = "cpa.bam")
public class BAMTransferRelation implements TransferRelation {

  @Options
  static class PCCInformation {

    @Option(secure=true, name = "pcc.proofgen.doPCC", description = "Generate and dump a proof")
    private boolean doPCC = false;

    private static PCCInformation instance = null;

    private PCCInformation(Configuration pConfig) throws InvalidConfigurationException {
      pConfig.inject(this);
    }

    public static void instantiate(Configuration pConfig) throws InvalidConfigurationException {
      instance = new PCCInformation(pConfig);
    }

    public static boolean isPCCEnabled() {
      return instance.doPCC;
    }

  }

  @Option(name = "handleRecursiveProcedures", secure = true,
      description = "BAM allows to analyse recursive procedures depending on the underlying CPA.")
  private boolean handleRecursiveProcedures = false;

  private final BAMCache argCache;

  final Map<AbstractState, ReachedSet> abstractStateToReachedSet = new HashMap<>();
  final Map<AbstractState, AbstractState> expandedToReducedCache = new HashMap<>();
  final Map<AbstractState, Block> expandedToBlockCache = new HashMap<>();

  private Block currentBlock;
  private BlockPartitioning partitioning;
  private int depth = 0;
  private final List<Triple<AbstractState, Precision, Block>> stack = new ArrayList<>();

  private final LogManager logger;
  private final CPAAlgorithmFactory algorithmFactory;
  private final TransferRelation wrappedTransfer;
  private final ReachedSetFactory reachedSetFactory;
  private final Reducer wrappedReducer;
  private final BAMCPA bamCPA;
  private final ProofChecker wrappedProofChecker;

  // Callstack-CPA is used for additional recursion handling
  private final CallstackTransferRelation callstackTransfer;

  private Map<AbstractState, Precision> forwardPrecisionToExpandedPrecision;
  private Map<Pair<ARGState, Block>, Collection<ARGState>> correctARGsForBlocks = null;

  //Stats
  int maxRecursiveDepth = 0;

  final Timer recomputeARTTimer = new Timer();
  final Timer removeCachedSubtreeTimer = new Timer();
  final Timer removeSubtreeTimer = new Timer();

  boolean breakAnalysis = false;

  // flags of the fixpoint-algorithm for recursion
  private boolean recursionSeen = false;
  private boolean resultStatesChanged = false;
  private boolean targetFound = false;
  final Map<AbstractState, Triple<AbstractState, Precision, Block>> potentialRecursionUpdateStates = new HashMap<>();

  public BAMTransferRelation(Configuration pConfig, LogManager pLogger, BAMCPA bamCpa,
                             ProofChecker wrappedChecker, BAMCache cache,
      ReachedSetFactory pReachedSetFactory, ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    pConfig.inject(this);

    logger = pLogger;
    algorithmFactory = new CPAAlgorithmFactory(bamCpa, logger, pConfig, pShutdownNotifier, null);
    reachedSetFactory = pReachedSetFactory;
    callstackTransfer = (CallstackTransferRelation) (CPAs.retrieveCPA(bamCpa, CallstackCPA.class)).getTransferRelation();
    wrappedTransfer = bamCpa.getWrappedCpa().getTransferRelation();
    wrappedReducer = bamCpa.getReducer();
    PCCInformation.instantiate(pConfig);
    bamCPA = bamCpa;
    wrappedProofChecker = wrappedChecker;
    argCache = cache;

    assert wrappedReducer != null;
  }


  void setForwardPrecisionToExpandedPrecision(
      Map<AbstractState, Precision> pForwardPrecisionToExpandedPrecision) {
    forwardPrecisionToExpandedPrecision = pForwardPrecisionToExpandedPrecision;
  }

  void setBlockPartitioning(BlockPartitioning pManager) {
    partitioning = pManager;
  }

  public BlockPartitioning getBlockPartitioning() {
    assert partitioning != null;
    return partitioning;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
          final AbstractState pState, final Precision pPrecision)
          throws CPATransferException, InterruptedException {
    try {

    final Collection<? extends AbstractState> successors = getAbstractSuccessorsWithoutWrapping(pState, pPrecision);

    assert !Iterables.any(successors, IS_TARGET_STATE) || successors.size() == 1 :
            "target-state should be returned as single-element-collection";

    return attachAdditionalInfoToCallNodes(successors);

    } catch (CPAException e) {
      throw new RecursiveAnalysisFailedException(e);
    }
  }

  private Collection<? extends AbstractState> getAbstractSuccessorsWithoutWrapping(
      final AbstractState pState, final Precision pPrecision)
          throws CPAException, InterruptedException {

    forwardPrecisionToExpandedPrecision.clear();

    final CFANode node = extractLocation(pState);

    if (handleRecursiveProcedures && stack.isEmpty() && isHeadOfMainFunction(node)) {
      // we are at the start of the program (root-node of CFA).
      return doFixpointIterationForRecursion(pState, pPrecision, node);
    }

      // we are at some location inside the program,
      // this part is always and only reached as recursive call with 'doRecursiveAnalysis'
      // (except we have a full cache-hit).

    if (currentBlock != null && currentBlock.isReturnNode(node) && !alreadyReturnedFromSameBlock(pState, currentBlock)) {
      // we are leaving the block, do not perform analysis beyond the current block.
      // special case: returning from a recursive function is only allowed once per state.
      return Collections.emptySet();
    }

    if (partitioning.isCallNode(node)
            && !(handleRecursiveProcedures && ((ARGState)pState).getParents().isEmpty()) // if no parents, we have already started a new block
            && (!partitioning.getBlockForCallNode(node).equals(currentBlock)
                || (handleRecursiveProcedures && isFunctionBlock(partitioning.getBlockForCallNode(node))))) {
      // we are at the entryNode of a new block and we are in a new context,
      // so we have to start a recursive analysis
      logger.log(Level.FINEST, "Starting recursive analysis of depth", ++depth);
      maxRecursiveDepth = Math.max(depth, maxRecursiveDepth);

      Collection<? extends AbstractState> resultStates = doRecursiveAnalysis(pState, pPrecision, node);

      logger.log(Level.FINEST, "Finished recursive analysis of depth", depth--);
      return resultStates;
    }

    // the easy case: we are in the middle of a block, so just forward to wrapped CPAs.
    // if there are several leaving edges, the wrapped CPA should handle all of them.

    // The Callstack-CPA is not able to handle a recursion of the form f-g-f,
    // because the operation Reduce splits it into f-g and g-f.
    // Thus we check for recursion here and (if we do not handle recursion here)
    // set a flag for the Callstack-CPA, such that it knows about the recursion.
    final boolean foundRecursion = !handleRecursiveProcedures && isRecursiveCall(node);
    if (foundRecursion) {
      callstackTransfer.enableRecursiveContext();
    }
    final Collection<? extends AbstractState> result = wrappedTransfer.getAbstractSuccessors(pState, pPrecision);
    if (foundRecursion) {
      callstackTransfer.disableRecursiveContext();
    }
    return result;
  }


  /**
   * check if
   * - the current node is before a function-block and
   * - the block was entered before (and thus is part of the stack).
   */
  private boolean isRecursiveCall(final CFANode node) {
    if (!partitioning.isCallNode(node)) {

      // TODO Why filter for functionCallEdge?
      // If only LoopBlocks are used, we can have recursive Loops, too.

      for (CFAEdge e : CFAUtils.leavingEdges(node).filter(CFunctionCallEdge.class)) {
        for (Block block : Lists.transform(stack, Triple.<Block>getProjectionToThird())) {
          if (block.getCallNodes().contains(e.getSuccessor())) {
            return true;
          }
        }
      }
    }
    return false;
  }


  private Collection<? extends AbstractState> doFixpointIterationForRecursion(
          final AbstractState pHeadOfMainFunctionState, final Precision pPrecision, final CFANode pHeadOfMainFunction)
          throws CPAException, InterruptedException {

    assert isHeadOfMainFunction(pHeadOfMainFunction) && stack.isEmpty();

    Collection<? extends AbstractState> resultStates;
    int iterationCounter = 0;
    while (true) { // fixpoint-iteration to handle recursive functions

      if (!targetFound) {
        // The target might be outside the recursive function.
        // If CEGAR removes the target through refinement,
        // we might miss the recursive function, if we reset the flags. So we do not reset them in that case.

        recursionSeen = false; // might be changed in recursive analysis
        resultStatesChanged = false; // might be changed in recursive analysis
        potentialRecursionUpdateStates.clear();
      }

      logger.log(Level.FINE, "Starting recursive analysis of main-block");

      resultStates = doRecursiveAnalysis(pHeadOfMainFunctionState, pPrecision, pHeadOfMainFunction);

      logger.log(Level.FINE, "Finished recursive analysis of main-block");

      // EITHER: result is an target-state, return it 'as is' and let CEGAR-algorithm perform a refinement, if needed.
      // OR:     we have completely analyzed the main-block and have not found an target-state.
      //         now we check, if we need to unwind recursive calls further until a fixpoint is reached.

      targetFound = Iterables.any(resultStates, IS_TARGET_STATE);
      if (targetFound) {
        // not really a fixpoint, but we return and let CEGAR check the target-state
        logger.log(Level.INFO, "fixpoint-iteration aborted, because there was a target state.");
        break;
      }

      if (!resultStatesChanged) {
        logger.log(Level.INFO, "fixpoint-iteration aborted, because we did not get new states (fixpoint reached).");
        break;
      }

      logger.log(Level.INFO, "fixpoint was not reached, starting new iteration", ++iterationCounter);

      reAddStatesForFixPointIteration(pHeadOfMainFunctionState);

      // continue;
    }

    return resultStates;
  }

  private void reAddStatesForFixPointIteration(final AbstractState pHeadOfMainFunctionState) {
    for (final AbstractState recursionUpdateState : potentialRecursionUpdateStates.keySet()) {
      // final Triple<AbstractState, Precision, Block> level = potentialRecursionUpdateStates.get(recursionUpdateState);

      // final Pair<ReachedSet, Collection<AbstractState>> reachedResult =
      //         argCache.get(level.getFirst(), level.getSecond(), level.getThird());
      // final ReachedSet innerReachedSet = checkNotNull(reachedResult.getFirst());
      // final Collection<AbstractState> innerResultStates = checkNotNull(reachedResult.getSecond());

      // update waitlist
      for (final ReachedSet reachedSet : argCache.getAllCachedReachedStates()) {
        if (reachedSet.contains(recursionUpdateState)) {
          logger.log(Level.ALL, "re-adding state", recursionUpdateState);
          reachedSet.reAddToWaitlist(recursionUpdateState);
        }
        // else if (pHeadOfMainFunctionState == recursionUpdateState) {
          // special case: this is the root-state of the whole program, it is in the main-reachedset.
          // we have no possibility to re-add it,because we do not have access to the main-reachedset.
          // however we do not need to re-add it, because it is the initial-state of current transfer-relation.
        // }
      }
      // TODO break loop earlier?
    }
  }

  /** returns a covering level or Null, if no such level is found. */
  private Triple<AbstractState, Precision, Block> getCoveringLevel(final List<Triple<AbstractState, Precision, Block>> stack,
                                final Triple<AbstractState, Precision, Block> currentLevel)
    throws CPAException, InterruptedException {
    for (Triple<AbstractState, Precision, Block> level : stack.subList(0, stack.size() - 1)) {
      if (level.getThird() == currentLevel.getThird()
              // && level.getSecond().equals(currentLevel.getSecond())
              && bamCPA.isCoveredBy(currentLevel.getFirst(), level.getFirst())) {
        // previously reached state contains 'less' information, it is a super-state of the currentState.
        // From currentState we could reach the levelState again (with further unrolling).
        // Thus we would have found an endless recursion (with current information (precision/state)).

        // TODO how to compare precisions? equality would be enough
        return level;
      }
    }
    return null;
  }

  /** check, if all base-states are covered by the covering-states.
   * examples:
   * {x} is covered by {x,y,z}
   * {} is covered by {x,y,z}
   * {x} is covered by {(x or y),z}
   * {(x and y),z} is covered by {x,z}
   */
  private Collection<AbstractState> getStatesNotCoveredBy(@Nonnull final Collection<AbstractState> baseStates,
      @Nonnull final Collection<AbstractState> coveringStates)
      throws CPAException, InterruptedException {
    final Collection<AbstractState> notCoveredStates = new ArrayList<>();
    for (final AbstractState baseState : baseStates) {
      if (!isCoveredByAny(baseState, coveringStates)) {
        notCoveredStates.add(baseState);
      }
    }
    return notCoveredStates;
  }

  /** is there any covering-state, that covers the base-state? */
  private boolean isCoveredByAny(@Nonnull final AbstractState baseState,
                                 @Nonnull final Collection<AbstractState> coveringStates)
          throws CPAException, InterruptedException {
    if (coveringStates.contains(baseState)) {
      return true;
    }
    for (final AbstractState coveringState : coveringStates) {
      if (bamCPA.isCoveredBy(baseState, coveringState)) {
        return true;
      }
    }
    return false;
  }

  /** Enters a new block and performs a new analysis or returns result from cache. */
  private Collection<? extends AbstractState> doRecursiveAnalysis(
          final AbstractState initialState, final Precision pPrecision, final CFANode node)
          throws CPAException, InterruptedException  {

    //Create ReachSet with node as initial element (+ add corresponding Location+CallStackElement)
    //do an CPA analysis to get the complete reachset
    //if lastElement is error State
    // -> return lastElement and break at precision adjustment
    //else
    // -> compute which states refer to return nodes
    // -> return these states as successor
    // -> cache the result

    final Block outerSubtree = currentBlock;
    currentBlock = partitioning.getBlockForCallNode(node);

    logger.log(Level.ALL, "Reducing state", initialState);
    final AbstractState reducedInitialState = wrappedReducer.getVariableReducedState(initialState, currentBlock, node);
    final Precision reducedInitialPrecision = wrappedReducer.getVariableReducedPrecision(pPrecision, currentBlock);

    final Triple<AbstractState, Precision, Block> currentLevel = Triple.of(reducedInitialState, reducedInitialPrecision, currentBlock);
    stack.add(currentLevel);
    logger.log(Level.FINEST, "current Stack:", stack);

    final Collection<? extends AbstractState> resultStates;

    if (!handleRecursiveProcedures || (!(node instanceof FunctionEntryNode) || isHeadOfMainFunction(node))) {
      // block is a loop-block
      // or we are at the start of the main-block, we do not need the rebuilding-part for main,
      // so we use a simplified version of function-handling and handle it like a loop-block
      resultStates = analyseBlockAndExpand(initialState, pPrecision, outerSubtree, reducedInitialState, reducedInitialPrecision);

    } else {
      // function-entry and old block --> begin new block
      // TODO match only recursive function, not all functions

      assert !((ARGState)initialState).getParents().isEmpty();

      // get the rootState, that is the abstract state of the functioncall.
      Collection<ARGState> possibleRootStates = ((ARGState)initialState).getParents();
      assert possibleRootStates.size() == 1 : "too many functioncalls: " + possibleRootStates;
      AbstractState rootState = possibleRootStates.iterator().next();

      final Collection<AbstractState> expandedFunctionReturnStates;

      final Triple<AbstractState, Precision, Block> coveringLevel = getCoveringLevel(stack, currentLevel);
      if (coveringLevel != null) {
        // if level is twice in stack, we have endless recursion.
        // with current knowledge we would never abort unrolling the recursion.
        // lets skip the function and return only a short "summary" of the function.
        // this summary is the result of a previous analysis of this block from the cache.
        logger.logf(Level.FINEST, "recursion will cause endless unrolling (with current precision), " +
                "aborting call of function '%s' at state %s", node.getFunctionName(), reducedInitialState);

        expandedFunctionReturnStates = analyseRecursiveBlockAndExpand(
            initialState, pPrecision, outerSubtree, reducedInitialState, coveringLevel);

      } else {
        // enter block of function-call and start recursive analysis
        expandedFunctionReturnStates = analyseBlockAndExpand(
                initialState, pPrecision, outerSubtree, reducedInitialState, reducedInitialPrecision);
      }

      if (breakAnalysis) {
        // analysis aborted, so lets abort here too
        // TODO why return element?
        assert expandedFunctionReturnStates.size() == 1;
        return expandedFunctionReturnStates;
      }

      final Collection<AbstractState> rebuildStates = new ArrayList<>(expandedFunctionReturnStates.size());
      for (final AbstractState expandedState : expandedFunctionReturnStates) {
        rebuildStates.add(getRebuildState(rootState, initialState, expandedState));
      }
      logger.log(Level.ALL, "finished rebuilding of", rebuildStates.size(), "states");
      resultStates = rebuildStates;

      // current location is "before" the function-return-edge.
    }

    if (recursionSeen) {
      // we are returning from an recursive call.
      // if we would need to do further analysis for recursion later (fixpoint-analysis!),
      // we need to know, where to update the reachedset --> store initialState for later usage
      potentialRecursionUpdateStates.put(initialState, currentLevel);
    }

    final Triple<AbstractState, Precision, Block> lastLevel = stack.remove(stack.size() - 1);
    assert lastLevel.equals(currentLevel);
    currentBlock = outerSubtree;

    return resultStates;
  }

  /** This method analyses a recursive procedure and
   * returns (expanded) abstract states from the cache, that match the current procedure,
   * or an empty set, if no cache entry was found. */
  private Collection<AbstractState> analyseRecursiveBlockAndExpand(
      final AbstractState initialState, final Precision pPrecision,
      final Block pOuterSubtree, final AbstractState pReducedInitialState,
      final Triple<AbstractState, Precision, Block> pCoveringLevel) throws CPATransferException {

    recursionSeen = true;
    // after this point we have to check all returnStates for changes.
    // If no changes are found, we have found the fixpoint.

    // try to get previously computed states from cache
    final Pair<ReachedSet, Collection<AbstractState>> pair =
            //argCache.get(reducedInitialState, reducedInitialPrecision, currentBlock);
            argCache.get(pCoveringLevel.getFirst(), pCoveringLevel.getSecond(), pCoveringLevel.getThird());
    final ReachedSet reached = pair.getFirst();
    final Collection<AbstractState> previousResult = pair.getSecond();
    final Collection<Pair<AbstractState, Precision>> reducedResult;

    assert reached != null : "cached entry has no reached set";
    if (previousResult == null) {
      // outer block was not finished, abort recursion
      reducedResult = Collections.emptySet();
      logger.logf(Level.FINEST, "skipping recursive call with new empty result");
    } else {
      // use previously computed outer block as inner block,
      // this is equal to 'add one recursive step' in the recursion
      reducedResult = imbueAbstractStatesWithPrecision(reached, previousResult);
      logger.logf(Level.FINEST, "skipping recursive call with cached result");
    }

    abstractStateToReachedSet.put(initialState, reached);

    addBlockAnalysisInfo(pReducedInitialState);

    return expandResultStates(reducedResult, pOuterSubtree, initialState, pPrecision);
  }

  static boolean isHeadOfMainFunction(CFANode currentNode) {
    return currentNode instanceof FunctionEntryNode && currentNode.getNumEnteringEdges() == 0;
  }

  static boolean isFunctionBlock(Block block) {
    return block.getCallNodes().size() == 1 && block.getCallNode() instanceof FunctionEntryNode;
  }

  /** Analyse block, return expanded exit-states. */
  private Collection<AbstractState> analyseBlockAndExpand(
          final AbstractState entryState, final Precision precision, final Block outerSubtree,
          final AbstractState reducedInitialState, final Precision reducedInitialPrecision)
          throws CPAException, InterruptedException {

    final Collection<Pair<AbstractState, Precision>> reducedResult =
            getReducedResult(entryState, reducedInitialState, reducedInitialPrecision);

    logger.log(Level.ALL, "Resulting states:", reducedResult);

    addBlockAnalysisInfo(reducedInitialState);

    if (breakAnalysis) {
      // analysis aborted, so lets abort here too
      // TODO why return element?
      assert reducedResult.size() == 1;
      return Collections.singleton(Iterables.getOnlyElement(reducedResult).getFirst());
    }

    return expandResultStates(reducedResult, outerSubtree, entryState, precision);
  }

  /** This function returns expanded states for all reduced states and updates the caches. */
  private List<AbstractState> expandResultStates(
          final Collection<Pair<AbstractState, Precision>> reducedResult,
          final Block outerSubtree, final AbstractState state, final Precision precision) {

    logger.log(Level.ALL, "Expanding states with initial state", state);
    logger.log(Level.ALL, "Expanding states", reducedResult);

    final List<AbstractState> expandedResult = new ArrayList<>(reducedResult.size());
    for (Pair<AbstractState, Precision> reducedPair : reducedResult) {
      AbstractState reducedState = reducedPair.getFirst();
      Precision reducedPrecision = reducedPair.getSecond();

      AbstractState expandedState =
              wrappedReducer.getVariableExpandedState(state, currentBlock, reducedState);
      expandedToReducedCache.put(expandedState, reducedState);
      expandedToBlockCache.put(expandedState, currentBlock);

      Precision expandedPrecision =
              outerSubtree == null ? reducedPrecision : // special case: return from main
              wrappedReducer.getVariableExpandedPrecision(precision, outerSubtree, reducedPrecision);

      ((ARGState)expandedState).addParent((ARGState) state);
      expandedResult.add(expandedState);

      forwardPrecisionToExpandedPrecision.put(expandedState, expandedPrecision);
    }

    logger.log(Level.ALL, "Expanded results:", expandedResult);

    return expandedResult;
  }

  /** Analyse the block starting at node with initialState.
   * If there is a result in the cache, it is used,
   * otherwise a recursive CPAAlgorithm is started. */
  private Collection<Pair<AbstractState, Precision>> getReducedResult(
          final AbstractState initialState,
          final AbstractState reducedInitialState, final Precision reducedInitialPrecision)
          throws InterruptedException, CPAException {

    final Collection<AbstractState> reducedResult;
    // statesForFurtherAnalysis is always equal to reducedResult, except one special case (aka re-visiting recursion)
    final Collection<AbstractState> statesForFurtherAnalysis;

    // try to get previously computed element from cache
    final Pair<ReachedSet, Collection<AbstractState>> pair =
            argCache.get(reducedInitialState, reducedInitialPrecision, currentBlock);
    ReachedSet reached = pair.getFirst();
    final Collection<AbstractState> cachedReturnStates = pair.getSecond();

    assert cachedReturnStates == null || reached != null : "there cannot be result-states without reached-states";

    if (cachedReturnStates != null && !reached.hasWaitingState()) {

      // cache hit, return element from cache
      logger.log(Level.FINEST, "Cache hit with finished reachedset.");
      reducedResult = cachedReturnStates;
      statesForFurtherAnalysis = reducedResult;

    } else if (cachedReturnStates != null && cachedReturnStates.size() == 1 &&
        reached.getLastState() != null && ((ARGState)reached.getLastState()).isTarget()) {
      assert Iterables.getOnlyElement(cachedReturnStates) == reached.getLastState() :
              "cache hit only allowed for finished reached-sets or target-states";

      // cache hit, return element from cache
      logger.log(Level.FINEST, "Cache hit with target-state.");
      reducedResult = cachedReturnStates;
      statesForFurtherAnalysis = reducedResult;

    } else {
      if (reached == null) {
        // we have not even cached a partly computed reach-set,
        // so we must compute the subgraph specification from scratch
        reached = createInitialReachedSet(reducedInitialState, reducedInitialPrecision);
        argCache.put(reducedInitialState, reducedInitialPrecision, currentBlock, reached);
        logger.log(Level.FINEST, "Cache miss: starting recursive CPAAlgorithm with new initial reached-set.");
      } else {
        logger.log(Level.FINEST, "Partial cache hit: starting recursive CPAAlgorithm with partial reached-set.");
      }

      reducedResult = performCompositeAnalysisWithCPAAlgorithm(reached);

      assert reducedResult != null;
      if (cachedReturnStates == null) {
        logger.log(Level.FINEST, "there was no cache-entry for result-states.");
        resultStatesChanged = true;
        statesForFurtherAnalysis = reducedResult;

      } else {
        // this is the result from a previous analysis of a recursive functioncall
        // now we check, if we really get new states or if all new states (= reducedResult) are
        final Collection<AbstractState> newStates = getStatesNotCoveredBy(reducedResult, cachedReturnStates);
        if (newStates.isEmpty()) {
          // analysis of recursive function did not produce more states.
          logger.log(Level.FINEST, "all previous return-states are covering the current new states, no new states found.");
        } else {
          // new states found, set flag for fixpoint-analysis and return (and later update the cache).
          logger.log(Level.FINEST, "some cached result-states are not covered. returning new result-states.");
          resultStatesChanged = true;
        }

        // we are in an the fixpoint-algorithm for recursion,
        // we have already analysed all covered states in the previous iteration,
        // thus we only need to analyse the remaining states.
        // this is an optimisation,
        statesForFurtherAnalysis = newStates;
      }
    }

    assert reached != null;
    abstractStateToReachedSet.put(initialState, reached);

    ARGState rootOfBlock = null;
    if (PCCInformation.isPCCEnabled()) {
      if (!(reached.getFirstState() instanceof ARGState)) {
        throw new CPATransferException("Cannot build proof, ARG, for BAM analysis.");
      }
      rootOfBlock = BAMARGUtils.copyARG((ARGState) reached.getFirstState());
    }

    // use 'reducedResult' for cache and 'statesForFurtherAnalysis' as return value,
    // both are always equal, except analysis of recursive procedures (@fixpoint-algorithm)
    argCache.put(reducedInitialState, reached.getPrecision(reached.getFirstState()), currentBlock, reducedResult, rootOfBlock);

    return imbueAbstractStatesWithPrecision(reached, statesForFurtherAnalysis);
  }

  /** Reconstruct the resulting state from root-, entry- and expanded-state.
   * Also cleanup and update the ARG with the new build state. */
  private AbstractState getRebuildState(final AbstractState rootState, final AbstractState entryState, final AbstractState expandedState) {
    logger.log(Level.ALL, "rebuilding state with root state", rootState);
    logger.log(Level.ALL, "rebuilding state with entry state", entryState);
    logger.log(Level.ALL, "rebuilding state with expanded state", expandedState);

    final CFANode location = extractLocation(expandedState);
    if (!(location instanceof FunctionExitNode)) {
      logger.log(Level.ALL, "rebuilding skipped because of non-function-exit-location");
      assert isTargetState(expandedState) : "only target states are returned without rebuild";
      return expandedState;
    }

    final AbstractState rebuildState = wrappedReducer.rebuildStateAfterFunctionCall(
            rootState, entryState, expandedState, (FunctionExitNode)location);
    logger.log(Level.ALL, "rebuilding finished with state", rebuildState);

    // in the ARG of the outer block we have now the connection "rootState <-> expandedState"
    assert ((ARGState)expandedState).getChildren().isEmpty() && ((ARGState)expandedState).getParents().size() == 1:
        "unexpected expanded state: " + expandedState;
    assert ((ARGState)entryState).getChildren().contains(expandedState):
        "successor of entry state " +  entryState + " must be expanded state " + expandedState;

    // we replace this connection with "rootState <-> rebuildState"
    ((ARGState)expandedState).removeFromARG();
    ((ARGState)rebuildState).addParent((ARGState)entryState);

    assert expandedToBlockCache.get(expandedState) == currentBlock : "returning from wrong block?";

    // also clean up local data structures
    replaceStateInCaches(expandedState, rebuildState, true);

    return rebuildState;
  }

  void replaceStateInCaches(AbstractState oldState, AbstractState newState, boolean oldStateMustExist) {
    if (oldStateMustExist || expandedToReducedCache.containsKey(oldState)) {
      final AbstractState reducedState = expandedToReducedCache.remove(oldState);
      expandedToReducedCache.put(newState, reducedState);
    }

    if (oldStateMustExist || expandedToBlockCache.containsKey(oldState)) {
      final Block innerBlock = expandedToBlockCache.remove(oldState);
      expandedToBlockCache.put(newState, innerBlock);
    }

    if (oldStateMustExist || forwardPrecisionToExpandedPrecision.containsKey(oldState)) {
      final Precision expandedPrecision = forwardPrecisionToExpandedPrecision.remove(oldState);
      forwardPrecisionToExpandedPrecision.put(newState, expandedPrecision);
    }
  }

  /** checks, if the current state is at a node, where several block-exits are available and
   * one of them was already left. */
  private boolean alreadyReturnedFromSameBlock(AbstractState state, Block block) {
    while (expandedToReducedCache.containsKey(state)) {
      if (expandedToBlockCache.containsKey(state) && block == expandedToBlockCache.get(state)) {
        return true;
      }
      state = expandedToReducedCache.get(state);
    }
    return false;
  }

  /** Analyse the block with a 'recursive' call to the CPAAlgorithm.
   * Then analyse the result and get the returnStates. */
  private Collection<AbstractState> performCompositeAnalysisWithCPAAlgorithm(
          final ReachedSet reached)
          throws InterruptedException, CPAException {

    // CPAAlgorithm is not re-entrant due to statistics
    final CPAAlgorithm algorithm = algorithmFactory.newInstance();
    algorithm.run(reached);

    // if the element is an error element
    final Collection<AbstractState> returnStates;
    final AbstractState lastState = reached.getLastState();
    if (isTargetState(lastState)) {
      //found a target state inside a recursive subgraph call
      //this needs to be propagated to outer subgraph (till main is reached)
      returnStates = Collections.singletonList(lastState);

    } else if (reached.hasWaitingState()) {
      //no target state, but waiting elements
      //analysis failed -> also break this analysis
      breakAnalysis = true;
      returnStates =  Collections.singletonList(lastState);

    } else {
      // get only those states, that are at block-exit.
      // in case of recursion, the block-exit-nodes might also appear in the middle of the block,
      // but the middle states have children, the exit-states have not.
      returnStates = new ArrayList<>();
      for (AbstractState returnState : AbstractStates.filterLocations(reached, currentBlock.getReturnNodes())) {
        if (((ARGState)returnState).getChildren().isEmpty()) {
          returnStates.add(returnState);
        }
      }
    }

    return returnStates;
  }

  private List<Pair<AbstractState, Precision>> imbueAbstractStatesWithPrecision(
      ReachedSet pReached, Collection<AbstractState> pElements) {
    List<Pair<AbstractState, Precision>> result = new ArrayList<>();
    for (AbstractState ele : pElements) {
      result.add(Pair.of(ele, pReached.getPrecision(ele)));
    }
    return result;
  }

  private Collection<? extends AbstractState> attachAdditionalInfoToCallNodes(
      Collection<? extends AbstractState> pSuccessors) {
    if (PCCInformation.isPCCEnabled()) {
      List<AbstractState> successorsWithExtendedInfo = new ArrayList<>(pSuccessors.size());
      for (AbstractState elem : pSuccessors) {
        if (!(elem instanceof ARGState)) { return pSuccessors; }
        if (!(elem instanceof BAMARGBlockStartState)) {
          successorsWithExtendedInfo.add(createAdditionalInfo((ARGState) elem));
        } else {
          successorsWithExtendedInfo.add(elem);
        }
      }
      return successorsWithExtendedInfo;
    }
    return pSuccessors;
  }

  protected AbstractState attachAdditionalInfoToCallNode(AbstractState pElem) {
    if (!(pElem instanceof BAMARGBlockStartState) && PCCInformation.isPCCEnabled() && pElem instanceof ARGState) { return createAdditionalInfo((ARGState) pElem); }
    return pElem;
  }

  private ARGState createAdditionalInfo(ARGState pElem) {
    CFANode node = AbstractStates.extractLocation(pElem);
    if (partitioning.isCallNode(node) && !partitioning.getBlockForCallNode(node).equals(currentBlock)) {
      BAMARGBlockStartState replaceWith = new BAMARGBlockStartState(pElem.getWrappedState(), null);
      replaceInARG(pElem, replaceWith);
      return replaceWith;
    }
    return pElem;
  }

  private void replaceInARG(ARGState toReplace, ARGState replaceWith) {
    if (toReplace.isCovered()) {
      replaceWith.setCovered(toReplace.getCoveringState());
    }
    toReplace.uncover();

    toReplace.replaceInARGWith(replaceWith);
  }

  private void addBlockAnalysisInfo(AbstractState pElement) throws CPATransferException {
    if (PCCInformation.isPCCEnabled()) {
      if (argCache.getLastAnalyzedBlock() == null || !(pElement instanceof BAMARGBlockStartState)) { throw new CPATransferException(
          "Cannot build proof, ARG, for BAM analysis."); }
      ((BAMARGBlockStartState) pElement).setAnalyzedBlock(argCache.getLastAnalyzedBlock());
    }
  }

  private ReachedSet createInitialReachedSet(AbstractState initialState, Precision initialPredicatePrecision) {
    ReachedSet reached = reachedSetFactory.create();
    reached.add(initialState, initialPredicatePrecision);
    return reached;
  }

  void removeSubtree(ARGReachedSet mainReachedSet, ARGPath pPath,
      ARGState element, List<Precision> pNewPrecisions,
      List<Predicate<? super Precision>> pNewPrecisionTypes,
      Map<ARGState, ARGState> pPathElementToReachedState) {
    removeSubtreeTimer.start();

    final ARGSubtreeRemover argSubtreeRemover = new ARGSubtreeRemover(
            partitioning, wrappedReducer, argCache, reachedSetFactory, abstractStateToReachedSet,
            removeCachedSubtreeTimer, logger);
    argSubtreeRemover.removeSubtree(mainReachedSet, pPath, element,
            pNewPrecisions, pNewPrecisionTypes, pPathElementToReachedState);

    removeSubtreeTimer.stop();
  }

  //returns root of a subtree leading from the root element of the given reachedSet to the target state
  //subtree is represented using children and parents of ARGElements, where newTreeTarget is the ARGState
  //in the constructed subtree that represents target
  ARGState computeCounterexampleSubgraph(ARGState target, ARGReachedSet reachedSet,
                                                 Map<ARGState, ARGState> pPathElementToReachedState) {
    assert reachedSet.asReachedSet().contains(target);
    assert pPathElementToReachedState.isEmpty() : "new path should be started with empty set of states.";

    final BAMCEXSubgraphComputer cexSubgraphComputer = new BAMCEXSubgraphComputer(
            partitioning, wrappedReducer, argCache, pPathElementToReachedState,
            abstractStateToReachedSet, expandedToReducedCache, logger);
    return cexSubgraphComputer.computeCounterexampleSubgraph(
        target, reachedSet, new BAMCEXSubgraphComputer.BackwardARGState(target));
  }

  /** searches through all available reachedSet for a matching state and returns its precision */
  Precision getPrecisionForState(final ARGState state, final UnmodifiableReachedSet mainReachedSet) {
    if (mainReachedSet.contains(state)) {
      return mainReachedSet.getPrecision(state);
    }
    for (UnmodifiableReachedSet rs : abstractStateToReachedSet.values()) {
      if (rs.contains(state)) {
        return rs.getPrecision(state);
      }
    }
    throw new AssertionError("No reachedset found for state " + state + ". Where does this state come from?");
  }

  void clearCaches() {
    argCache.clear();
    abstractStateToReachedSet.clear();
  }

  Pair<Block, ReachedSet> getCachedReachedSet(ARGState root, Precision rootPrecision) {
    CFANode rootNode = extractLocation(root);
    Block rootSubtree = partitioning.getBlockForCallNode(rootNode);

    ReachedSet reachSet = abstractStateToReachedSet.get(root);
    assert reachSet != null;
    return Pair.of(rootSubtree, reachSet);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pElement, List<AbstractState> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException,
      InterruptedException {
    return attachAdditionalInfoToCallNodes(wrappedTransfer.strengthen(pElement, pOtherElements, pCfaEdge, pPrecision));
  }

  public boolean areAbstractSuccessors(AbstractState pState, CFAEdge pCfaEdge,
      Collection<? extends AbstractState> pSuccessors) throws CPATransferException,
      InterruptedException {
    if (pCfaEdge != null) { return wrappedProofChecker.areAbstractSuccessors(pState, pCfaEdge, pSuccessors); }
    return areAbstractSuccessors0(pState, pSuccessors, partitioning.getMainBlock());
  }

  private boolean areAbstractSuccessors0(AbstractState pState,
      Collection<? extends AbstractState> pSuccessors, final Block currentBlock)
      throws CPATransferException,
      InterruptedException {
    // currently cannot deal with blocks for which the set of call nodes and return nodes of that block is not disjunct
    boolean successorExists;

    CFANode node = extractLocation(pState);

    if (partitioning.isCallNode(node)
        && !partitioning.getBlockForCallNode(node).equals(currentBlock)) {
      // do not support nodes which are call nodes of multiple blocks
      Block analyzedBlock = partitioning.getBlockForCallNode(node);
      try {
        if (!(pState instanceof BAMARGBlockStartState)
            || ((BAMARGBlockStartState) pState).getAnalyzedBlock() == null
            || !bamCPA.isCoveredBy(wrappedReducer.getVariableReducedStateForProofChecking(pState, analyzedBlock, node),
                ((BAMARGBlockStartState) pState).getAnalyzedBlock())) { return false; }
      } catch (CPAException e) {
        throw new CPATransferException("Missing information about block whose analysis is expected to be started at "
            + pState);
      }
      try {
        Collection<ARGState> endOfBlock;
        Pair<ARGState, Block> key = Pair.of(((BAMARGBlockStartState) pState).getAnalyzedBlock(), analyzedBlock);
        if (correctARGsForBlocks != null && correctARGsForBlocks.containsKey(key)) {
          endOfBlock = correctARGsForBlocks.get(key);
        } else {
          Pair<Boolean, Collection<ARGState>> result =
              checkARGBlock(((BAMARGBlockStartState) pState).getAnalyzedBlock(), analyzedBlock);
          if (!result.getFirst()) { return false; }
          endOfBlock = result.getSecond();
          setCorrectARG(key, endOfBlock);
        }

        HashSet<AbstractState> notFoundSuccessors = new HashSet<>(pSuccessors);
        AbstractState expandedState;

        Multimap<CFANode, AbstractState> blockSuccessors = HashMultimap.create();
        for (AbstractState absElement : pSuccessors) {
          ARGState successorElem = (ARGState) absElement;
          blockSuccessors.put(extractLocation(absElement), successorElem);
        }


        for (ARGState leaveB : endOfBlock) {
          successorExists = false;
          expandedState = wrappedReducer.getVariableExpandedStateForProofChecking(pState, analyzedBlock, leaveB);
          for (AbstractState next : blockSuccessors.get(extractLocation(leaveB))) {
            if (bamCPA.isCoveredBy(expandedState, next)) {
              successorExists = true;
              notFoundSuccessors.remove(next);
            }
          }
          if (!successorExists) { return false; }
        }

        if (!notFoundSuccessors.isEmpty()) { return false; }

      } catch (CPAException e) {
        throw new CPATransferException("Checking ARG with root " + ((BAMARGBlockStartState) pState).getAnalyzedBlock()
            + " for block " + currentBlock + "failed.");
      }
    } else {
      HashSet<CFAEdge> usedEdges = new HashSet<>();
      for (AbstractState absElement : pSuccessors) {
        ARGState successorElem = (ARGState) absElement;
        usedEdges.add(((ARGState) pState).getEdgeToChild(successorElem));
      }

      //no call node, check if successors can be constructed with help of CFA edges
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        // edge leads to node in inner block
        Block currentNodeBlock = partitioning.getBlockForReturnNode(node);
        if (currentNodeBlock != null && !currentBlock.equals(currentNodeBlock)
            && currentNodeBlock.getNodes().contains(node.getLeavingEdge(i).getSuccessor())) {
          if (usedEdges.contains(node.getLeavingEdge(i))) { return false; }
          continue;
        }
        // edge leaves block, do not analyze, check for call node since if call node is also return node analysis will go beyond current block
        if (!currentBlock.isCallNode(node) && currentBlock.isReturnNode(node)
            && !currentBlock.getNodes().contains(node.getLeavingEdge(i).getSuccessor())) {
          if (usedEdges.contains(node.getLeavingEdge(i))) { return false; }
          continue;
        }
        if (!wrappedProofChecker.areAbstractSuccessors(pState, node.getLeavingEdge(i), pSuccessors)) {
          return false;
        }
      }
    }
    return true;
  }

  private Pair<Boolean, Collection<ARGState>> checkARGBlock(ARGState rootNode,
      final Block currentBlock)
      throws CPAException, InterruptedException {
    Collection<ARGState> returnNodes = new ArrayList<>();
    Set<ARGState> waitingForUnexploredParents = new HashSet<>();
    boolean unexploredParent;
    Stack<ARGState> waitlist = new Stack<>();
    HashSet<ARGState> visited = new HashSet<>();
    HashSet<ARGState> coveredNodes = new HashSet<>();
    ARGState current;

    waitlist.add(rootNode);
    visited.add(rootNode);

    while (!waitlist.isEmpty()) {
      current = waitlist.pop();

      if (current.isTarget()) {
        returnNodes.add(current);
      }

      if (current.isCovered()) {
        coveredNodes.clear();
        coveredNodes.add(current);
        do {
          if (!bamCPA.isCoveredBy(current, current.getCoveringState())) {
            returnNodes = Collections.emptyList();
            return Pair.of(false, returnNodes);
          }
          coveredNodes.add(current);
          if (coveredNodes.contains(current.getCoveringState())) {
            returnNodes = Collections.emptyList();
            return Pair.of(false, returnNodes);
          }
          current = current.getCoveringState();
        } while (current.isCovered());

        if (!visited.contains(current)) {
          unexploredParent = false;
          for (ARGState p : current.getParents()) {
            if (!visited.contains(p) || waitlist.contains(p)) {
              waitingForUnexploredParents.add(current);
              unexploredParent = true;
              break;
            }
          }
          if (!unexploredParent) {
            visited.add(current);
            waitlist.add(current);
          }
        }
        continue;
      }

      CFANode node = extractLocation(current);
      if (currentBlock.isReturnNode(node)) {
        returnNodes.add(current);
      }

      if (!areAbstractSuccessors0(current, current.getChildren(), currentBlock)) {
        returnNodes = Collections.emptyList();
        return Pair.of(false, returnNodes);
      }

      for (ARGState child : current.getChildren()) {
        unexploredParent = false;
        for (ARGState p : child.getParents()) {
          if (!visited.contains(p) || waitlist.contains(p)) {
            waitingForUnexploredParents.add(child);
            unexploredParent = true;
            break;
          }
        }
        if (unexploredParent) {
          continue;
        }
        if (visited.contains(child)) {
          returnNodes = Collections.emptyList();
          return Pair.of(false, returnNodes);
        } else {
          waitingForUnexploredParents.remove(child);
          visited.add(child);
          waitlist.add(child);
        }
      }

    }
    if (!waitingForUnexploredParents.isEmpty()) {
      returnNodes = Collections.emptyList();
      return Pair.of(false, returnNodes);
    }
    return Pair.of(true, returnNodes);
  }

  public void setCorrectARG(Pair<ARGState, Block> pKey, Collection<ARGState> pEndOfBlock) {
    if (correctARGsForBlocks == null) {
      correctARGsForBlocks = new HashMap<>();
    }
    correctARGsForBlocks.put(pKey, pEndOfBlock);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge) {

    throw new UnsupportedOperationException(
        "BAMCPA needs to be used as the outer-most CPA,"
        + " thus it does not support returning successors for a single edge.");
  }
}
