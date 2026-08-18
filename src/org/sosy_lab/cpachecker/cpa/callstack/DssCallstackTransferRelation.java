// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.callstack;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.block.BlockState.BlockStateType;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Transfer relation for {@link DssCallstackState}.
 *
 * <p>The transfer relation records every traversed edge in the successor state. Depending on {@link
 * DssCallstackState#allowsAllTransfers()}, it either applies the standard callstack semantics of
 * {@link CallstackTransferRelation} or it applies every edge without inspecting the callstack. Even
 * a state that allows all transfers uses the standard semantics for edges that enter or leave a
 * function, though (see {@link #changesCallstack(CFAEdge)}).
 *
 * <p>The information that the callstack of a block analysis is missing is only available at the end
 * of a block: the violation condition that the successor block sent contains the callstack at the
 * block end. Whenever the ghost edge of a block (see {@link BlockGraph#GHOST_EDGE_DESCRIPTION}) is
 * traversed, {@link #strengthen(AbstractState, Iterable, CFAEdge, Precision)} therefore replays all
 * traversed edges backwards, starting from that callstack. If the backwards replay fails, the path
 * through the block does not fit the callstack of the violation condition and is discarded.
 */
public class DssCallstackTransferRelation extends CallstackTransferRelation {

  private final CallstackTransferRelationBackwards backwards;

  public DssCallstackTransferRelation(CallstackOptions pOptions, LogManager pLogger) {
    super(pOptions, pLogger);
    backwards = new CallstackTransferRelationBackwards(pOptions, pLogger);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pElement, Precision pPrecision, CFAEdge pEdge) throws CPATransferException {
    if (!(pElement instanceof DssCallstackState state)) {
      // states that were not created by this CPA do not track edges
      return super.getAbstractSuccessorsForEdge(pElement, pPrecision, pEdge);
    }

    if (state.allowsAllTransfers() && !changesCallstack(pEdge)) {
      // the callstack must not prune any path, but calls to unsupported functions still abort
      if (pEdge instanceof AStatementEdge statementEdge) {
        checkForUnsupportedFunctionCall(statementEdge);
      }
      return ImmutableList.of(state.withTraversedEdge(pEdge));
    }

    // the wrapped state, not this state, is given to the ordinary transfer relation:
    // it reuses state objects (for example, on a function return), which defines the identity
    // of the successors and, thus, whether they can be covered by the stop operator
    ImmutableList.Builder<DssCallstackState> successors = ImmutableList.builder();
    for (AbstractState successor :
        super.getAbstractSuccessorsForEdge(state.getWrappedState(), pPrecision, pEdge)) {
      successors.add(state.withWrappedStateAndTraversedEdge((CallstackState) successor, pEdge));
    }
    return successors.build();
  }

  private static boolean changesCallstack(CFAEdge pEdge) {
    return pEdge instanceof FunctionCallEdge || pEdge instanceof FunctionReturnEdge;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      Iterable<AbstractState> pOtherStates,
      @Nullable CFAEdge pCfaEdge,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {
    if (!(pState instanceof DssCallstackState state) || !isGhostEdge(pCfaEdge)) {
      return ImmutableList.of(pState);
    }

    BlockState blockState =
        FluentIterable.from(pOtherStates)
            .transformAndConcat(AbstractStates::asIterable)
            .filter(BlockState.class)
            .first()
            .orNull();
    if (blockState == null || blockState.getType() != BlockStateType.ABSTRACTION) {
      return ImmutableList.of(pState);
    }

    List<? extends AbstractState> violationConditions = blockState.getViolationConditions();
    if (violationConditions.isEmpty()) {
      // without a violation condition, the callstack at the block end is unknown
      return ImmutableList.of(pState);
    }

    // at abstraction locations, a block state stores exactly one violation condition
    AbstractState violationCondition = Iterables.getOnlyElement(violationConditions);
    CallstackState callstackAtBlockEnd =
        checkNotNull(
            AbstractStates.extractStateByType(violationCondition, CallstackState.class),
            "Violation condition %s does not contain a callstack state",
            violationCondition);

    if (fitsCallstackAtBlockEnd(callstackAtBlockEnd, state, pPrecision)) {
      return ImmutableList.of(state);
    }
    return ImmutableList.of();
  }

  /**
   * Replays all edges that the given state traversed backwards, starting from the callstack at the
   * end of the block.
   *
   * @param pCallstackAtBlockEnd the callstack that the violation condition of the successor block
   *     reports for the end of the current block
   * @param pState the state that reached the end of the current block
   * @param pPrecision the precision to use for the backwards transfer
   * @return whether the path that {@code pState} represents is possible for the given callstack
   */
  private boolean fitsCallstackAtBlockEnd(
      CallstackState pCallstackAtBlockEnd, DssCallstackState pState, Precision pPrecision)
      throws CPATransferException {
    AbstractState current = DssCallstackState.unwrap(pCallstackAtBlockEnd);
    for (CFAEdge edge : pState.getReversedTraversedEdges()) {
      if (isGhostEdge(edge)) {
        // ghost edges are artificial and do not change the callstack
        continue;
      }
      Collection<? extends AbstractState> predecessors =
          backwards.getAbstractSuccessorsForEdge(current, pPrecision, edge);
      if (predecessors.isEmpty()) {
        return false;
      }
      current = Iterables.getOnlyElement(predecessors);
    }
    return true;
  }

  private static boolean isGhostEdge(@Nullable CFAEdge pEdge) {
    return pEdge instanceof BlankEdge blankEdge
        && blankEdge.getDescription().equals(BlockGraph.GHOST_EDGE_DESCRIPTION);
  }
}
