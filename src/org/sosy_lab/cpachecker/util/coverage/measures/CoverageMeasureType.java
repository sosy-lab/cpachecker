// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.measures;

import org.sosy_lab.cpachecker.util.coverage.collectors.CoverageCollectorHandler;

/**
 * Coverage Measure Type is used to distinguish between different coverage measure calculation
 * approaches. Approaches are based typically on a CoverageMeasureCategory. In addition, it is
 * possible to specify a friendly name for each category which is displayed in the report.html
 */
public enum CoverageMeasureType {
  VISITED_LOCATIONS("Visited Locations", CoverageMeasureCategory.LOCATION_BASED),
  REACHED_LOCATIONS("Reached Locations", CoverageMeasureCategory.LOCATION_BASED),
  CONSIDERED_LOCATIONS_HEAT_MAP(
      "Considered-Locations Heat Map", CoverageMeasureCategory.LOCATION_BASED),
  VISITED_LINES_HEAT_MAP("Visited-Lines Heat Map", CoverageMeasureCategory.LINE_BASED),
  PREDICATE_CONSIDERED("Predicate-Considered-Locations", CoverageMeasureCategory.LOCATION_BASED),
  PREDICATE_RELEVANT_VARIABLES(
      "Predicate-Relevant-Variables", CoverageMeasureCategory.LOCATION_BASED),
  PREDICATE_ABSTRACTION_VARIABLES(
      "Predicate-Abstraction-Variables", CoverageMeasureCategory.VARIABLE_BASED);

  private final String name;
  private final CoverageMeasureCategory category;

  CoverageMeasureType(String pName, CoverageMeasureCategory pCategeory) {
    name = pName;
    category = pCategeory;
  }

  public CoverageMeasure getCoverageMeasure(CoverageCollectorHandler covHandler) {
    switch (this) {
      case VISITED_LOCATIONS:
        return new LocationBasedCoverageMeasure(
            covHandler.getAnalysisIndependentCollector().getVisitedLocations(),
            covHandler.getAnalysisIndependentCollector().getTotalLocationCount());
      case REACHED_LOCATIONS:
        return new LocationBasedCoverageMeasure(
            covHandler.getReachedSetCoverageCollector().getReachedLocations(),
            covHandler.getReachedSetCoverageCollector().getTotalLocationCount());
      case CONSIDERED_LOCATIONS_HEAT_MAP:
        return new MultiLocationBasedCoverageMeasure(
            covHandler.getAnalysisIndependentCollector().getVisitedLocations(),
            covHandler.getReachedSetCoverageCollector().getReachedLocations(),
            covHandler.getAnalysisIndependentCollector().getTotalLocationCount());
      case VISITED_LINES_HEAT_MAP:
        return new LineBasedCoverageMeasure(
            covHandler.getAnalysisIndependentCollector().getVisitedLinesPerFile(),
            covHandler.getAnalysisIndependentCollector().getExistingLinesPerFile());
      case PREDICATE_CONSIDERED:
        return new LocationBasedCoverageMeasure(
            covHandler.getPredicateAnalysisCollector().getPredicateConsideredLocations(),
            covHandler.getPredicateAnalysisCollector().getTotalLocationCount());
      case PREDICATE_RELEVANT_VARIABLES:
        return new LocationBasedCoverageMeasure(
            covHandler.getPredicateAnalysisCollector().getPredicateRelevantConsideredLocations(),
            covHandler.getPredicateAnalysisCollector().getTotalLocationCount());
      case PREDICATE_ABSTRACTION_VARIABLES:
        return new VariableBasedCoverageMeasure(
            covHandler.getPredicateAnalysisCollector().getAllVariables(),
            covHandler.getPredicateAnalysisCollector().getVisitedVariables());
      default:
        throw new AssertionError("Unknown CoverageMeasureType used: " + this.name);
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
