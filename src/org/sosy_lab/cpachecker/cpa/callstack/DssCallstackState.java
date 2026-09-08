// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.callstack;

import com.google.common.base.Preconditions;
import java.io.Serial;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.PersistentLinkedList;
import org.sosy_lab.common.collect.PersistentList;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * Callstack state for distributed summary synthesis (DSS).
 *
 * <p>A block analysis of DSS may start somewhere in the middle of the program without knowing the
 * callstack at the beginning of its block. A state of this class therefore either behaves exactly
 * like a {@link CallstackState} (see {@link CallstackTransferRelation}), or it allows every
 * transfer, i.e., the callstack is never used to prune a path (see {@link
 * DssCallstackTransferRelation}). The block analysis decides this per state: it allows all
 * transfers exactly if it explores a block without knowing the callstack of the block entry.
 *
 * <p>In addition, a state of this class records all CFA edges that {@link
 * DssCallstackTransferRelation} traversed to reach it. Whenever a block end is reached, this list
 * allows to replay the path of the block backwards, starting from the callstack of the violation
 * condition that the successor block sent. Only such a backwards replay can tell whether the path
 * through the block is feasible with respect to the callstack.
 *
 * <p>Every state of this class wraps the {@link CallstackState} that an ordinary callstack analysis
 * would have at the same point, and it forwards {@link #equals(Object)} and {@link #hashCode()} to
 * that wrapped state. This is crucial for the stop operator: {@link CallstackTransferRelation}
 * deliberately reuses state objects (it returns the given state for most edges and the previous
 * state of the stack on a function return), and the identity-based equality of {@link
 * CallstackState} relies on that. Since the wrapped states are created by {@link
 * CallstackTransferRelation} itself, two DSS states are equal exactly if the two states of an
 * ordinary callstack analysis would be. Neither the recorded edges nor {@link #canBeTopState()}
 * take part in the comparison: including the edges would prevent coverage of two states at the same
 * program location, so the analysis of a block with a loop would not terminate.
 */
public class DssCallstackState extends CallstackState {

  @Serial private static final long serialVersionUID = -8623434399412295045L;

  /**
   * The state that an ordinary callstack analysis would have at this point. It is never a {@link
   * DssCallstackState} and it defines the identity of this state.
   */
  private final CallstackState wrappedState;

  /**
   * All edges traversed since the analysis started, most recent edge first (i.e., already in the
   * order that a backwards analysis requires).
   */
  private final PersistentList<CFAEdge> reversedTraversedEdges;

  private final boolean canBeTopState;

  public DssCallstackState(CallstackState pWrappedState, boolean pCanBeTopState) {
    this(pWrappedState, pCanBeTopState, PersistentLinkedList.of());
  }

  private DssCallstackState(
      CallstackState pWrappedState,
      boolean pCanBeTopState,
      PersistentList<CFAEdge> pReversedTraversedEdges) {
    super(
        pWrappedState.getPreviousState(),
        pWrappedState.getCurrentFunction(),
        pWrappedState.getCallNode());
    Preconditions.checkArgument(
        !(pWrappedState instanceof DssCallstackState),
        "DSS callstack states must not be nested: %s",
        pWrappedState);
    wrappedState = pWrappedState;
    canBeTopState = pCanBeTopState;
    reversedTraversedEdges = pReversedTraversedEdges;
  }

  /** Returns the given state itself, or the state that it wraps if it is a DSS callstack state. */
  public static @Nullable CallstackState unwrap(@Nullable CallstackState pState) {
    return pState instanceof DssCallstackState dssState ? dssState.wrappedState : pState;
  }

  /** Returns the state that an ordinary callstack analysis would have at this point. */
  public CallstackState getWrappedState() {
    return wrappedState;
  }

  /**
   * Whether the transfer relation may apply every CFA edge to this state when the stack only
   * contains its initial state
   *
   * @return {@code true} if the callstack should prune a transfer when the stack is one state only,
   *     {@code false} if this state behaves exactly like a {@link CallstackState}.
   */
  public boolean canBeTopState() {
    return canBeTopState;
  }

  public boolean isTopState() {
    return canBeTopState() && wrappedState.previousState == null;
  }

  /**
   * Returns all traversed edges in reverse order, i.e., in the order in which a backwards analysis
   * has to process them.
   */
  public List<CFAEdge> getReversedTraversedEdges() {
    return reversedTraversedEdges;
  }

  /**
   * Returns a copy of this state that additionally recorded the given edge. The callstack itself
   * remains unchanged, so the returned state is equal to this state.
   */
  public DssCallstackState withTraversedEdge(CFAEdge pEdge) {
    return withWrappedStateAndTraversedEdge(wrappedState, pEdge);
  }

  /**
   * Returns a state that wraps the given callstack state and that recorded all edges that this
   * state traversed plus the given edge.
   *
   * @param pWrappedState the successor that {@link CallstackTransferRelation} computed for {@link
   *     #getWrappedState()}
   * @param pEdge the edge that was traversed to obtain {@code pWrappedState} from this state
   */
  public DssCallstackState withWrappedStateAndTraversedEdge(
      CallstackState pWrappedState, CFAEdge pEdge) {
    return new DssCallstackState(pWrappedState, canBeTopState, reversedTraversedEdges.with(pEdge));
  }

  @Override
  public boolean equals(@Nullable Object pOther) {
    // identity of the wrapped state, exactly like the equality of an ordinary callstack analysis
    return this == pOther
        || (pOther instanceof DssCallstackState other && wrappedState.equals(other.wrappedState));
  }

  @Override
  public int hashCode() {
    return wrappedState.hashCode();
  }

  @Override
  public String toString() {
    return super.toString() + ", traversed edges " + reversedTraversedEdges.size();
  }
}
