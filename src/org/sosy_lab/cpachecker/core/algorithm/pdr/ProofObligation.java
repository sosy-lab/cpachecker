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
package org.sosy_lab.cpachecker.core.algorithm.pdr;

import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.solver.api.BooleanFormula;

import java.util.Objects;

/**
 * Represents the attempt to prove that a state is unreachable from a location at a certain frame level.
 */
public class ProofObligation implements Comparable<ProofObligation> {

  /** The state to be blocked. */
  private final BooleanFormula state;

  /** The location the state should be blocked in. */
  private final CFANode location;

  /** The frame level the state should be blocked in. */
  private final int frameLevel;

  /** The obligation that caused this one to be created. */
  private final Optional<ProofObligation> cause;

  /**
   * Creates a new ProofObligation that says: Try to prove that {@code pState} can't be reached
   * from {@code pLocation} in at most {@code pFrameLevel} steps.
   * @param pFrameLevel The level where the state should be blocked.
   * @param pLocation The location where the state should be blocked.
   * @param pState The state to be blocked.
   * @param pCause The ProofObligation that lead to the creation of this one.
   */
  public ProofObligation(
      int pFrameLevel, CFANode pLocation, BooleanFormula pState, ProofObligation pCause) {
    this(pFrameLevel, pLocation, pState, Optional.of(pCause));
  }

  /**
   * Creates a new ProofObligation that says: Try to prove that {@code pState} can't be reached
   * from {@code pLocation} in at most {@code pFrameLevel} steps.
   * @param pFrameLevel The level where the state should be blocked.
   * @param pLocation The location where the state should be blocked.
   * @param pState The state to be blocked.
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

  public int getFrameLevel() {
    return frameLevel;
  }

  public CFANode getLocation() {
    return location;
  }

  public BooleanFormula getState() {
    return state;
  }

  public Optional<ProofObligation> getCause() {
    return cause;
  }

  /**
   * Compares this ProofObligation to another one based on the difference of their frame levels.
   * This method returns precisely returns this.getFrameLevel() - pOther.getFrameLevel().
   * @see #getFrameLevel()
   */
  @Override
  public int compareTo(ProofObligation pOther) {
    return Integer.compare(frameLevel, pOther.frameLevel);
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
        && this.location.equals(otherObl.getLocation())
        && this.cause.equals(otherObl.getCause());
  }

  @Override
  public int hashCode() {
    return Objects.hash(frameLevel, location, state, cause);
  }
}
