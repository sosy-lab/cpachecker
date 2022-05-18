// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.measures;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.util.coverage.data.CoverageStatistics;
import org.sosy_lab.cpachecker.util.coverage.data.FileCoverageStatistics;

/**
 * Handler for managing all coverage measures. This class (including its corresponding enums) needs
 * to be expanded if new coverage measures should be tracked and visualized. They first need to be
 * initialized with initNewData and second, a switch case within the fillCoverageData needs to be
 * added. Last step is important to feed this measure with data at the point when the analysis is
 * done. The corresponding data is delivered by CoverageData which holds all relevant coverage data.
 */
public class CoverageMeasureHandler {
  private final Map<CoverageMeasureType, CoverageMeasure> coverageMeasureMap;
  private final List<CoverageMeasureType> coverageMeasureTypes;

  public CoverageMeasureHandler() {
    coverageMeasureMap = new LinkedHashMap<>();
    coverageMeasureTypes = new ArrayList<>();
  }

  /** Init all measures which are analysis independent */
  public void addAllAnalysisIndependentMeasuresTypes() {
    coverageMeasureTypes.add(CoverageMeasureType.VisitedLocations);
    coverageMeasureTypes.add(CoverageMeasureType.ReachedLocations);
    coverageMeasureTypes.add(CoverageMeasureType.ConsideredLocationsHeatMap);
    coverageMeasureTypes.add(CoverageMeasureType.ConsideredLinesHeatMap);
  }

  /** Init all measures which depend on predicate analysis */
  public void addAllPredicateAnalysisMeasuresTypes() {
    coverageMeasureTypes.add(CoverageMeasureType.PredicateConsidered);
    coverageMeasureTypes.add(CoverageMeasureType.PredicateRelevantVariables);
    coverageMeasureTypes.add(CoverageMeasureType.PredicateAbstractionVariables);
  }

  /**
   * This method is called in the end of the analysis. It is used to populate the coverage data for
   * all initialized measures. When adding a new coverage measure this method should be expanded by
   * its type.
   *
   * @param infosPerFile infosPerFile holds all relevant coverage data gathered during the analysis
   */
  public void fillCoverageData(Map<String, FileCoverageStatistics> infosPerFile) {
    CoverageStatistics covStatistics = new CoverageStatistics(infosPerFile);
    for (CoverageMeasureType type : getAllTypes()) {
      switch (type) {
        case VisitedLocations:
          addData(
              type,
              new LocationCoverageMeasure(
                  covStatistics.visitedLocations, covStatistics.numTotalNodes));
          break;
        case ReachedLocations:
          addData(
              type,
              new LocationCoverageMeasure(
                  covStatistics.reachedLocations, covStatistics.numTotalNodes));
          break;
        case ConsideredLocationsHeatMap:
          addData(
              type,
              new MultiLocationCoverageMeasure(
                  covStatistics.visitedLocations,
                  covStatistics.reachedLocations,
                  covStatistics.numTotalNodes));
          break;
        case ConsideredLinesHeatMap:
          addData(type, new LineCoverageMeasure(infosPerFile));
          break;
        case PredicateConsidered:
          addData(
              type,
              new LocationCoverageMeasure(
                  covStatistics.predicateConsideredNodes, covStatistics.numTotalNodes));
          break;
        case PredicateRelevantVariables:
          addData(
              type,
              new LocationCoverageMeasure(
                  covStatistics.predicateRelevantVariablesConsideredNodes,
                  covStatistics.numTotalNodes));
          break;
        case PredicateAbstractionVariables:
          addData(
              type,
              new VariableCoverageMeasure(
                  covStatistics.allVariableNames, covStatistics.relevantVariableNames));
      }
    }
  }

  public CoverageMeasure getData(CoverageMeasureType type) {
    return coverageMeasureMap.get(type);
  }

  public ImmutableList<CoverageMeasureType> getAllTypes() {
    return ImmutableList.copyOf(coverageMeasureTypes);
  }

  public ImmutableList<String> getAllTypesForCategoriesAsString(
      CoverageMeasureCategory... categories) {
    return coverageMeasureTypes.stream()
        .filter(v -> isContainedIn(categories, v.getCategory()))
        .map(v -> v.getName())
        .collect(ImmutableList.toImmutableList());
  }

  public ImmutableList<CoverageMeasureType> getAllTypesForCategories(
      CoverageMeasureCategory... categories) {
    return coverageMeasureTypes.stream()
        .filter(v -> isContainedIn(categories, v.getCategory()))
        .collect(ImmutableList.toImmutableList());
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

  private void addData(CoverageMeasureType type, CoverageMeasure data) {
    coverageMeasureMap.put(type, data);
  }
}
