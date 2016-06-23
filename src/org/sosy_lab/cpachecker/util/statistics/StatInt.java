/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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


public class StatInt extends AbstractStatValue {

  private int maxValue = Integer.MIN_VALUE;
  private int minValue = Integer.MAX_VALUE;
  private int valueCount = 0;
  private int valueSum = 0;

  public StatInt(StatKind pMainStatisticKind, String pTitle) {
    super(pMainStatisticKind, pTitle);
  }

  public void setNextValue(int newValue) {
    valueSum += newValue;
    valueCount += 1;
    maxValue = Math.max(newValue, maxValue);
    minValue = Math.min(newValue, minValue);
  }

  public int getMaxValue() {
    return valueCount == 0 ? 0 : maxValue;
  }

  public int getMinValue() {
    return valueCount == 0 ? 0 : minValue;
  }

  public int getValueCount() {
    return valueCount;
  }

  public int getValueSum() {
    return valueSum;
  }

  public float getAverage() {
    if (valueCount > 0) {
      return (float) valueSum / (float) valueCount;
    } else {
      return 0;
    }
  }

  @Override
  public int getUpdateCount() {
    return valueCount;
  }

  @Override
  public String toString() {
    return String.format("%8d (count: %d, min: %d, max: %d, avg: %.2f)",
        valueSum, valueCount, getMinValue(), getMaxValue(), getAverage());
  }

}