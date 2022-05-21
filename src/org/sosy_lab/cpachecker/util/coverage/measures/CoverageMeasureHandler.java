// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.measures;

import com.google.common.collect.ImmutableList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handler for managing all coverage measures. This class (including its corresponding enums) needs
 * to be expanded if new coverage measures should be tracked and visualized. First, one needs to add
 * all types of CoverageMeasureType which should be tracked and Second, one needs to feed this
 * measure with data at the point when the analysis is done. The corresponding data is delivered by
 * CoverageStatistics which holds all relevant coverage data.
 */
public class CoverageMeasureHandler {
  private final Map<CoverageMeasureType, CoverageMeasure> coverageMeasureMap;

  public CoverageMeasureHandler() {
    coverageMeasureMap = new LinkedHashMap<>();
  }

  public CoverageMeasure getData(CoverageMeasureType type) {
    return coverageMeasureMap.get(type);
  }

  public ImmutableList<CoverageMeasureType> getAllTypes() {
    return ImmutableList.copyOf(coverageMeasureMap.keySet());
  }

  public ImmutableList<String> getAllTypesForCategoriesAsString(
      CoverageMeasureCategory... categories) {
    return coverageMeasureMap.keySet().stream()
        .filter(v -> isContainedIn(categories, v.getCategory()))
        .map(v -> v.getName())
        .collect(ImmutableList.toImmutableList());
  }

  public ImmutableList<CoverageMeasureType> getAllTypesForCategories(
      CoverageMeasureCategory... categories) {
    return coverageMeasureMap.keySet().stream()
        .filter(v -> isContainedIn(categories, v.getCategory()))
        .collect(ImmutableList.toImmutableList());
  }

  public void addData(CoverageMeasureType type, CoverageMeasure data) {
    coverageMeasureMap.put(type, data);
  }

  private boolean isContainedIn(
      CoverageMeasureCategory[] categories, CoverageMeasureCategory pCategory) {
    for (CoverageMeasureCategory category : categories) {
      if (category == pCategory) {
        return true;
      }
    }
    return false;
  }
}
