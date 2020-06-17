// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.util.Set;
import java.util.stream.Collectors;

public class AutomatonSpecification {
  public String automatonName;
  public Set<StateSpecification> stateSpecifications;
  public Set<TransitionSpecification> transitions;
  public Set<String> initialStates;

  private AutomatonSpecification(
      String pAutomatonName,
      Set<StateSpecification> pStateSpecifications,
      Set<TransitionSpecification> pTransitions,
      Set<String> pInitialStates) {
    automatonName = pAutomatonName;
    stateSpecifications = pStateSpecifications;
    transitions = pTransitions;
    initialStates = pInitialStates;
  }

  public static class Builder {
    private String automatonName;
    private Set<StateSpecification> stateSpecifications;
    private Set<TransitionSpecification> transitions;
    private Set<String> initialStates;

    public Builder automatonName(String pAutomatonName) {
      automatonName = checkNotNull(pAutomatonName);
      checkArgument(!automatonName.isEmpty(), "Empty automaton names are not allowed");
      return this;
    }

    public Builder stateSpecifications(Set<StateSpecification> pStateSpecifications) {
      stateSpecifications = checkNotNull(pStateSpecifications);
      return this;
    }

    public Builder transitions(Set<TransitionSpecification> pTransitions) {
      transitions = checkNotNull(pTransitions);
      return this;
    }

    public Builder initialStates(Set<String> pInitialStates) {
      initialStates = checkNotNull(pInitialStates);
      checkArgument(!initialStates.contains(""), "Empty state names are not allowed");
      return this;
    }

    public AutomatonSpecification build() {
      checkNotNull(automatonName);
      checkNotNull(stateSpecifications);
      checkNotNull(transitions);
      checkNotNull(initialStates);

      var statesInTransitions =
          transitions.stream()
              .flatMap(trans -> ImmutableList.of(trans.source, trans.target).stream())
              .collect(Collectors.toSet());
      var stateNames =
          stateSpecifications.stream().map(state -> state.name).collect(Collectors.toSet());

      var unspecifiedTransitionStates = Sets.difference(statesInTransitions, stateNames);
      checkState(
          unspecifiedTransitionStates.isEmpty(),
          "Unspecified states "
              + String.join(", ", unspecifiedTransitionStates)
              + " cannot appear as source or target state in automaton "
              + automatonName);

      var unspecifiedInitialStates = Sets.difference(initialStates, stateNames);
      checkState(
          unspecifiedInitialStates.isEmpty(),
          "Unspecified states "
              + String.join(", ", unspecifiedTransitionStates)
              + " cannot appear as initial state in automaton "
              + automatonName);

      checkState(
          !initialStates.isEmpty(),
          "Automaton " + automatonName + " needs at least one initial state");
      checkState(
          !stateSpecifications.isEmpty(),
          "Automaton " + automatonName + " needs at least one state");

      return new AutomatonSpecification(
          automatonName, stateSpecifications, transitions, initialStates);
    }
  }
}
