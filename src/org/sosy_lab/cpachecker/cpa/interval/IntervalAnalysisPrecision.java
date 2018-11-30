/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.interval;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class IntervalAnalysisPrecision implements Precision {
  Map<String, Pair<Long, Long>> precision;

  public IntervalAnalysisPrecision() {
    precision = new HashMap<>();
  }

  public IntervalAnalysisPrecision(Collection<String> pPrecision) {
    precision = new HashMap<>();
    for (String nprecision : pPrecision) {
      precision.put(nprecision, Pair.of(Long.valueOf(0), Long.valueOf(0)));
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof IntervalAnalysisPrecision) {
      IntervalAnalysisPrecision x = (IntervalAnalysisPrecision) o;
      return precision.equals(x.getPrecision());
    }
    return false;
  }

  public Pair<Long, Long> getInterval(String memString) {
    if (precision.containsKey(memString)) {
      return precision.get(memString);
    }
    return Pair.of(Long.valueOf(0), Long.valueOf(0));
  }

  public Map<String, Pair<Long, Long>> getPrecision() {
    return precision;
  }

  public boolean isTracking(String memoryLocation) {
    return precision.keySet().contains(memoryLocation);
  }

  public int getSize() {
    return precision.size();
  }

  public boolean isEmpty() {
    return precision.isEmpty();
  }

  public void remove(String x) {
    if (precision.keySet().contains(x)) {
      precision.remove(x);
    }
  }

  public void add(String s) {
    precision.put(s, Pair.of(Long.valueOf(0), Long.valueOf(0)));
  }

  public void addAll(Collection<String> add) {
    for (String s : add) {
      add(s);
    }
  }

  public void join(IntervalAnalysisPrecision x) {
    for (Entry<String, Pair<Long, Long>> prec : x.getPrecision().entrySet()) {
      if (!precision.containsKey(prec.getKey())) {
        precision.put(prec.getKey(), prec.getValue());
      } else {
        // join the Integer
      }
    }
  }

  public void setLow(String memoryLocation, Long value) {
    if (precision.containsKey(memoryLocation)) {
        Long high = precision.get(memoryLocation).getSecond();
        precision.replace(memoryLocation, Pair.of(value, high));
    }
  }

  public void setHigh(String memoryLocation, Long value) {
    if (precision.containsKey(memoryLocation)) {
      Long low = precision.get(memoryLocation).getFirst();
      precision.replace(memoryLocation, Pair.of(low, value));
    }
  }

  public boolean containsVariable(String variableName) {
    return precision.keySet().contains(variableName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(precision);
  }

  @Override
  public String toString() {
    return "IntervalAnalysisPrecision: [ " + precision.toString() + " ]";
  }

  public static class IntervalAnalysisFullPrecision extends IntervalAnalysisPrecision {
    public IntervalAnalysisFullPrecision() {
      super();
    }

    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    public String toString() {
      return "IntervalAnalysisFullPrecision";
    }

    @Override
    public boolean isTracking(String memoryLocation) {
      return true;
    }

    @Override
    public boolean containsVariable(String variableName) {
      return false;
    }
  }
}
