// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.argReplay;

import com.google.common.collect.Sets;
import java.util.Set;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

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
    if (equals(other)) {
      return this;
    }
    return new ARGReplayState(Sets.union(states, other.states), cpa);
  }

  @Override
  public boolean isLessOrEqual(ARGReplayState other) {
    if (equals(other)) {
      return true;
    }
    return other.states.containsAll(states);
  }

  @Override
  public int hashCode() {
    return states.hashCode() + 31 * cpa.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ARGReplayState
        && states.equals(((ARGReplayState) other).states)
        && cpa.equals(((ARGReplayState) other).cpa);
  }
}
