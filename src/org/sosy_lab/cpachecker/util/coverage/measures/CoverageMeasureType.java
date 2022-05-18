// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.measures;

import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.util.coverage.data.CoverageStatistics;
import org.sosy_lab.cpachecker.util.coverage.data.FileCoverageStatistics;

/**
 * Coverage Measure Type is used to distinguish between different coverage measure calculation
 * approaches. Approaches are based typically on a CoverageMeasureCategory. In addition, it is
 * possible to specify a friendly name for each category which is displayed in the report.html
 */
public enum CoverageMeasureType {
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

  CoverageMeasureType(String pName, CoverageMeasureCategory pCategeory) {
    name = pName;
    category = pCategeory;
  }

  public Optional<CoverageMeasure> getCoverageMeasure(Map<String, FileCoverageStatistics> infosPerFile) {
    CoverageStatistics covStatistics = new CoverageStatistics(infosPerFile);
    switch (this) {
      case VisitedLocations:
            return Optional.of(new LocationCoverageMeasure(
                covStatistics.visitedLocations, covStatistics.numTotalNodes));
      case ReachedLocations:
        return Optional.of(new LocationCoverageMeasure(
                covStatistics.reachedLocations, covStatistics.numTotalNodes));
      case ConsideredLocationsHeatMap:
        return Optional.of(new MultiLocationCoverageMeasure(
                covStatistics.visitedLocations,
                covStatistics.reachedLocations,
                covStatistics.numTotalNodes));
      case ConsideredLinesHeatMap:
        return Optional.of(new LineCoverageMeasure(infosPerFile));
      case PredicateConsidered:
        return Optional.of(new LocationCoverageMeasure(
                covStatistics.predicateConsideredNodes, covStatistics.numTotalNodes));
      case PredicateRelevantVariables:
        return Optional.of(new LocationCoverageMeasure(
                covStatistics.predicateRelevantVariablesConsideredNodes,
                covStatistics.numTotalNodes));
      case PredicateAbstractionVariables:
        return Optional.of(new VariableCoverageMeasure(
                covStatistics.allVariableNames, covStatistics.relevantVariableNames));
      default:
        return Optional.empty();
    }
  }

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
