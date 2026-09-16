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
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
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
 * <p>The current stack, unknown-stack mode, and backwards callstack effect determine local
 * equality. The effect records exactly the information needed to check a successor block's
 * callstack at the ghost edge; execution paths belong to the ARG. Distributed precondition coverage
 * has its own relation and deliberately ignores this local effect.
 */
public class DssCallstackState extends CallstackState {

  @Serial private static final long serialVersionUID = -8623434399412295045L;

  /**
   * The state that an ordinary callstack analysis would have at this point. It is never a {@link
   * DssCallstackState}. DSS compares its stack contents with the proof-checking equivalence.
   */
  private final CallstackState wrappedState;

  private final boolean canBeTopState;
  private final DssCallstackEffect effect;

  public DssCallstackState(CallstackState pWrappedState, boolean pCanBeTopState) {
    this(pWrappedState, pCanBeTopState, DssCallstackEffect.EMPTY);
  }

  private DssCallstackState(
      CallstackState pWrappedState, boolean pCanBeTopState, DssCallstackEffect pEffect) {
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
    effect = pEffect;
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
   * @return {@code true} if an unknown caller may be traversed when only the initial frame remains
   */
  public boolean canBeTopState() {
    return canBeTopState;
  }

  public boolean isTopState() {
    return canBeTopState() && wrappedState.previousState == null;
  }

  DssCallstackEffect getEffect() {
    return effect;
  }

  /** Start another block exploration with the same stack and no replay history. */
  public DssCallstackState reset() {
    return new DssCallstackState(wrappedState, canBeTopState);
  }

  /** Records another edge without changing the current stack. The backwards effect may change. */
  public DssCallstackState withTraversedEdge(CFAEdge pEdge) {
    return withWrappedStateAndTraversedEdge(wrappedState, pEdge);
  }

  /**
   * Returns a state that wraps the given callstack and extends this state's backwards effect by the
   * given edge.
   *
   * @param pWrappedState the successor that {@link CallstackTransferRelation} computed for {@link
   *     #getWrappedState()}
   * @param pEdge the edge that was traversed to obtain {@code pWrappedState} from this state
   */
  public DssCallstackState withWrappedStateAndTraversedEdge(
      CallstackState pWrappedState, CFAEdge pEdge) {
    return new DssCallstackState(pWrappedState, canBeTopState, effect.append(pEdge));
  }

  @Override
  public boolean equals(@Nullable Object pOther) {
    return this == pOther
        || (pOther instanceof DssCallstackState other
            && wrappedState.sameStateInProofChecking(other.wrappedState)
            && canBeTopState == other.canBeTopState
            && effect.equals(other.effect));
  }

  @Override
  public int hashCode() {
    return Objects.hash(new CallstackStateEqualsWrapper(wrappedState), canBeTopState, effect);
  }

  @Override
  public String toString() {
    return super.toString() + ", unknown entry stack " + canBeTopState;
  }
}
