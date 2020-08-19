// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SystemSpecification {
  public Set<ModuleSpecification> modules;
  public ModuleInstantiation instantiation;

  private SystemSpecification(
      Set<ModuleSpecification> pModules, ModuleInstantiation pInstantiation) {
    modules = pModules;
    instantiation = pInstantiation;
  }

  public static class Builder {
    private Set<ModuleSpecification> modules = new HashSet<>();
    private ModuleInstantiation instantiation;

    public Builder modules(Set<ModuleSpecification> pModules) {
      modules = checkNotNull(pModules);
      return this;
    }

    public Builder instantiation(ModuleInstantiation pInstantiation) {
      instantiation = checkNotNull(pInstantiation);
      return this;
    }

    public SystemSpecification build() {
      checkConsitency();
      return new SystemSpecification(modules, instantiation);
    }

    private void checkConsitency() {
      checkNotNull(instantiation);
      var rootModules =
          modules.stream()
              .filter(module -> module.isRoot)
              .map(module -> module.moduleName)
              .collect(Collectors.toSet());
      checkState(
          rootModules.size() == 1,
          "Invalid number of root modules. Expected 1 but got %s",
          rootModules.size());

      var instantiatedModules =
          modules.stream()
              .flatMap(module -> module.instantiations.stream())
              .map(inst -> inst.specificationName)
              .collect(Collectors.toSet());
      instantiatedModules.add(rootModules.iterator().next());
      var moduleNames =
          modules.stream().map(moduleSpec -> moduleSpec.moduleName).collect(Collectors.toSet());

      var specNameOccurences =
          modules.stream()
              .map(moduleSpec -> moduleSpec.moduleName)
              .collect(Collectors.groupingBy(moduleSpec -> moduleSpec, Collectors.counting()));
      var duplicateSpecNames =
          specNameOccurences.entrySet().stream()
              .filter(entry -> entry.getValue() > 1)
              .map(entry -> entry.getKey())
              .collect(Collectors.toSet());
      checkState(
          duplicateSpecNames.isEmpty(),
          "Module specification names must be uniques. Found duplicate name(s): %s",
          String.join(", ", duplicateSpecNames));

      var unknownInstantiations = Sets.difference(instantiatedModules, moduleNames);
      checkState(
          unknownInstantiations.isEmpty(),
          "Instantiation of module(s) %s is invalid. No matching specification found.",
          String.join(", ", unknownInstantiations));

      var uninstantiatedSpecifications = Sets.difference(moduleNames, instantiatedModules);
      checkState(
          uninstantiatedSpecifications.isEmpty(),
          "The following module(s) are specified but never instantiated: %s",
          String.join(", ", uninstantiatedSpecifications));
    }
  }
}
