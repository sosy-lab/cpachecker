// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification;

import java.util.Set;

public class ModuleSpecification {
  public Boolean isRoot;
  public Set<VariableSpecification> variables;
  public Set<ModuleInstantiation> instantiations;
  public AutomatonSpecification automaton;
  public String moduleName;

  private ModuleSpecification(
      Boolean pIsRoot,
      Set<VariableSpecification> pVariables,
      Set<ModuleInstantiation> pInstantiations,
      AutomatonSpecification pAutomaton,
      String pModuleName) {
    isRoot = pIsRoot;
    variables = pVariables;
    instantiations = pInstantiations;
    automaton = pAutomaton;
    moduleName = pModuleName;
  }

  public static class Builder {
    private Boolean isRoot;
    private Set<VariableSpecification> variables;
    private Set<ModuleInstantiation> instantiations;
    private AutomatonSpecification automaton;
    private String moduleName;

    public Builder isRoot(Boolean pIsRoot) {
      isRoot = pIsRoot;
      return this;
    }

    public Builder variables(Set<VariableSpecification> pVariables) {
      variables = pVariables;
      return this;
    }

    public Builder instantiations(Set<ModuleInstantiation> pInstantiations) {
      instantiations = pInstantiations;
      return this;
    }

    public Builder automaton(AutomatonSpecification pAutomaton) {
      automaton = pAutomaton;
      return this;
    }

    public Builder moduleName(String pModuleName) {
      moduleName = pModuleName;
      return this;
    }

    public ModuleSpecification build() {
      return new ModuleSpecification(isRoot, variables, instantiations, automaton, moduleName);
    }
  }
}
