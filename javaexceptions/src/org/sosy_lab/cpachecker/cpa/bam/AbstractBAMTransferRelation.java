// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMDataManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public abstract class AbstractBAMTransferRelation<EX extends CPAException>
    extends AbstractSingleWrapperTransferRelation implements TransferRelation {

  final BAMDataManager data;
  protected final BlockPartitioning partitioning;
  protected final LogManager logger;
  protected final Reducer wrappedReducer;
  protected final ShutdownNotifier shutdownNotifier;

  private final boolean useDynamicAdjustment;

  protected AbstractBAMTransferRelation(
      AbstractBAMCPA pBamCPA, ShutdownNotifier pShutdownNotifier) {
    super(pBamCPA.getWrappedCpa().getTransferRelation());
    logger = pBamCPA.getLogger();
    wrappedReducer = pBamCPA.getReducer();
    data = pBamCPA.getData();
    partitioning = pBamCPA.getBlockPartitioning();
    shutdownNotifier = pShutdownNotifier;
    useDynamicAdjustment = pBamCPA.useDynamicAdjustment();
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      final AbstractState pState, final Precision pPrecision)
      throws CPATransferException, InterruptedException {
    try {

      final Collection<? extends AbstractState> successors =
          getAbstractSuccessorsWithoutWrapping(pState, pPrecision);

      // assert !Iterables.any(successors, IS_TARGET_STATE) || successors.size() == 1
      //    : "target-state should be returned as single-element-collection";

      return successors;

    } catch (CPAException e) {
      throw new RecursiveAnalysisFailedException(e);
    }
  }

  protected Collection<? extends AbstractState> getAbstractSuccessorsWithoutWrapping(
      final AbstractState pState, final Precision pPrecision)
      throws EX, InterruptedException, CPATransferException {
    shutdownNotifier.shutdownIfNecessary();

    final CFANode node = extractLocation(pState);
    final ARGState argState = (ARGState) pState;

    // we are at some location inside the program,
    // this part is always and only reached as recursive call with 'doRecursiveAnalysis'
    // (except we have a full cache-hit).
    if (exitBlockAnalysis(argState, node)) {

      // We are leaving the block, do not perform analysis beyond the current block.
      return ImmutableSet.of();
    }

    if (startNewBlockAnalysis(argState, node)) {

      // we are at the entryNode of a new block and we are in a new context,
      // so we have to start a recursive analysis
      return doRecursiveAnalysis(argState, pPrecision, node);
    }

    // the easy case: we are in the middle of a block, so just forward to wrapped CPAs.
    // if there are several leaving edges, the wrapped CPA should handle all of them.
    return getWrappedTransferSuccessor(argState, pPrecision, node);
  }

  protected abstract Collection<? extends AbstractState> doRecursiveAnalysis(
      AbstractState pState, Precision pPrecision, CFANode pNode) throws EX, InterruptedException;

  /**
   * Return the successor using the wrapped CPA.
   *
   * @param pState current abstract state
   * @param pPrecision current precision
   * @param pNode current location
   * @throws EX thrown in subclass
   */
  protected Collection<? extends AbstractState> getWrappedTransferSuccessor(
      final ARGState pState, final Precision pPrecision, final CFANode pNode)
      throws EX, InterruptedException, CPATransferException {
    return transferRelation.getAbstractSuccessors(pState, pPrecision);
  }

  /**
   * When a block-start-location is reached, we start a new sub-analysis for the entered block.
   *
   * @param pState the abstract state at the location
   * @param node the node of the location
   */
  protected boolean startNewBlockAnalysis(final ARGState pState, final CFANode node) {
    boolean result =
        partitioning.isCallNode(node)
            // at begin of a block, we do not want to enter it again immediately, new block != old
            // block
            // -> state in the middle of a block
            && !pState.getParents().isEmpty()
            // -> start another block (needed for loop-blocks)
            && !partitioning.getBlockForCallNode(node).equals(getBlockForState(pState));

    if (result && useDynamicAdjustment && data.isUncachedBlockEntry(node)) {
      return false;
    }
    return result;
  }

  /**
   * When finding a block-exit-location, we do not return any further states. This stops the current
   * running CPA-algorithm, when its waitlist is emtpy.
   *
   * @param argState the abstract state at the location
   * @param node the node of the location
   */
  protected boolean exitBlockAnalysis(final ARGState argState, final CFANode node) {
    // exit- and start-locations can overlap -> order of statements in calling method.

    return partitioning.isReturnNode(node)
        // multiple exits at same location possible.
        && partitioning.getBlocksForReturnNode(node).contains(getBlockForState(argState));
  }

  /**
   * We assume that the root of a reached-set is a initial state at block-entry-location. Searching
   * backwards from an ARGstate should end in the root-state.
   *
   * <p>This method traverses the reached-set and might be costly. Please call only when needed.
   */
  protected Block getBlockForState(ARGState state) {

    // search backwards for initial states, we assume there is only one
    final Collection<ARGState> finished = new HashSet<>();
    final Deque<ARGState> waitlist = new ArrayDeque<>();
    waitlist.add(state);
    while (!waitlist.isEmpty()) {
      state = waitlist.pop();

      // optimization to skip plain chains
      while (state.getParents().size() == 1) {
        state = state.getParents().iterator().next();
      }

      // initial states in the reached-set have no predecessor
      if (state.getParents().isEmpty()) {
        break;
      }

      if (finished.add(state)) {
        waitlist.addAll(state.getParents());
      }
    }

    CFANode location = extractLocation(state);
    assert partitioning.isCallNode(location)
        : "root of reached-set must be located at block entry.";
    return partitioning.getBlockForCallNode(location);
  }

  static boolean isHeadOfMainFunction(CFANode currentNode) {
    return currentNode instanceof FunctionEntryNode && currentNode.getNumEnteringEdges() == 0;
  }

  static boolean isFunctionBlock(Block block) {
    return block.getCallNodes().size() == 1 && block.getCallNode() instanceof FunctionEntryNode;
  }

  /**
   * Returns expanded states for all reduced states and updates the caches.
   *
   * @param reducedResult pairs of reduced sets associated with the block exit.
   * @param innerSubtree block associated with {@code reducedResult}.
   * @param outerSubtree block above the one associated with {@code reducedResult}.
   * @param state state associated with the block entry.
   * @param precision precision associated with the block entry.
   */
  protected List<AbstractState> expandResultStates(
      final Collection<AbstractState> reducedResult,
      final ReachedSet reached,
      final Block innerSubtree,
      @Nullable final Block outerSubtree,
      final AbstractState state,
      final Precision precision)
      throws InterruptedException {

    logger.log(Level.FINEST, "Expanding states with initial state", state);
    logger.log(Level.FINEST, "Expanding states", reducedResult);

    final List<AbstractState> expandedResult = new ArrayList<>(reducedResult.size());
    for (AbstractState reducedState : reducedResult) {
      Precision reducedPrecision = reached.getPrecision(reducedState);

      AbstractState expandedState =
          wrappedReducer.getVariableExpandedState(state, innerSubtree, reducedState);

      if (expandedState == null) {
        // if the expanded reducedResult is not satisfiable, ignore it.
        // TODO If this happens, we might want to re-analyze
        // the nested reached-set for further states from the waitlist.
        continue;
      }

      Precision expandedPrecision =
          outerSubtree == null
              ? reducedPrecision
              : // special case: return from main
              wrappedReducer.getVariableExpandedPrecision(
                  precision, outerSubtree, reducedPrecision);

      ((ARGState) expandedState).addParent((ARGState) state);
      expandedResult.add(expandedState);

      data.registerExpandedState(expandedState, expandedPrecision, reducedState, innerSubtree);

      if (useDynamicAdjustment && wrappedReducer.canBeUsedInCache(expandedState)) {
        if (expandedState instanceof Targetable && ((Targetable) expandedState).isTarget()) {
          // In case of error location we should look at root state,
          // because the 'target' state is not a real exit of the block
          if (wrappedReducer.canBeUsedInCache(state)) {
            data.addUncachedBlockEntry(innerSubtree.getCallNode());
          }
        } else {
          data.addUncachedBlockEntry(innerSubtree.getCallNode());
        }
      }
    }

    logger.log(Level.FINEST, "Expanded results:", expandedResult);

    return expandedResult;
  }

  protected void registerInitalAndExitStates(
      final AbstractState initialState,
      final Collection<AbstractState> exitStates,
      final ReachedSet reached) {
    assert reached != null;
    for (AbstractState exitState :
        exitStates) { // blocks without an exit are never added to the cache
      data.registerInitialState(initialState, exitState, reached);
    }
  }

  protected boolean isCacheHit(
      ReachedSet cachedReached, Collection<AbstractState> cachedReturnStates) {
    if (cachedReturnStates != null) {
      if (!cachedReached.hasWaitingState()) {
        // cache hit with finished reached-set, return element from cache.
        return true;
      }

      if (cachedReturnStates.size() == 1
          && cachedReached.getLastState() != null
          && AbstractStates.isTargetState(cachedReached.getLastState())) {
        // cache hit with found target state, return element from cache.
        // TODO we currently expect only one target state per reached-set.
        assert Iterables.getOnlyElement(cachedReturnStates) == cachedReached.getLastState()
            : "cache hit only allowed for finished reached-sets or target-states";
        return true;
      }
    }

    return false;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      Iterable<AbstractState> pOtherStates,
      CFAEdge pCfaEdge,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {
    shutdownNotifier.shutdownIfNecessary();
    return transferRelation.strengthen(pState, pOtherStates, pCfaEdge, pPrecision);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge) {

    throw new UnsupportedOperationException(
        "BAMCPA needs to be used as the outermost CPA,"
            + " thus it does not support returning successors for a single edge.");
  }
}
