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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

public class IntervalAnalysisPrecision implements Precision {

  private final Map<String, Long> precision;

  public IntervalAnalysisPrecision(Collection<String> pPrecision) {
    precision = new HashMap<>();
    for (String s : pPrecision) {
      add(s);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof IntervalAnalysisPrecision) {
      IntervalAnalysisPrecision x = (IntervalAnalysisPrecision) o;
      return precision.equals(x.precision);
    }
    return false;
  }

  public boolean isTracking(String memoryLocation) {
    return precision.containsKey(memoryLocation);
  }

  public int getSize() {
    return precision.size();
  }

  public boolean isEmpty() {
    return precision.isEmpty();
  }

  public void remove(String x) {
    precision.remove(x);
  }

  public void add(String s) {
    precision.put(s, Long.MAX_VALUE);
  }

  public long getValue(String memLocation){
    return precision.get(memLocation);
  }

  public void replace(String memLocation, long size){
    precision.replace(memLocation, size);
  }

  /**
   * whether this precision explicitly tracks the given variable.
   *
   * @see IntervalAnalysisFullPrecision
   */
  public boolean containsVariable(String variableName) {
    return isTracking(variableName);
  }

  public void join(IntervalAnalysisPrecision pOther){
    for (Entry<String, Long> other : pOther.precision.entrySet()) {
      if(precision.containsKey(other.getKey())){
        if(other.getValue() < precision.get(other.getKey())){
          precision.replace(other.getKey(), other.getValue());
        }
      }else{
        precision.put(other.getKey(), other.getValue());
      }
    }
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
      super(Collections.emptySet());
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
