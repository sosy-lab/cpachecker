// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.concurrent;

import static com.google.common.base.Preconditions.checkState;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.util.AbstractStates;

public final class ThreadState extends AbstractSingleWrapperState
    implements AbstractStateWithLocation {

  /**
   * A thread the binder has not looked at yet, cf. {@link ConcurrentState#bindWitnessThreadIds}.
   */
  static final int UNSEEN_WITNESS_THREAD_ID = -3;

  /** A thread that was created but whose identifier in the witness is not known yet. */
  static final int AWAITING_WITNESS_THREAD_ID = -1;

  /** A thread the witness does not refer to, because it passes no waypoint for its creation. */
  static final int NO_WITNESS_THREAD_ID = -2;

  // direct reference to location state as it is needed frequently
  private final AbstractStateWithLocation locationState;

  /**
   * The identifier by which a violation witness refers to this thread, or one of the markers above.
   * A witness numbers only the threads whose creation it passes a waypoint for, so this is not the
   * PID that POR assigns in creation order.
   */
  private final int witnessThreadId;

  public ThreadState(CompositeState pWrappedState) {
    this(pWrappedState, UNSEEN_WITNESS_THREAD_ID);
  }

  private ThreadState(CompositeState pWrappedState, int pWitnessThreadId) {
    super(pWrappedState);
    this.locationState =
        AbstractStates.extractStateByType(pWrappedState, AbstractStateWithLocation.class);
    checkState(this.locationState != null, "No location state found in thread state.");
    witnessThreadId = pWitnessThreadId;
  }

  int getWitnessThreadId() {
    return witnessThreadId;
  }

  /**
   * This thread advanced to {@code pWrappedState}, keeping the identifier the witness refers to it
   * by: a thread does not become a different thread by taking a step.
   */
  ThreadState withWrappedState(CompositeState pWrappedState) {
    return new ThreadState(pWrappedState, witnessThreadId);
  }

  ThreadState withWitnessThreadId(int pWitnessThreadId) {
    return pWitnessThreadId == witnessThreadId
        ? this
        : new ThreadState((CompositeState) getWrappedState(), pWitnessThreadId);
  }

  @Override
  public String toString() {
    return "(loc=%s)".formatted(getLocationNode().getNodeNumber());
  }

  @Override
  public CFANode getLocationNode() {
    return locationState.getLocationNode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    ThreadState other = (ThreadState) obj;
    return witnessThreadId == other.witnessThreadId
        && getWrappedState().equals(other.getWrappedState());
  }

  @Override
  public int hashCode() {
    return 31 * getWrappedState().hashCode() + witnessThreadId;
  }
}
