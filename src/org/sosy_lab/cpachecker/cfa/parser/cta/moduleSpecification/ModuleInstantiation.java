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

import java.util.HashMap;
import java.util.Map;

public class ModuleInstantiation {
  public String specificationName;
  public String instanceName;
  public Map<String, String> variableMappings;

  private ModuleInstantiation(
      String pSpecificationName, String pInstanceName, Map<String, String> pVariableMappings) {
    specificationName = pSpecificationName;
    instanceName = pInstanceName;
    variableMappings = pVariableMappings;
  }

  public static ModuleInstantiation getDummyInstantiationForModule(ModuleSpecification module) {
    return new Builder()
        .instanceName(module.moduleName)
        .specificationName(module.moduleName)
        .build();
  }

  public static class Builder {
    private String specificationName;
    private String instanceName;
    private Map<String, String> variableMappings = new HashMap<>();

    public Builder specificationName(String pSpecificationName) {
      specificationName = checkNotNull(pSpecificationName);
      checkArgument(
          !specificationName.isEmpty(), "Empty module specification names are not allowed");
      return this;
    }
    
    public Builder instanceName(String pInstanceName) {
      instanceName = checkNotNull(pInstanceName);
      checkArgument(!instanceName.isEmpty(), "Empty module instance names are not allowed");

      return this;
    }

    public Builder variableMapping(String vSpecName, String vInstanceName) {
      checkNotNull(vSpecName);
      checkNotNull(vInstanceName);
      checkArgument(!vInstanceName.isEmpty(), "Empty variable instance names are not allowed");
      checkArgument(!vSpecName.isEmpty(), "Empty variable names are not allowed");

      variableMappings.put(vSpecName, vInstanceName);
      return this;
    }

    public ModuleInstantiation build() {
      checkNotNull(specificationName);
      checkNotNull(instanceName);
      checkNotNull(variableMappings);

      return new ModuleInstantiation(specificationName, instanceName, variableMappings);
    }
  }
}
