// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.tdcg;

import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.common.time.Tickers;
import org.sosy_lab.common.time.Tickers.TickerWithUnit;
import org.sosy_lab.common.time.TimeSpan;

/**
 * Time-dependent Coverage Data is a data structure used by Time-dependent coverage graphs (TDCGs).
 * It holds a Map of (Long, Double) entries where Longs are interpreted as time and the Doubles as
 * raw coverage value at that corresponding time. Coverage values are calculated within a
 * CoverageCPA.
 */
public class TimeDependentCoverageData {
  private List<TimeDependentCoverageDataElement> coveragePerTimestamps;
  private long startTimeInNanos = 0;
  private final TickerWithUnit TICKER = Tickers.getWalltimeNanos();

  public static class TimeDependentCoverageDataElement {
    private final TimeSpan time;
    private final double value;

    private TimeDependentCoverageDataElement(TimeSpan pTime, double pValue) {
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

    public double getValue() {
      return value;
    }
  }

  public TimeDependentCoverageData() {
    initCoveragePerTimestamps();
  }

  public void addTimestamp(double coverage) {
    coveragePerTimestamps.add(
        new TimeDependentCoverageDataElement(getDurationInMicros(), coverage));
  }

  public List<TimeDependentCoverageDataElement> getReducedCoveragePerTimestamps(int max) {
    return thinOutMap(getCoveragePerTimestamps(), max);
  }

  public List<TimeDependentCoverageDataElement> getCoveragePerTimestamps() {
    return coveragePerTimestamps;
  }

  private void initCoveragePerTimestamps() {
    coveragePerTimestamps = new ArrayList<>();
    coveragePerTimestamps.add(new TimeDependentCoverageDataElement());
  }

  private TimeSpan getDurationInMicros() {
    long durationInNanos = 0;
    if (startTimeInNanos == 0) {
      startTimeInNanos = TICKER.read();
    } else {
      durationInNanos = TICKER.read() - startTimeInNanos;
    }
    return TimeSpan.ofNanos(durationInNanos);
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
