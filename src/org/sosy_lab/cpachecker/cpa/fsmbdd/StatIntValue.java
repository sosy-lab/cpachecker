/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.fsmbdd;


public class StatIntValue {
  private Integer lastValue;
  private int max;
  private int min;
  private int count;
  private double sum;

  public StatIntValue() {
    this.lastValue = null;
    this.count = 0;
    this.max = Integer.MIN_VALUE;
    this.min = Integer.MAX_VALUE;
  }

  public void noteValue(int value) {
    this.lastValue = value;
    this.max = Math.max(this.max, value);
    this.min = Math.min(this.min, value);
    this.count += 1;
    this.sum += value;
  }

  public int getMax() {
    return max;
  }

  public int getMin() {
    return min;
  }

  public int getCount() {
    return count;
  }

  public Integer getLastValue() {
    return lastValue;
  }

  public double getMean() {
    if (count > 0) {
      return (sum / count);
    } else {
      return 0;
    }
  }
}
