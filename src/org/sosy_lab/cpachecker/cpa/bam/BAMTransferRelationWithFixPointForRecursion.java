// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;
import static org.sosy_lab.cpachecker.util.AbstractStates.isTargetState;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMCache.BAMCacheEntry;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Triple;

@Options(prefix="cpa.bam")
public class BAMTransferRelationWithFixPointForRecursion extends BAMTransferRelation {

  @Option(secure=true, description="if we cannot determine a repeating/covering call-state, "
      + "we will run into CallStackOverflowException. Thus we bound the stack size (unsound!). "
      + "This option only limits non-covered recursion, but not a recursion "
      + "where we find a coverage and re-use the cached block several times. "
      + "The value '-1' disables this option.")
  private int maximalDepthForExplicitRecursion = -1;

  private final BAMCPA bamCpa;

  // flags of the fixpoint-algorithm for recursion
  private boolean recursionSeen = false;
  private boolean resultStatesChanged = false;
  private boolean targetFound = false;
  private final Collection<AbstractState> potentialRecursionUpdateStates = new LinkedHashSet<>();

  public BAMTransferRelationWithFixPointForRecursion(
      Configuration pConfig,
      BAMCPA pBamCpa,
      ShutdownNotifier pShutdownNotifier,
      AlgorithmFactory pAlgorithmFactory,
      BAMPCCManager pBamPccManager,
      boolean pSearchTargetStatesOnExit)
      throws InvalidConfigurationException {
    super(pBamCpa, pShutdownNotifier, pAlgorithmFactory, pBamPccManager, pSearchTargetStatesOnExit);
    pConfig.inject(this);
    bamCpa = pBamCpa;
  }

  @Override
  protected Collection<? extends AbstractState> getAbstractSuccessorsWithoutWrapping(
      final AbstractState pState, final Precision pPrecision)
          throws CPAException, InterruptedException {

    final CFANode node = extractLocation(pState);

    if (stack.isEmpty() && isHeadOfMainFunction(node)) {
      // we are at the start of the program (root-node of CFA).
      return doFixpointIterationForRecursion(pState, pPrecision, node);
    }

    if (maximalDepthForExplicitRecursion != -1 && stack.size() > maximalDepthForExplicitRecursion) {
      return ImmutableSet.of();
    }

    return super.getAbstractSuccessorsWithoutWrapping(pState, pPrecision);
  }

  @Override
  protected boolean startNewBlockAnalysis(final ARGState pState, final CFANode node) {
    return partitioning.isCallNode(node)
        && !pState.getParents().isEmpty() // if no parents, we have already started a new block
        && (!partitioning
                .getBlockForCallNode(node)
                .equals(stack.isEmpty() ? null : stack.peek().getThird())
            || isFunctionBlock(partitioning.getBlockForCallNode(node)));
  }

  @Override
  protected boolean exitBlockAnalysis(final ARGState pState, final CFANode node) {
    // special case: returning from a recursive function is only allowed once per state.
    return super.exitBlockAnalysis(pState, node)
        && !data.alreadyReturnedFromSameBlock(
            pState, stack.isEmpty() ? null : stack.peek().getThird());
  }

  /** recursion is handled by callstack-reduction, so we do not check it here. */
  @Override
  protected boolean isRecursiveCall(final CFANode node) {
    return false;
  }

  private Collection<? extends AbstractState> doFixpointIterationForRecursion(
          final AbstractState pHeadOfMainFunctionState, final Precision pPrecision, final CFANode pHeadOfMainFunction)
          throws CPAException, InterruptedException {

    assert isHeadOfMainFunction(pHeadOfMainFunction) && stack.isEmpty();

    Collection<AbstractState> resultStates;
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

      logger.log(Level.FINEST, "Starting recursive analysis of main-block");

      resultStates = doRecursiveAnalysis(pHeadOfMainFunctionState, pPrecision, pHeadOfMainFunction);

      logger.log(Level.FINEST, "Finished recursive analysis of main-block");

      // EITHER: result is an target-state, return it 'as is' and let CEGAR-algorithm perform a refinement, if needed.
      // OR:     we have completely analyzed the main-block and have not found an target-state.
      //         now we check, if we need to unwind recursive calls further until a fixpoint is reached.

      targetFound = Iterables.any(resultStates, AbstractStates::isTargetState);
      if (targetFound) {
        // not really a fixpoint, but we return and let CEGAR check the target-state
        logger.log(Level.INFO, "fixpoint-iteration aborted, because there was a target state.");
        break;
      }

      if (!resultStatesChanged) {
        logger.log(
            Level.INFO,
            "fixpoint-iteration aborted, because we did not get new states (fixpoint reached).");

        // the fixpoint algorithm should have coverage for all states and thus return zero new
        // states.
        // the initially computed successors from pHeadOfMainFunctionState are the successors for
        // the CPA-algorithm.
        List<AbstractState> exitStates =
            new ArrayList<>(((ARGState) pHeadOfMainFunctionState).getChildren());
        assert getStatesNotCoveredBy(resultStates, exitStates).isEmpty()
            : "there should not be any new state.";
        resultStates = exitStates;

        break;
      }

      logger.log(
          Level.INFO, "fixpoint was not reached, starting new iteration", ++iterationCounter);

      reAddStatesForFixPointIteration();

      // continue;
    }

    return resultStates;
  }

  /** update waitlists of all reachedsets to re-explore the previously found recursive function-call. */
  private void reAddStatesForFixPointIteration() {
    for (final AbstractState recursionUpdateState : potentialRecursionUpdateStates) {
      for (final ReachedSet reachedSet : data.getCache().getAllCachedReachedStates()) {
        if (reachedSet.contains(recursionUpdateState)) {
          logger.log(Level.FINEST, "re-adding state", recursionUpdateState);
          reachedSet.reAddToWaitlist(recursionUpdateState);
        }
        // else if (pHeadOfMainFunctionState == recursionUpdateState) {
          // special case: this is the root-state of the whole program, it is in the main-reachedset.
          // we have no possibility to re-add it,because we do not have access to the main-reachedset.
          // however we do not need to re-add it, because it is the initial-state of current transfer-relation.
        // }
      }
    }
  }

  /** returns a covering level or Null, if no such level is found. */
  private Triple<AbstractState, Precision, Block> getCoveringLevel(
      final Triple<AbstractState, Precision, Block> currentLevel)
      throws CPAException, InterruptedException {

    // Iterate over elements of the stack, except current level (index=0)
    for (Triple<AbstractState, Precision, Block> level : Iterables.skip(stack, 1)) {

      if (level.getThird() == currentLevel.getThird()
          // && level.getSecond().equals(currentLevel.getSecond())
          && bamCpa
              .getWrappedCpa()
              .isCoveredByRecursiveState(currentLevel.getFirst(), level.getFirst())) {
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
  private Collection<AbstractState> getStatesNotCoveredBy(@NonNull final Collection<AbstractState> baseStates,
      @NonNull final Collection<AbstractState> coveringStates)
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
  private boolean isCoveredByAny(@NonNull final AbstractState baseState,
                                 @NonNull final Collection<AbstractState> coveringStates)
          throws CPAException, InterruptedException {
    if (coveringStates.contains(baseState)) {
      return true;
    }
    for (final AbstractState coveringState : coveringStates) {
      if (bamCpa.getAbstractDomain().isLessOrEqual(baseState, coveringState)) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected Collection<AbstractState> analyseBlockAndExpand(
      final AbstractState initialState,
      final Precision pPrecision,
      final Block innerSubtree,
      final Block outerSubtree,
      final AbstractState reducedInitialState,
      final Precision reducedInitialPrecision)
      throws CPAException, InterruptedException {

    final CFANode node = AbstractStates.extractLocation(initialState);
    final Collection<AbstractState> resultStates;

    if (!(node instanceof FunctionEntryNode) || isHeadOfMainFunction(node)) {
      // block is a loop-block
      // or we are at the start of the main-block, we do not need the rebuilding-part for main,
      // so we use a simplified version of function-handling and handle it like a loop-block
      resultStates =
          super.analyseBlockAndExpand(
              initialState,
              pPrecision,
              innerSubtree,
              outerSubtree,
              reducedInitialState,
              reducedInitialPrecision);

    } else {
      // function-entry and old block --> begin new block
      // TODO match only recursive function, not all functions

      resultStates =
          analyseBlockAndExpandForRecursion(
              initialState,
              pPrecision,
              node,
              innerSubtree,
              outerSubtree,
              reducedInitialState,
              reducedInitialPrecision);

      // current location is "before" the function-return-edge.
    }

    if (recursionSeen) {
      // we are returning from an recursive call.
      // if we would need to do further analysis for recursion later (fixpoint-analysis!),
      // we need to know, where to update the reachedset --> store initialState for later usage
      potentialRecursionUpdateStates.add(initialState);
    }

    return resultStates;
  }

  private Collection<AbstractState> analyseBlockAndExpandForRecursion(
      final AbstractState initialState,
      final Precision pPrecision,
      final CFANode node,
      final Block innerSubtree,
      final Block outerSubtree,
      final AbstractState reducedInitialState,
      final Precision reducedInitialPrecision)
      throws CPAException, InterruptedException {

    final Collection<AbstractState> expandedFunctionReturnStates;
    final Triple<AbstractState, Precision, Block> coveringLevel = getCoveringLevel(stack.peek());
    if (coveringLevel != null) {
      // if level is twice in stack, we have endless recursion.
      // with current knowledge we would never abort unrolling the recursion.
      // lets skip the function and return only a short "summary" of the function.
      // this summary is the result of a previous analysis of this block from the cache.
      logger.logf(
          Level.FINEST,
          "recursion will cause endless unrolling (with current precision), "
              + "aborting call of function '%s' at state %s",
          node.getFunctionName(),
          reducedInitialState);

      expandedFunctionReturnStates =
          analyseRecursiveBlockAndExpand(
              initialState,
              pPrecision,
              innerSubtree,
              outerSubtree,
              reducedInitialState,
              coveringLevel);

    } else {
      // enter block of function-call and start recursive analysis
      expandedFunctionReturnStates =
          super.analyseBlockAndExpand(
              initialState,
              pPrecision,
              innerSubtree,
              outerSubtree,
              reducedInitialState,
              reducedInitialPrecision);
    }

    // get the rootState, which is defined as the abstract state calling the function
    final AbstractState rootState = Iterables.getOnlyElement(((ARGState)initialState).getParents());

    final Collection<AbstractState> rebuildStates = new ArrayList<>(expandedFunctionReturnStates.size());
    for (final AbstractState expandedState : expandedFunctionReturnStates) {
      rebuildStates.add(getRebuildState(rootState, initialState, expandedState));
    }

    return rebuildStates;
  }

  /**
   * This method analyses a recursive procedure and returns (expanded) abstract states from the
   * cache, that match the current procedure, or an empty set, if no cache entry was found.
   */
  private Collection<AbstractState> analyseRecursiveBlockAndExpand(
      final AbstractState initialState,
      final Precision pPrecision,
      final Block innerSubtree,
      final Block pOuterSubtree,
      final AbstractState pReducedInitialState,
      final Triple<AbstractState, Precision, Block> pCoveringLevel)
      throws CPATransferException, InterruptedException {

    recursionSeen = true;
    // after this point we have to check all returnStates for changes.
    // If no changes are found, we have found the fixpoint.

    // try to get previously computed states from cache
    final BAMCacheEntry entry =
        // argCache.get(reducedInitialState, reducedInitialPrecision, currentBlock);
        data.getCache()
            .get(pCoveringLevel.getFirst(), pCoveringLevel.getSecond(), pCoveringLevel.getThird());
    final ReachedSet reached = entry.getReachedSet();
    final Collection<AbstractState> previousResult = entry.getExitStates();
    final Collection<AbstractState> reducedResult;

    assert reached != null : "cached entry has no reached set";
    if (previousResult == null) {
      // outer block was not finished, abort recursion
      reducedResult = ImmutableSet.of();
      logger.logf(
          Level.FINEST,
          "skipping recursive call with new empty result (root is %s)",
          reached.getFirstState());
    } else {
      // use previously computed outer block as inner block,
      // this is equal to 'add one recursive step' in the recursion
      reducedResult = previousResult;
      logger.logf(
          Level.FINEST,
          "skipping recursive call with cached result (root is %s)",
          reached.getFirstState());
    }

    registerInitalAndExitStates(initialState, reducedResult, reached);

    if (bamPccManager.isPCCEnabled()) {
      bamPccManager.addBlockAnalysisInfo(pReducedInitialState);
    }

    return expandResultStates(
        reducedResult, reached, innerSubtree, pOuterSubtree, initialState, pPrecision);
  }

  /** We try to get a smaller set of states for further analysis.
   * We check, which states were already analyzed further in a previous iteration of the fixpoint-algorithm.
   * Those states are excluded from further analysis. */
  @Override
  protected Collection<AbstractState> filterResultStatesForFurtherAnalysis(final Collection<AbstractState> reducedResult,
      final Collection<AbstractState> cachedReturnStates) throws CPAException, InterruptedException {
    if (cachedReturnStates == null) {
      logger.log(Level.FINEST, "there was no cache-entry for result-states.");
      resultStatesChanged = true;
    } else {
      // this is the result from a previous analysis of a recursive function-call
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
    }
    return super.filterResultStatesForFurtherAnalysis(reducedResult, cachedReturnStates);
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

    // also clean up local data structures
    data.replaceStateInCaches(expandedState, rebuildState, true);

    return rebuildState;
  }
}
