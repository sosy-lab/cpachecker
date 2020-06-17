// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification;

import java.util.HashSet;
import java.util.Set;

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
      modules = pModules;
      return this;
    }

    public Builder instantiation(ModuleInstantiation pInstantiation) {
      instantiation = pInstantiation;
      return this;
    }

    public SystemSpecification build() {
      return new SystemSpecification(modules, instantiation);
    }
  }
}
