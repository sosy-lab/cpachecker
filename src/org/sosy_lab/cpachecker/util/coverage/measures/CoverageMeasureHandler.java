// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.measures;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ImmutableList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.util.coverage.CoverageData;
import org.sosy_lab.cpachecker.util.coverage.report.CoverageStatistics;

public class CoverageMeasureHandler {
  private Map<CoverageMeasureType, CoverageMeasure> coverageMeasureMap;

  public CoverageMeasureHandler() {
    coverageMeasureMap = new LinkedHashMap<>();
  }

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

  public void initAnalysisIndependentMeasures() {
    initNewData(CoverageMeasureType.None);
    initNewData(CoverageMeasureType.VisitedLocations);
    initNewData(CoverageMeasureType.ReachedLocations);
    initNewData(CoverageMeasureType.ConsideredLocationsHeatMap);
    initNewData(CoverageMeasureType.ConsideredLinesHeatMap);
  }

  public void initPredicateAnalysisMeasures() {
    initNewData(CoverageMeasureType.PredicateConsidered);
    initNewData(CoverageMeasureType.PredicateRelevantVariables);
  }

  public void addData(CoverageMeasureType type, CoverageMeasure data) {
    coverageMeasureMap.put(type, data);
  }

  public CoverageMeasure getData(CoverageMeasureType type) {
    return coverageMeasureMap.get(type);
  }

  public ImmutableList<CoverageMeasureType> getAllTypes() {
    return ImmutableList.copyOf(coverageMeasureMap.keySet());
  }

  public ImmutableList<String> getAllTypesAsString() {
    return transformedImmutableListCopy(coverageMeasureMap.keySet(), v -> v.getName());
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

  private boolean isContainedIn(
      CoverageMeasureCategory[] categories, CoverageMeasureCategory pCategory) {
    for (CoverageMeasureCategory category : categories) {
      if (category == pCategory) {
        return true;
      }
    }
    return false;
  }

  public void mergeData(CoverageMeasureHandler secondHandler) {
    for (var type : getAllTypes()) {
      secondHandler.addData(type, getData(type));
    }
    coverageMeasureMap = secondHandler.getCoverageMeasureMap();
  }

  public Map<CoverageMeasureType, CoverageMeasure> getCoverageMeasureMap() {
    return coverageMeasureMap;
  }

  public void fillCoverageData(CoverageData coverageData) {
    CoverageStatistics covStatistics = new CoverageStatistics(coverageData);
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
          lineCov =
              new LineCoverageMeasure(coverageData.getInfosPerFile(), covStatistics.numTotalLines);
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
}
