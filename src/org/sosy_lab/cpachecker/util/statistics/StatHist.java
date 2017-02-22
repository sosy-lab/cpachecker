/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.statistics;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;

/**
 * Thread-safe implementation of numerical statistics.
 * This class tracks how often a value is used in a series of values.
 * Use case might be sampling of values during an analysis.
 */
public class StatHist extends AbstractStatValue {

  private final Multiset<Integer> hist = ConcurrentHashMultiset.create();

  public StatHist(String pTitle) {
    super(StatKind.AVG, pTitle);
  }

  public int getTimesWithValue(Integer value) {
    return hist.count(value);
  }

  public void insertValue(int pNewValue) {
    hist.add(pNewValue);
  }

  @Override
  public String toString() {
    return String.format("%s (avg=%.2f, dev=%.2f)", hist, getAvg(), getStdDeviation());
  }

  private double getStdDeviation() {
    synchronized (hist) {
      final double avg = getAvg();
      double sum = 0;
      for (Entry<Integer> e : hist.entrySet()) {
        double deviation = avg - e.getElement();
        sum += (deviation * deviation * e.getCount());
      }
      return sum / hist.size();
    }
  }

  private double getAvg() {
    synchronized (hist) {
      long sum = 0;
      for (Entry<Integer> e : hist.entrySet()) {
        sum += (e.getElement() * e.getCount());
      }
      return (double) sum / hist.size();
    }
  }

  @Override
  public int getUpdateCount() {
    return hist.size();
  }

}