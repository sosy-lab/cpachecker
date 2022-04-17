// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.tdcg;

import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TimeDependentCoverageData {

  private final Map<Long, Double> timeStampsPerCoverage;
  private Instant startTime = Instant.MIN;

  public TimeDependentCoverageData() {
    timeStampsPerCoverage = new LinkedHashMap<>();
    timeStampsPerCoverage.put(0L, 0.0);
  }

  public Map<Long, Double> getReducedTimeStampsPerCoverage(int max) {
    return thinOutMap(getTimeStampsPerCoverage(), max);
  }

  public Map<Long, Double> getTimeStampsPerCoverage() {
    return timeStampsPerCoverage;
  }

  public ImmutableList<Double> getCoverageList() {
    return timeStampsPerCoverage.values().stream()
        .sorted()
        .collect(ImmutableList.toImmutableList());
  }

  public void addTimeStamp(double coverage) {
    initStartTime();
    timeStampsPerCoverage.put(getDurationInMicros(), coverage);
  }

  private long getDurationInMicros() {
    long durationInNanos = Duration.between(startTime, Instant.now()).toNanos();
    return TimeUnit.NANOSECONDS.toMicros(durationInNanos);
  }

  private void initStartTime() {
    if (startTime.equals(Instant.MIN)) {
      startTime = Instant.now();
    }
  }

  private Map<Long, Double> thinOutMap(Map<Long, Double> map, int max) {
    if (max < 0) {
      max = 0;
    }
    int mapSize = map.size();
    if (mapSize < max) {
      return map;
    }
    Map<Long, Double> outputMap = new HashMap<>();
    List<Long> list = map.keySet().stream().sorted().collect(ImmutableList.toImmutableList());
    int ruleOutQuotient = (int) Math.ceil(mapSize / (double) max);
    for (int i = 0; i < mapSize; i++) {
      if (i % ruleOutQuotient == 0) {
        long key = list.get(i);
        outputMap.put(key, map.get(key));
      }
    }
    return outputMap;
  }
}
