// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.measures;

public enum CoverageMeasureType {
  None("None", CoverageMeasureCategory.None),
  ConsideredLinesHeatMap("Considered-Lines Heat Map", CoverageMeasureCategory.LineBased),
  PredicateConsidered("Predicate-Considered", CoverageMeasureCategory.LocationBased),
  PredicateRelevantVariables("Predicate-Relevant-Variables", CoverageMeasureCategory.LocationBased);

  private final String name;
  private final CoverageMeasureCategory category;

  CoverageMeasureType(String pName, CoverageMeasureCategory pCategeory) {
    name = pName;
    category = pCategeory;
  }

  public String getName() {
    return name;
  }

  public CoverageMeasureCategory getCategory() {
    return category;
  }

  public String getId() {
    return getName().replaceAll("-|\\s+", "");
  }
}
