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
  /* ##### Class Fields ##### */
  private final Map<CoverageMeasureType, CoverageMeasure> coverageMeasureMap;

  /* ##### Constructors ##### */
  public CoverageMeasureHandler() {
    coverageMeasureMap = new LinkedHashMap<>();
    initNewData(CoverageMeasureType.None);
  }

  /* ##### Public Methods ##### */
  /**
   * Method for initializing a generic type of coverage measure When adding new
   * CoverageMeasureCategory fields, this method should include those cases.
   *
   * @param type type of the coverage measure
   */
  public void initNewData(CoverageMeasureType type) {
    CoverageMeasure coverageMeasure = null;
    switch (type.getCategory()) {
      case None:
        coverageMeasure = new UndefinedCoverageMeasure();
        break;
      case LineBased:
        coverageMeasure = new LineCoverageMeasure();
        break;
      case LocationBased:
        coverageMeasure = new LocationCoverageMeasure();
        break;
    }
    coverageMeasureMap.put(type, coverageMeasure);
  }

  /** Init all measures which are analysis independent */
  public void initAnalysisIndependentMeasures() {
    initNewData(CoverageMeasureType.VisitedLocations);
    initNewData(CoverageMeasureType.ReachedLocations);
    initNewData(CoverageMeasureType.ConsideredLocationsHeatMap);
    initNewData(CoverageMeasureType.ConsideredLinesHeatMap);
  }

  /** Init all measures which depend on predicate analysis */
  public void initPredicateAnalysisMeasures() {
    initNewData(CoverageMeasureType.PredicateConsidered);
    initNewData(CoverageMeasureType.PredicateRelevantVariables);
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
    for (var type : getAllTypes()) {
      LocationCoverageMeasure locCov;
      LineCoverageMeasure lineCov;
      switch (type) {
        case None:
          break;
        case VisitedLocations:
          locCov =
              new LocationCoverageMeasure(
                  covStatistics.visitedLocations, covStatistics.numTotalNodes);
          addData(type, locCov);
          break;
        case ReachedLocations:
          locCov =
              new LocationCoverageMeasure(
                  covStatistics.reachedLocations, covStatistics.numTotalNodes);
          addData(type, locCov);
          break;
        case ConsideredLocationsHeatMap:
          locCov =
              new MultiLocationCoverageMeasure(
                  covStatistics.visitedLocations,
                  covStatistics.reachedLocations,
                  covStatistics.numTotalNodes);
          addData(type, locCov);
          break;
        case ConsideredLinesHeatMap:
          lineCov = new LineCoverageMeasure(infosPerFile);
          addData(type, lineCov);
          break;
        case PredicateConsidered:
          locCov =
              new LocationCoverageMeasure(
                  covStatistics.predicateConsideredNodes, covStatistics.numTotalNodes);
          addData(type, locCov);
          break;
        case PredicateRelevantVariables:
          locCov =
              new LocationCoverageMeasure(
                  covStatistics.predicateRelevantVariablesConsideredNodes,
                  covStatistics.numTotalNodes);
          addData(type, locCov);
          break;
      }
    }
  }

  /* ##### Getter and Setter ##### */
  public void addData(CoverageMeasureType type, CoverageMeasure data) {
    coverageMeasureMap.put(type, data);
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

  /* ##### Helper Methods ##### */
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
