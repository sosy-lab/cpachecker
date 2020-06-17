// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification;

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
      specificationName = pSpecificationName;
      return this;
    }

    public Builder instanceName(String pInstanceName) {
      instanceName = pInstanceName;
      return this;
    }

    public Builder variableMapping(String vSpecName, String vInstanceName) {
      variableMappings.put(vSpecName, vInstanceName);
      return this;
    }

    public ModuleInstantiation build() {
      return new ModuleInstantiation(specificationName, instanceName, variableMappings);
    }
  }
}
