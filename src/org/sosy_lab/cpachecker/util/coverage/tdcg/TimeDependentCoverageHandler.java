// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.tdcg;

import com.google.common.collect.ImmutableList;
import java.util.LinkedHashMap;
import java.util.Map;

public class TimeDependentCoverageHandler {

  private Map<TimeDependentCoverageType, TimeDependentCoverageData> timeDependentCoverageDataMap;

  public TimeDependentCoverageHandler() {
    timeDependentCoverageDataMap = new LinkedHashMap<>();
  }

  public void initNewData(TimeDependentCoverageType type) {
    timeDependentCoverageDataMap.put(type, new TimeDependentCoverageData());
  }

  public void initAnalysisIndependentTDCG() {
    initNewData(TimeDependentCoverageType.VisitedLines);
  }

  public void initPredicateAnalysisTDCG() {
    initNewData(TimeDependentCoverageType.PredicatesGenerated);
    initNewData(TimeDependentCoverageType.PredicateConsideredLocations);
    initNewData(TimeDependentCoverageType.PredicateRelevantVariables);
    initNewData(TimeDependentCoverageType.AbstractStateCoveredNodes);
  }

  public ImmutableList<TimeDependentCoverageType> getAllTypes() {
    return ImmutableList.copyOf(timeDependentCoverageDataMap.keySet());
  }

  public void addData(TimeDependentCoverageType type, TimeDependentCoverageData data) {
    timeDependentCoverageDataMap.put(type, data);
  }

  public TimeDependentCoverageData getData(TimeDependentCoverageType type) {
    return timeDependentCoverageDataMap.get(type);
  }

  public Map<TimeDependentCoverageType, TimeDependentCoverageData>
      getTimeDependentCoverageDataMap() {
    return timeDependentCoverageDataMap;
  }

  public void mergeData(TimeDependentCoverageHandler secondHandler) {
    for (var type : getAllTypes()) {
      secondHandler.addData(type, getData(type));
    }
    timeDependentCoverageDataMap = secondHandler.getTimeDependentCoverageDataMap();
  }
}
