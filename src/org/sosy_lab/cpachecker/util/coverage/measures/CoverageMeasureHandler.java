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
import org.sosy_lab.cpachecker.util.coverage.CoverageData;
import org.sosy_lab.cpachecker.util.coverage.report.CoverageStatistics;

public class CoverageMeasureHandler {
  private final Map<CoverageMeasureType, CoverageMeasure> coverageMeasureMap;

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

  public void initAllCoverageMeasures() {
    initNewData(CoverageMeasureType.None);
    initNewData(CoverageMeasureType.ConsideredLinesHeatMap);
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
    return coverageMeasureMap.keySet().stream().collect(ImmutableList.toImmutableList());
  }

  public ImmutableList<String> getAllTypesAsString() {
    return coverageMeasureMap.keySet().stream()
        .map(v -> v.getName())
        .collect(ImmutableList.toImmutableList());
  }

  public void fillCoverageData(CoverageData pCoverageData) {
    CoverageStatistics covStatistics = new CoverageStatistics(pCoverageData);
    for (var type : getAllTypes()) {
      LocationCoverageMeasure locCov;
      LineCoverageMeasure lineCov;
      switch (type) {
        case None:
          break;
        case ConsideredLinesHeatMap:
          lineCov =
              new LineCoverageMeasure(covStatistics.visitedLines, covStatistics.numTotalLines);
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
