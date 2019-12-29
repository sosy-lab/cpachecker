/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.argReplay;

import java.util.Set;

import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

import com.google.common.collect.Sets;

/** Abstract state of a powerset domain. */
public class ARGReplayState implements LatticeAbstractState<ARGReplayState> {

  private final Set<ARGState> states;

  private final ConfigurableProgramAnalysis cpa;

  ARGReplayState(Set<ARGState> states, ConfigurableProgramAnalysis cpa) {
    this.states = states;
    this.cpa = cpa;
  }

  public Set<ARGState> getStates() {
    return states;
  }

  public ConfigurableProgramAnalysis getCPA() {
    return cpa;
  }

  @Override
  public String toString() {
    return String.format("(%s)", states).replace("\n", "\n    ");
  }

  @Override
  public ARGReplayState join(ARGReplayState other) {
    if (this == other) {
      return this;
    }
    return new ARGReplayState(Sets.union(this.states, other.states), cpa);
  }

  @Override
  public boolean isLessOrEqual(ARGReplayState other) {
    if (this == other) {
      return true;
    }
    return other.states.containsAll(this.states);
  }

  @Override
  public int hashCode() {
    return states.hashCode() + 31 * cpa.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ARGReplayState
        && this.states.equals(((ARGReplayState)other).states)
        && this.cpa.equals(((ARGReplayState)other).cpa);
  }
}
