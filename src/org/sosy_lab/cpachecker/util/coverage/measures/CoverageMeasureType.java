// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.measures;

/**
 * Coverage Measure Type is used to distinguish between different coverage measure calculation
 * approaches. Approaches are based typically on a CoverageMeasureCategory. In addition, it is
 * possible to specify a friendly name for each category which is displayed in the report.html
 */
public enum CoverageMeasureType {
  /* ##### Enum Fields ##### */
  None("None", CoverageMeasureCategory.None),
  VisitedLocations("Visited Locations", CoverageMeasureCategory.LocationBased),
  ReachedLocations("Reached Locations", CoverageMeasureCategory.LocationBased),
  ConsideredLocationsHeatMap(
      "Considered-Locations Heat Map", CoverageMeasureCategory.LocationBased),
  ConsideredLinesHeatMap("Considered-Lines Heat Map", CoverageMeasureCategory.LineBased),
  PredicateConsidered("Predicate-Considered", CoverageMeasureCategory.LocationBased),
  PredicateRelevantVariables("Predicate-Relevant-Variables", CoverageMeasureCategory.LocationBased),
  PredicateAbstractionVariables(
      "Predicate-Abstraction-Variables", CoverageMeasureCategory.VariableBased);

  private final String name;
  private final CoverageMeasureCategory category;

  /* ##### Constructors ##### */
  CoverageMeasureType(String pName, CoverageMeasureCategory pCategeory) {
    name = pName;
    category = pCategeory;
  }

  /* ##### Getter Methods ##### */
  public String getName() {
    return name;
  }

  public String getCoverageName() {
    return name + " Coverage";
  }

  public CoverageMeasureCategory getCategory() {
    return category;
  }

  public String getId() {
    return getName().replaceAll("-|\\s+", "");
  }
}
