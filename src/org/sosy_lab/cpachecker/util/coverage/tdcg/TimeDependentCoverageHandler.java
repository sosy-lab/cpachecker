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

/**
 * Time-dependent Coverage Handler holds all Time-dependent coverage data which are in use. New TDCG
 * get initialized here, they consist of a Time-dependent coverage data with a corresponding
 * Time-dependent coverage type. This handler holds all these tuples within a Map. All initialized
 * TDCGs are available within CoverageCPAs and are shown at the end of the analysis within the TDCG
 * Tab in the report.html.
 */
public class TimeDependentCoverageHandler {
  private final Map<TimeDependentCoverageType, TimeDependentCoverageData>
      timeDependentCoverageDataMap;

  public TimeDependentCoverageHandler() {
    timeDependentCoverageDataMap = new LinkedHashMap<>();
  }

  public TimeDependentCoverageData getData(TimeDependentCoverageType type) {
    TimeDependentCoverageData tdgData = timeDependentCoverageDataMap.get(type);
    if (tdgData == null) {
      TimeDependentCoverageData newTDGData = new TimeDependentCoverageData();
      timeDependentCoverageDataMap.put(type, newTDGData);
      return newTDGData;
    }
    return tdgData;
  }

  public ImmutableList<TimeDependentCoverageType> getAllTypes() {
    return ImmutableList.copyOf(timeDependentCoverageDataMap.keySet());
  }
}
