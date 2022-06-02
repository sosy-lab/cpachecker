// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.measures;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

/**
 * A coverage measure which is based on variables defined within the source code. The coverage
 * criteria is applied on all given variables. Data gathering is typically done after the analysis
 * within the CoverageCollector or during the analysis within a CoverageCPA.
 */
public class VariableBasedCoverageMeasure implements CoverageMeasure {
  private final ImmutableSet<String> visitedVariables;
  private final ImmutableSet<String> allVariables;

  public VariableBasedCoverageMeasure(
      ImmutableSet<String> pAllVariables, Set<String> pVisitedVariables) {
    visitedVariables = ImmutableSet.copyOf(pVisitedVariables);
    allVariables = pAllVariables;
  }

  public String getAllRelevantVariablesAsString() {
    StringBuilder variablesBuilder = new StringBuilder();
    int i = 0;
    int max = visitedVariables.size() - 1;
    for (String variableStr : visitedVariables) {
      variablesBuilder.append(variableStr);
      if (i++ != max) {
        variablesBuilder.append(",");
      }
    }
    return variablesBuilder.toString();
  }

  @Override
  public double getNormalizedValue() {
    return getValue() / getMaxValue();
  }

  @Override
  public double getValue() {
    return visitedVariables.size();
  }

  @Override
  public double getMaxValue() {
    return allVariables.size();
  }
}
