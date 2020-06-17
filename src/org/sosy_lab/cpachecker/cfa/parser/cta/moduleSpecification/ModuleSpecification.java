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

import com.google.common.base.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ModuleSpecification {
  public Boolean isRoot;
  public Set<VariableSpecification> variables;
  public Set<ModuleInstantiation> instantiations;
  public Optional<AutomatonSpecification> automaton;
  public String moduleName;

  private ModuleSpecification(
      Boolean pIsRoot,
      Set<VariableSpecification> pVariables,
      Set<ModuleInstantiation> pInstantiations,
      Optional<AutomatonSpecification> pAutomaton,
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
    private Optional<AutomatonSpecification> automaton;
    private String moduleName;

    public Builder isRoot(Boolean pIsRoot) {
      isRoot = pIsRoot;
      return this;
    }

    public Builder variables(Set<VariableSpecification> pVariables) {
      variables = checkNotNull(pVariables);
      return this;
    }

    public Builder instantiations(Set<ModuleInstantiation> pInstantiations) {
      instantiations = checkNotNull(pInstantiations);
      return this;
    }

    public Builder automaton(Optional<AutomatonSpecification> pAutomaton) {
      automaton = checkNotNull(pAutomaton);
      return this;
    }

    public Builder moduleName(String pModuleName) {
      moduleName = checkNotNull(pModuleName);
      checkArgument(!moduleName.isEmpty(), "Empty module names are not allowed");
      return this;
    }

    public ModuleSpecification build() {
      checkNotNull(variables);
      checkNotNull(instantiations);
      checkNotNull(automaton);
      checkNotNull(moduleName);

      var instantiatedModules =
          instantiations.stream().map(inst -> inst.specificationName).collect(Collectors.toSet());
      checkState(
          !instantiatedModules.contains(moduleName),
          "Module " + moduleName + " must not instantiate itself.");

      var variableNameOccurences =
          variables.stream()
              .collect(Collectors.groupingBy(variable -> variable.name, Collectors.counting()));
      var duplicateVariableNames =
          variableNameOccurences.entrySet().stream()
              .filter(entry -> entry.getValue() > 1)
              .map(entry -> entry.getKey())
              .collect(Collectors.toSet());
      checkState(
          duplicateVariableNames.isEmpty(),
          "The following variable names of module "
              + moduleName
              + " are not unique: "
              + String.join(", ", duplicateVariableNames));

      var instNameOccurences =
          instantiations.stream()
              .collect(Collectors.groupingBy(inst -> inst.instanceName, Collectors.counting()));
      var duplicateInstNames =
          instNameOccurences.entrySet().stream()
              .filter(entry -> entry.getValue() > 1)
              .map(entry -> entry.getKey())
              .collect(Collectors.toSet());
      checkState(
          duplicateInstNames.isEmpty(),
          "The following instance names defined in module "
              + moduleName
              + " are not unique: "
              + String.join(", ", duplicateInstNames));

      return new ModuleSpecification(isRoot, variables, instantiations, automaton, moduleName);
    }
  }
}
