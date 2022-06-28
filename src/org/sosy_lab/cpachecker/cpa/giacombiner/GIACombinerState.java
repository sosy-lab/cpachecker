// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.giacombiner;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonStateTypes;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class GIACombinerState
    implements LatticeAbstractState<GIACombinerState>, Serializable, Graphable {

  private static final long serialVersionUID = -7715698130885641252L;

  private final AbstractGIAState stateOfAutomaton1;
  private final AbstractGIAState stateOfAutomaton2;
  private final Multimap<GIATransition, GIACombinerState> successors;

  public GIACombinerState(
      AbstractGIAState pStateOfAutomaton1, AbstractGIAState pStateOfAutomaton2) {
    stateOfAutomaton1 = pStateOfAutomaton1;
    stateOfAutomaton2 = pStateOfAutomaton2;
    successors = HashMultimap.create();
  }

  public Map<GIATransition, Collection<GIACombinerState>> getSuccessors() {
    return successors.asMap();
  }

  public void addSuccessor(GIATransition pTransition, GIACombinerState pSuccessor) {
    successors.put(pTransition, pSuccessor);
  }

  @Override
  public GIACombinerState join(GIACombinerState other) throws CPAException, InterruptedException {
    if (this.isLessOrEqual(other)) {
      return other;
    }
    if (Objects.equals(this.stateOfAutomaton1, other.stateOfAutomaton1)
        && Objects.equals(this.stateOfAutomaton2, other.stateOfAutomaton2)) {
      GIACombinerState newState = new GIACombinerState(stateOfAutomaton1, stateOfAutomaton2);
      for (Entry<GIATransition, GIACombinerState> automatonTransitionGIACombinerStateEntry :
          successors.entries()) {
        newState.addSuccessor(
            automatonTransitionGIACombinerStateEntry.getKey(),
            automatonTransitionGIACombinerStateEntry.getValue());
      }
      for (Entry<GIATransition, GIACombinerState> e : other.successors.entries()) {
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
        && other.successors.entries().containsAll(this.successors.entries());
  }

  @Override
  public String toString() {
    return toDOTLabel();
  }

  @Override
  public String toDOTLabel() {
    String resString = "";
    resString = resString + this.stateOfAutomaton1.toString();
    resString = resString + "_";
    resString = resString + this.stateOfAutomaton2.toString();
    return resString;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (!(pO instanceof GIACombinerState)) {
      return false;
    }
    return Objects.equals(this.stateOfAutomaton1, ((GIACombinerState) pO).stateOfAutomaton1)
        && Objects.equals(this.stateOfAutomaton2, ((GIACombinerState) pO).stateOfAutomaton2)
        && this.successors.entries().stream()
            .allMatch(
                e -> ((GIACombinerState) pO).successors.containsEntry(e.getKey(), e.getValue()))
        && ((GIACombinerState) pO)
            .successors.entries().stream()
                .allMatch(e -> this.successors.containsEntry(e.getKey(), e.getValue()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        stateOfAutomaton1,
        stateOfAutomaton2,
        successors.size(),
        successors.entries().parallelStream()
            .map(e -> e.getKey().hashCode() + e.getValue().nonRecHashCode())
            .collect(ImmutableSet.toImmutableSet()));
  }

  private int nonRecHashCode() {
    return Objects.hash(stateOfAutomaton1, stateOfAutomaton2, successors.size());
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
