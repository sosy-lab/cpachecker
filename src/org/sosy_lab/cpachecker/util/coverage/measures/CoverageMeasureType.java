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
 * approaches. Approaches are based typically on a CoverageMeasureInputCategory. In addition, it is
 * possible to specify a friendly name for each category which is displayed in the report.html
 * CoverageMeasureType is about the type of coverage, whereas CoverageMeasure is about actually
 * holding the coverage data and has methods accessing typical coverage values. The
 * CoverageMeasureHandler builds a connection between each CoverageMeasure and its corresponding
 * type.
 */
public enum CoverageMeasureType {
  VISITED_LOCATIONS(
      "Visited Locations",
      CoverageMeasureInputCategory.LOCATION_BASED,
      CoverageMeasureAnalysisCategory.ANALYSIS_INDEPENDENT),
  REACHED_LOCATIONS(
      "Reached Locations",
      CoverageMeasureInputCategory.LOCATION_BASED,
      CoverageMeasureAnalysisCategory.ANALYSIS_INDEPENDENT),
  CONSIDERED_LOCATIONS_HEAT_MAP(
      "Considered-Locations Heat Map",
      CoverageMeasureInputCategory.LOCATION_BASED,
      CoverageMeasureAnalysisCategory.ANALYSIS_INDEPENDENT),
  VISITED_LINES_HEAT_MAP(
      "Visited-Lines Heat Map",
      CoverageMeasureInputCategory.LINE_BASED,
      CoverageMeasureAnalysisCategory.ANALYSIS_INDEPENDENT),
  PREDICATE_CONSIDERED(
      "Predicate-Considered Locations",
      CoverageMeasureInputCategory.LOCATION_BASED,
      CoverageMeasureAnalysisCategory.PREDICATE_ANALYSIS),
  PREDICATE_RELEVANT_VARIABLES(
      "Predicate-Relevant-Variables Locations",
      CoverageMeasureInputCategory.LOCATION_BASED,
      CoverageMeasureAnalysisCategory.PREDICATE_ANALYSIS),
  PREDICATE_ABSTRACTION_VARIABLES(
      "Predicate-Abstraction Variables",
      CoverageMeasureInputCategory.VARIABLE_BASED,
      CoverageMeasureAnalysisCategory.PREDICATE_ANALYSIS);

  private final String name;
  private final CoverageMeasureInputCategory category;
  private final CoverageMeasureAnalysisCategory analysisCategory;

  CoverageMeasureType(
      String pName,
      CoverageMeasureInputCategory pCategeory,
      CoverageMeasureAnalysisCategory pAnalysisCategory) {
    name = pName;
    category = pCategeory;
    analysisCategory = pAnalysisCategory;
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

  public CoverageMeasureInputCategory getCategory() {
    return category;
  }

  public CoverageMeasureAnalysisCategory getAnalysisCategory() {
    return analysisCategory;
  }

  public String getId() {
    return getName().replaceAll("-|\\s+", "");
  }
}
