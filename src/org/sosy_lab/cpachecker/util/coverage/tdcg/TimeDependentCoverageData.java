// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.tdcg;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.common.time.TimeSpan;

/**
 * Time-dependent Coverage Data is a data structure used by Time-dependent coverage graphs (TDCGs).
 * It holds a Map of (Long,Double) entries where Longs are interpreted as time and the Doubles as
 * raw coverage value at that corresponding time. Coverage values are calculated within a
 * CoverageCPA.
 */
public class TimeDependentCoverageData {
  private List<TimeDependentCoverageDataElement> coveragePerTimestamps;
  private List<TimeDependentCoverageDataElement> previousCoveragePerTimeStamps;
  private Instant startTime = Instant.MIN;

  public static class TimeDependentCoverageDataElement {
    private final TimeSpan time;
    private final Double value;

    private TimeDependentCoverageDataElement(TimeSpan pTime, Double pValue) {
      time = pTime;
      value = pValue;
    }

    private TimeDependentCoverageDataElement() {
      time = TimeSpan.ofNanos(0);
      value = 0.0;
    }

    public TimeSpan getTime() {
      return time;
    }

    public Double getValue() {
      return value;
    }
  }

  public TimeDependentCoverageData() {
    initCoveragePerTimestamps();
    previousCoveragePerTimeStamps = new ArrayList<>();
  }

  public void addTimestamp(double coverage) {
    initStartTime();
    coveragePerTimestamps.add(
        new TimeDependentCoverageDataElement(getDurationInMicros(), coverage));
  }

  public void resetTimeStamps() {
    startTime = Instant.now();
    previousCoveragePerTimeStamps = coveragePerTimestamps;
    initCoveragePerTimestamps();
  }

  public List<TimeDependentCoverageDataElement> getReducedCoveragePerTimestamps(int max) {
    return thinOutMap(getCoveragePerTimestamps(), max);
  }

  public List<TimeDependentCoverageDataElement> getCoveragePerTimestamps() {
    if (!previousCoveragePerTimeStamps.isEmpty()) {
      double maxPreviousCoverage =
          previousCoveragePerTimeStamps.stream()
              .map(l -> l.getValue())
              .reduce(Double::max)
              .orElseThrow();
      double maxCoverage =
          coveragePerTimestamps.stream().map(l -> l.getValue()).reduce(Double::max).orElseThrow();
      if (maxPreviousCoverage > maxCoverage) {
        return previousCoveragePerTimeStamps;
      }
    }
    return coveragePerTimestamps;
  }

  private void initCoveragePerTimestamps() {
    coveragePerTimestamps = new ArrayList<>();
    coveragePerTimestamps.add(new TimeDependentCoverageDataElement());
  }

  private TimeSpan getDurationInMicros() {
    long durationInNanos = Duration.between(startTime, Instant.now()).toNanos();
    return TimeSpan.ofNanos(durationInNanos);
  }

  private void initStartTime() {
    if (startTime.equals(Instant.MIN)) {
      startTime = Instant.now();
    }
  }

  private List<TimeDependentCoverageDataElement> thinOutMap(
      List<TimeDependentCoverageDataElement> list, int max) {
    if (max < 0) {
      max = 0;
    }
    int listSize = list.size();
    if (listSize < max) {
      return list;
    }
    List<TimeDependentCoverageDataElement> outputList = new ArrayList<>();
    int ruleOutQuotient = (int) Math.ceil(listSize / (double) max);
    for (int i = 0; i < listSize; i += ruleOutQuotient) {
      outputList.add(list.get(i));
    }
    return outputList;
  }
}
