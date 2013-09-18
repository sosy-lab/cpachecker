/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import java.io.PrintStream;

public class StatisticsWriter {

  private final StatisticsWriter parentLevelWriter;
  private final PrintStream target;
  private final int level;
  private final int outputNameColWidth;

  private StatisticsWriter(PrintStream pTarget, int pLevel,
      int pOutputNameColWidth, StatisticsWriter pParentLevelWriter) {
    this.parentLevelWriter = pParentLevelWriter;
    this.target = pTarget;
    this.level = pLevel;
    this.outputNameColWidth = pOutputNameColWidth;
  }

  public static StatisticsWriter writingStatisticsTo(PrintStream pTarget) {
    return new StatisticsWriter(pTarget, 0, 50, null);
  }

  public StatisticsWriter withNameColumnWith(int pWidth) {
    return new StatisticsWriter(target, level, pWidth, this);
  }

  public StatisticsWriter beginLevel() {
    return new StatisticsWriter(target, level + 1, outputNameColWidth, this);
  }

  public StatisticsWriter withLevel(int pLevel) {
    return new StatisticsWriter(target, pLevel, outputNameColWidth, this);
  }

  public StatisticsWriter endLevel() {
    if (parentLevelWriter == null) {
      return this;
    } else {
      return parentLevelWriter;
    }
  }

  public StatisticsWriter spacer() {
    target.println();
    return this;
  }

  public StatisticsWriter put(String name, Object value) {
    StatisticsUtils.write(target, level, outputNameColWidth, name, value);
    return this;
  }

  public StatisticsWriter put(AbstractStatValue stat) {
    StatisticsUtils.write(target, level, outputNameColWidth, stat);
    return this;
  }
}