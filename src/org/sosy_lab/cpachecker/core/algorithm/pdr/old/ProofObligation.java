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
package org.sosy_lab.cpachecker.core.algorithm.pdr.old;

import com.google.common.base.Optional;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * Represents the attempt to prove that a state is unreachable from a location at a certain frame
 * level.
 */
public class ProofObligation implements Comparable<ProofObligation> {

  /** The state to be blocked. */
  private final BooleanFormula state;

  /** The location the state should be blocked at. */
  private final CFANode location;

  /** The frame level the state should be blocked at. */
  private final int frameLevel;

  /** The obligation that caused this one to be created. */
  private final Optional<ProofObligation> cause;

  /**
   * Creates a new ProofObligation that says: Try to prove that {@code pState} can't be reached from
   * {@code pLocation} in at most {@code pFrameLevel} steps. The ProofObligation {@code pCause} is
   * one that could not be resolved previously and lead to the creation of this one.
   *
   * <p>If the created ProofObligation should not have a cause associated with it, use {@link
   * #ProofObligation(int, CFANode, BooleanFormula)} instead.
   *
   * @param pFrameLevel the level the state should be blocked at
   * @param pLocation the location the state should be blocked at
   * @param pState the state to be blocked
   * @param pCause the ProofObligation that lead to the creation of this one
   */
  public ProofObligation(
      int pFrameLevel, CFANode pLocation, BooleanFormula pState, ProofObligation pCause) {
    this(pFrameLevel, pLocation, pState, Optional.of(pCause));
  }

  /**
   * Creates a new ProofObligation that says: Try to prove that {@code pState} can't be reached from
   * {@code pLocation} in at most {@code pFrameLevel} steps. It doesn't have a cause associated with
   * it (see {@link #getCause()}).
   *
   * <p>If the created ProofObligation should have a cause, use {@link #ProofObligation(int,
   * CFANode, BooleanFormula, ProofObligation)} instead.
   *
   * @param pFrameLevel the level the state should be blocked at
   * @param pLocation the location the state should be blocked at
   * @param pState the state to be blocked
   */
  public ProofObligation(int pFrameLevel, CFANode pLocation, BooleanFormula pState) {
    this(pFrameLevel, pLocation, pState, Optional.<ProofObligation>absent());

  }

  private ProofObligation(
      int pFrameLevel, CFANode pLocation, BooleanFormula pState, Optional<ProofObligation> pCause) {
    frameLevel = pFrameLevel;
    location = pLocation;
    state = pState;
    cause = pCause;
  }

  /**
   * Returns the frame level component of this ProofObligation. It defines the level the state
   * should be blocked at.
   * @return the frame level
   */
  public int getFrameLevel() {
    return frameLevel;
  }

  /**
   * Returns the location component of this ProofObligation. It defines the location the state
   * should be blocked at.
   * @return the CFANode representing the program location
   */
  public CFANode getLocation() {
    return location;
  }

  /**
   * Returns the state component of this ProofObligation. It defines the of the state to be blocked.
   *
   * @return the BooleanFormula representing state to be blocked
   */
  public BooleanFormula getState() {
    return state;
  }

  /**
   * Returns the ProofObligation that is the predecessor of this one. It is the cause for the
   * creation of this one. There may not exist such a predecessor if this ProofObligation is
   * the initial one.
   * @return an Optional containing the ProofObligation representing the cause for the creation
   * of this one, or an empty Optional if a cause doesn't exist
   */
  public Optional<ProofObligation> getCause() {
    return cause;
  }

  /**
   * Compares this ProofObligation to another one based on the difference of their frame levels.
   * This method precisely returns {@code this.getFrameLevel() - pOther.getFrameLevel()}.
   * @param pOther the ProofObligation this one should be compared to
   * @return the difference of frame levels between this ProofObligation and the other one
   * @see #getFrameLevel()
   */
  @Override
  public int compareTo(ProofObligation pOther) {
    return Integer.compare(frameLevel, pOther.getFrameLevel());
  }

  @Override
  public String toString() {
    return String.format("{Level = %s, Location = %s, State = %s}", frameLevel, location, state);
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
        && this.location.equals(otherObl.getLocation())
        && this.state.equals(otherObl.getState())
        && this.cause.equals(otherObl.getCause());
  }

  @Override
  public int hashCode() {
    return Objects.hash(frameLevel, location, state, cause);
  }
}
