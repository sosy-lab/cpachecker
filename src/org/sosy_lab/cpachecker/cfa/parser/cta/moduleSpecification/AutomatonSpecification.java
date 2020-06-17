// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification;

import java.util.Set;

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
      automatonName = pAutomatonName;
      return this;
    }

    public Builder stateSpecifications(Set<StateSpecification> pStateSpecifications) {
      stateSpecifications = pStateSpecifications;
      return this;
    }

    public Builder transitions(Set<TransitionSpecification> pTransitions) {
      transitions = pTransitions;
      return this;
    }

    public Builder initialStates(Set<String> pInitialStates) {
      initialStates = pInitialStates;
      return this;
    }

    public AutomatonSpecification build() {
      return new AutomatonSpecification(
          automatonName, stateSpecifications, transitions, initialStates);
    }
  }
}
