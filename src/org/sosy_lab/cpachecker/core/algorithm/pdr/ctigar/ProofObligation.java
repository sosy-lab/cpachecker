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
package org.sosy_lab.cpachecker.core.algorithm.pdr.ctigar;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents the attempt to prove that a set of states is unreachable in a certain number of steps.
 */
public class ProofObligation implements Comparable<ProofObligation> {

  /** The states that should be blocked and the corresponding location. */
  private final StatesWithLocation state;

  /** The frame-level the state should be blocked at. */
  private final int frameLevel;

  /** The obligation that caused this one to be created. */
  private final Optional<ProofObligation> cause;

  /**
   * Creates a new ProofObligation that says: Try to prove that {@code pStates} can't be reached in
   * at most {@code pFrameLevel} steps. The ProofObligation {@code pCause} is one that could not be
   * resolved previously and lead to the creation of this one.
   *
   * <p>If the created ProofObligation should not have a cause associated with it, use {@link
   * #ProofObligation(int, StatesWithLocation)} instead.
   *
   * @param pFrameLevel The level the states should be blocked at.
   * @param pStates The states to be blocked.
   * @param pCause The ProofObligation that lead to the creation of this one.
   */
  public ProofObligation(int pFrameLevel, StatesWithLocation pStates, ProofObligation pCause) {
    this(pFrameLevel, pStates, Optional.of(pCause));
  }

  /**
   * Creates a new ProofObligation that says: Try to prove that {@code pStates} can't be reached in
   * at most {@code pFrameLevel} steps. It doesn't have a cause associated with it (see {@link
   * #getCause()}).
   *
   * <p>If the created ProofObligation should have a cause, use {@link #ProofObligation(int,
   * StatesWithLocation, ProofObligation)} instead.
   *
   * @param pFrameLevel The level the states should be blocked at.
   * @param pState The states to be blocked.
   */
  public ProofObligation(int pFrameLevel, StatesWithLocation pState) {
    this(pFrameLevel, pState, Optional.empty());
  }

  private ProofObligation(
      int pFrameLevel, StatesWithLocation pState, Optional<ProofObligation> pCause) {
    frameLevel = Objects.requireNonNull(pFrameLevel);
    state = Objects.requireNonNull(pState);
    cause = Objects.requireNonNull(pCause);
  }

  /**
   * Returns the frame-level the states should be blocked at.
   *
   * @return The frame-level.
   */
  public int getFrameLevel() {
    return frameLevel;
  }

  /**
   * Returns the states to be blocked.
   *
   * @return The states to be blocked.
   */
  public StatesWithLocation getState() {
    return state;
  }

  /**
   * Returns the ProofObligation that is the predecessor of this one. It is the cause for the
   * creation of this one. There may not exist such a predecessor if this ProofObligation is the
   * first one to be created.
   *
   * @return An Optional containing the ProofObligation representing the cause for the creation of
   *     this one, or an empty Optional if a cause doesn't exist.
   */
  public Optional<ProofObligation> getCause() {
    return cause;
  }

  /**
   * Returns a ProofObligation that is identical to this one, but has a frame-level that is
   * incremented by 1. This method doesn't alter the original ProogObligation.
   *
   * @return This ProofObligation with an incremented frame-level.
   */
  public ProofObligation rescheduleToNextLevel() {
    return new ProofObligation(frameLevel + 1, state, cause);
  }

  /**
   * Compares this ProofObligation to another one based on the difference of their frame-levels.
   * This method precisely returns {@code this.getFrameLevel() - pOther.getFrameLevel()}.
   *
   * @param pOther The ProofObligation this one should be compared to.
   * @return The difference of frame-levels between this ProofObligation and the other one.
   * @see #getFrameLevel()
   */
  @Override
  public int compareTo(ProofObligation pOther) {
    return Integer.compare(frameLevel, pOther.getFrameLevel());
  }

  @Override
  public String toString() {
    return String.format("{Level = %s, State = %s}", frameLevel, state.getAbstract());
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ProofObligation)) {
      return false;
    }
    ProofObligation otherObl = (ProofObligation) other;
    return this.frameLevel == otherObl.getFrameLevel()
        && this.state.equals(otherObl.getState())
        && this.cause.equals(otherObl.getCause());
  }

  @Override
  public int hashCode() {
    return Objects.hash(frameLevel, state, cause);
  }
}
