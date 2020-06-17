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
      checkNotNull(instantiation);
      var specificationNames = modules.stream().map(moduleSpec -> moduleSpec.moduleName);
      var instantiations = modules.stream().flatMap(module -> module.instantiations.stream());
      var instantiatedModules = instantiations.map(inst -> inst.specificationName);
      var instanceNames = instantiations.map(inst -> inst.instanceName);

      var specNameOccurences =
          specificationNames.collect(
              Collectors.groupingBy(moduleSpec -> moduleSpec, Collectors.counting()));
      var duplicateSpecNames =
          specNameOccurences.entrySet().stream()
              .filter(entry -> entry.getValue() > 1)
              .map(entry -> entry.getKey())
              .collect(Collectors.toSet());
      checkState(
          duplicateSpecNames.isEmpty(),
          "Module specification names must be uniques. Found duplicate names: "
              + String.join(", ", duplicateSpecNames));

      var unknownInstantiations =
          Sets.difference(
              instantiatedModules.collect(Collectors.toSet()),
              specificationNames.collect(Collectors.toSet()));
      checkState(
          unknownInstantiations.isEmpty(),
          "Instantiation of module(s) "
              + String.join(", ", unknownInstantiations)
              + " invalid. No matching specification found.");

      var uninstantiatedSpecifications =
          Sets.difference(
              specificationNames.collect(Collectors.toSet()),
              instantiatedModules.collect(Collectors.toSet()));
      checkState(
          uninstantiatedSpecifications.isEmpty(),
          "The following modules are specified but never instantiated: "
              + String.join(", ", uninstantiatedSpecifications));

      var instNameOccurences =
          instanceNames.collect(
              Collectors.groupingBy(instanceName -> instanceName, Collectors.counting()));
      var duplicateInstanceNames =
          instNameOccurences.entrySet().stream()
              .filter(entry -> entry.getValue() > 1)
              .map(entry -> entry.getKey())
              .collect(Collectors.toSet());
      checkState(
          duplicateInstanceNames.isEmpty(),
          "Module instance names must be uniques. Found duplicate names: "
              + String.join(", ", duplicateInstanceNames));

      return new SystemSpecification(modules, instantiation);
    }
  }
}
