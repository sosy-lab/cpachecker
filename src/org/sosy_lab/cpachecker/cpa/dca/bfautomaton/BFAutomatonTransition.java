/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.dca.bfautomaton;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class BFAutomatonTransition {

  private final ImmutableList<BooleanFormula> assumptions;
  private final String followStateName;
  private BFAutomatonState followState = null;

  public BFAutomatonTransition(String pFollowStateName, BooleanFormula pAssumption) {
    this(pFollowStateName, ImmutableList.of(pAssumption));
  }

  public BFAutomatonTransition(String pFollowStateName, List<BooleanFormula> pAssumptions) {
    followStateName = checkNotNull(pFollowStateName);
    assumptions = ImmutableList.copyOf(pAssumptions);
  }

  public BFAutomatonTransition(
      String pFollowStateName,
      BFAutomatonState pFollowState,
      ImmutableList<BooleanFormula> pAssumptions) {
    this(pFollowStateName, pAssumptions);
    followState = checkNotNull(pFollowState);
  }

  /**
   * Resolves the follow-state relation for this transition.
   */
  void setFollowState(Map<String, BFAutomatonState> pAllStates) throws BFAutomatonException {
    if (followState != null) {
      return;
    }

    followState = pAllStates.get(followStateName);
    if (followState == null) {
      throw new BFAutomatonException("No follow-state with name " + followStateName + " found.");
    }
  }

  public BFAutomatonState getFollowState() {
    if (followState == null) {
      throw new IllegalStateException("No follow-state has been set.");
    }
    return followState;
  }

  public ImmutableList<BooleanFormula> getAssumptions() {
    return assumptions;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("MATCH BF  {");
    sb.append(Joiner.on("; ").join(Collections2.transform(assumptions, BooleanFormula::toString)));
    sb.append("}  ->  GOTO ");
    sb.append(followStateName);
    return sb.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((assumptions == null) ? 0 : assumptions.hashCode());
    result = prime * result + ((followStateName == null) ? 0 : followStateName.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    BFAutomatonTransition other = (BFAutomatonTransition) obj;
    if (assumptions == null) {
      if (other.assumptions != null) {
        return false;
      }
    } else if (!assumptions.equals(other.assumptions)) {
      return false;
    }
    if (followStateName == null) {
      if (other.followStateName != null) {
        return false;
      }
    } else if (!followStateName.equals(other.followStateName)) {
      return false;
    }
    if (followState == null) {
      if (other.followState != null) {
        return false;
      }
    } else if (!followState.equals(other.followState)) {
      return false;
    }
    return true;
  }

}
