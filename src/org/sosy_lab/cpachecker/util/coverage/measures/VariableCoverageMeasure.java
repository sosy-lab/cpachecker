// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.measures;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import java.util.Set;

/**
 * A coverage measure which is based on variables defined within the source code. The coverage
 * criteria is applied on all given variables. Data gathering is typically done after the analysis
 * within the CoverageCollector or during the analysis within a CoverageCPA.
 */
public class VariableCoverageMeasure implements CoverageMeasure {
  /* ##### Class Fields ##### */
  private final Multiset<String> variables;

  /* ##### Constructors ##### */
  public VariableCoverageMeasure(Multiset<String> pVariables) {
    variables = pVariables;
  }

  public VariableCoverageMeasure() {
    variables = HashMultiset.create();
  }

  /* ##### Getter Methods ##### */
  public String getVariables() {
    StringBuilder variablesBuilder = new StringBuilder();
    int i = 0;
    int max = getRelevantVariables().size() - 1;
    for (String variableStr : getRelevantVariables()) {
      variablesBuilder.append(variableStr);
      if (i++ != max) {
        variablesBuilder.append(",");
      }
    }
    return variablesBuilder.toString();
  }

  private Set<String> getRelevantVariables() {
    return variables.elementSet();
  }

  /* ##### Inherited Methods ##### */
  @Override
  public double getCoverage() {
    return 0;
  }

  @Override
  public double getCount() {
    return 0;
  }

  @Override
  public double getMaxCount() {
    return 0;
  }
}
