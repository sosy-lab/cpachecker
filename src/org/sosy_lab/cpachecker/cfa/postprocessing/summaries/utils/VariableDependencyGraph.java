// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.utils;

import java.util.HashSet;
import java.util.Set;

public class VariableDependencyGraph {

  private Set<VariableDependency> variableDependencies = new HashSet<>();

  public VariableDependencyGraph(VariableDependency pVariableDependency) {
    variableDependencies.add(pVariableDependency);
  }

  public VariableDependencyGraph(Set<VariableDependency> pVariableDependencies) {
    variableDependencies.addAll(pVariableDependencies);
  }
}
