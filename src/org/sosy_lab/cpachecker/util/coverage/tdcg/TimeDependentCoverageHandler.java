// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.tdcg;

import java.util.HashMap;
import java.util.Map;

public class TimeDependentCoverageHandler {

  private final Map<TimeDependentCoverageType, TimeDependentCoverageData>
      timeDependentCoverageDataMap;

  public TimeDependentCoverageHandler() {
    timeDependentCoverageDataMap = new HashMap<>();
  }

  public void initNewData(TimeDependentCoverageType type) {
    timeDependentCoverageDataMap.put(type, new TimeDependentCoverageData());
  }

  public void addData(TimeDependentCoverageType type, TimeDependentCoverageData data) {
    timeDependentCoverageDataMap.put(type, data);
  }

  public TimeDependentCoverageData getData(TimeDependentCoverageType type) {
    return timeDependentCoverageDataMap.get(type);
  }
}
