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

import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;


public class StatTimer extends AbstractStatValue {

  private final Timer timer = new Timer();

  public StatTimer(StatKind pMainStatisticKind, String pTitle) {
    super(pMainStatisticKind, pTitle);
  }

  public StatTimer(String pTitle) {
    super(StatKind.SUM, pTitle);
  }

  public void start() {
    timer.start();
  }

  public void stop() {
    timer.stop();
  }

  @Override
  public int getUpdateCount() {
    return timer.getNumberOfIntervals();
  }

  @Override
  public String toString() {
    return timer.toString();
  }

  public TimeSpan getConsumedTime() {
    return timer.getSumTime();
  }

  public TimeSpan getMaxTime() {
    return timer.getMaxTime();
  }
}
