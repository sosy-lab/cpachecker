// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.giacombiner;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonStateTypes;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class GIACombinerState
    implements LatticeAbstractState<GIACombinerState>, Serializable, Graphable {

  private static final long serialVersionUID = -7715698130885641252L;

  private final AbstractGIAState stateOfAutomaton1;
  private final AbstractGIAState stateOfAutomaton2;
  private final Map<GIATransition, GIACombinerState> successors;

  public GIACombinerState(
      AbstractGIAState pStateOfAutomaton1, AbstractGIAState pStateOfAutomaton2) {
    stateOfAutomaton1 = pStateOfAutomaton1;
    stateOfAutomaton2 = pStateOfAutomaton2;
    successors = new HashMap<>();
  }

  public Map<GIATransition, GIACombinerState> getSuccessors() {
    return successors;
  }

  public void addSuccessor(GIATransition pTransition, GIACombinerState pSuccessor)
      throws CPATransferException {
    if (this.successors.containsKey(pTransition)) {
      if (!this.successors.get(pTransition).equals(pSuccessor)) {
        throw new CPATransferException("Cannot have two transitions to different successors");
      } else {
        // nothing to do, as already contianed
      }
    } else {
      successors.put(pTransition, pSuccessor);
    }
  }

  @Override
  public GIACombinerState join(GIACombinerState other) throws CPAException, InterruptedException {
    if (this.isLessOrEqual(other)) return other;
    if (Objects.equals(this.stateOfAutomaton1, other.stateOfAutomaton1)
        && Objects.equals(this.stateOfAutomaton2, other.stateOfAutomaton2)) {
      GIACombinerState newState = new GIACombinerState(stateOfAutomaton1, stateOfAutomaton2);
      for (Entry<GIATransition, GIACombinerState> automatonTransitionGIACombinerStateEntry :
          successors.entrySet()) {
        newState.addSuccessor(
            automatonTransitionGIACombinerStateEntry.getKey(),
            automatonTransitionGIACombinerStateEntry.getValue());
      }
      for (Entry<GIATransition, GIACombinerState> e : other.successors.entrySet()) {
        newState.addSuccessor(e.getKey(), e.getValue());
      }
    }
    throw new CPAException(
        String.format(
            "Cannot merge the two states %s and %s", this.toDOTLabel(), other.toDOTLabel()));
  }

  @Override
  public boolean isLessOrEqual(GIACombinerState other) throws CPAException, InterruptedException {
    return Objects.equals(this.stateOfAutomaton1, other.stateOfAutomaton1)
        && Objects.equals(this.stateOfAutomaton2, other.stateOfAutomaton2)
        && this.successors.entrySet().stream()
            .allMatch(e -> e.getValue().equals(other.successors.get(e.getKey())));
  }


  @Override
  public String toString() {
    return toDOTLabel();
  }

  @Override
  public String toDOTLabel() {
    String resString = "";
    resString = resString.concat(this.stateOfAutomaton1.toString());
    resString = resString.concat("_");
    resString = resString.concat(this.stateOfAutomaton2.toString());
    return resString;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (Objects.isNull(pO) || !(pO instanceof GIACombinerState)) {
      return false;
    }
    return Objects.equals(this.stateOfAutomaton1, ((GIACombinerState) pO).stateOfAutomaton1)
        && Objects.equals(this.stateOfAutomaton2, ((GIACombinerState) pO).stateOfAutomaton2)
        && Objects.equals(this.successors, ((GIACombinerState) pO).successors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(stateOfAutomaton1, stateOfAutomaton2, successors);
  }

  public AbstractGIAState getStateOfAutomaton1() {
    return stateOfAutomaton1;
  }

  public AbstractGIAState getStateOfAutomaton2() {
    return stateOfAutomaton2;
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  public boolean isPresent(AutomatonStateTypes pType) {
    return (stateOfAutomaton1.statePresent()
            && ((GIAInternalState) stateOfAutomaton1)
                .getAutomatonState()
                .getInternalState()
                .getStateType()
                .equals(pType))
        || (stateOfAutomaton2.statePresent()
            && ((GIAInternalState) stateOfAutomaton2)
                .getAutomatonState()
                .getInternalState()
                .getStateType()
                .equals(pType));
  }
}
