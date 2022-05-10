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
  /* ##### Class Fields ##### */
  private Map<Long, Double> timeStampsPerCoverage;
  private Map<Long, Double> previousTimeStampsPerCoverage;
  private Instant startTime = Instant.MIN;

  /* ##### Constructors ##### */
  public TimeDependentCoverageData() {
    initTimeStampsPerCoverage();
    previousTimeStampsPerCoverage = new LinkedHashMap<>();
  }

  /* ##### Public Methods ##### */
  public void addTimeStamp(double coverage) {
    initStartTime();
    timeStampsPerCoverage.put(getDurationInMicros(), coverage);
  }

  public void resetTimeStamps() {
    startTime = Instant.now();
    previousTimeStampsPerCoverage = timeStampsPerCoverage;
    initTimeStampsPerCoverage();
  }

  /* ##### Getter and Setter ##### */
  public Map<Long, Double> getReducedTimeStampsPerCoverage(int max) {
    return thinOutMap(getTimeStampsPerCoverage(), max);
  }

  public Map<Long, Double> getTimeStampsPerCoverage() {
    if (!previousTimeStampsPerCoverage.isEmpty()) {
      double maxPreviousCoverage =
          previousTimeStampsPerCoverage.values().stream().reduce(Double::max).orElseThrow();
      double maxCoverage =
          timeStampsPerCoverage.values().stream().reduce(Double::max).orElseThrow();
      if (maxPreviousCoverage > maxCoverage) {
        return previousTimeStampsPerCoverage;
      }
    }
    return timeStampsPerCoverage;
  }

  /* ##### Private Methods ##### */
  private void initTimeStampsPerCoverage() {
    timeStampsPerCoverage = new LinkedHashMap<>();
    timeStampsPerCoverage.put(0L, 0.0);
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
