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

/**
 * Thread-safe implementation of numerical statistics.
 * This class tracks the number how often each value is added.
 */
public class StatIntHist extends StatInt {

  private Multiset<Integer> hist = ConcurrentHashMultiset.create();

  public StatIntHist(StatKind pMainStatisticKind, String pTitle) {
    super(pMainStatisticKind, pTitle);
  }

  public int getTimesWithValue(Integer value) {
    return hist.count(value);
  }

  @Override
  public void setNextValue(int pNewValue) {
    super.setNextValue(pNewValue);
    hist.add(pNewValue);
  }

  @Override
  public String toString() {
    return super.toString() + " " + hist.toString();
  }

}