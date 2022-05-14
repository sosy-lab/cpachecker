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

/**
 * A coverage measure which is based on variables defined within the source code. The coverage
 * criteria is applied on all given variables. Data gathering is typically done after the analysis
 * within the CoverageCollector or during the analysis within a CoverageCPA.
 */
public class VariableCoverageMeasure implements CoverageMeasure {
  /* ##### Class Fields ##### */
  private final Multiset<String> allVariables;
  private final Multiset<String> relevantVariables;

  /* ##### Constructors ##### */
  public VariableCoverageMeasure(
      Multiset<String> pAllVariables, Multiset<String> pRelevantVariables) {
    allVariables = pAllVariables;
    relevantVariables = pRelevantVariables;
  }

  public VariableCoverageMeasure() {
    this(HashMultiset.create(), HashMultiset.create());
  }

  /* ##### Getter Methods ##### */
  public String getAllVariablesAsString() {
    StringBuilder variablesBuilder = new StringBuilder();
    int i = 0;
    int max = relevantVariables.elementSet().size() - 1;
    for (String variableStr : relevantVariables.elementSet()) {
      variablesBuilder.append(variableStr);
      if (i++ != max) {
        variablesBuilder.append(",");
      }
    }
    return variablesBuilder.toString();
  }

  /* ##### Inherited Methods ##### */
  @Override
  public double getCoverage() {
    return getCount() / getMaxCount();
  }

  @Override
  public double getCount() {
    return relevantVariables.elementSet().size();
  }

  @Override
  public double getMaxCount() {
    return allVariables.elementSet().size();
  }
}
