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
package org.sosy_lab.cpachecker.util.invariants.balancer.prh12;

import java.util.Collections;
import java.util.LinkedList;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;


public class PivotRow2 implements Comparable<PivotRow2> {

  private LogManager logger;
  private final int rowNum;
  private final LinkedList<UsableColumn> usable;
  private boolean augColOption = false;
  private int height = -1;

  public PivotRow2(int i, LogManager lm) {
    logger = lm;
    rowNum = i;
    usable = new LinkedList<>();
  }

  @Override
  public int compareTo(PivotRow2 other) {
    // We want that row to come first which has the greatest height.
    return other.rowNum - this.rowNum;
  }

  public int getRowNum() {
    return rowNum;
  }

  /*
   * Check whether any usable column has a height. If so, take height h and return true.
   * Else return false.
   */
  boolean discoverHeight(int h) {
    boolean discovered = false;
    for (UsableColumn u : usable) {
      int g = u.getHeight();
      if (g >= 0) {
        height = h;
        discovered = true;
        break;
      }
    }
    return discovered;
  }

  public void setHeight(int h) {
    height = h;
  }

  public int getHeight() {
    return height;
  }

  public void addFreeColumn(UsableColumn u) {
    usable.addLast(u);
    // We keep the columns sorted by lowest height first.
    Collections.sort(usable);
  }

  public void addAugColumn(UsableColumn u) {
    augColOption = true;
    usable.addFirst(u);
  }

  /*
   * Return the total number of options that this row has, including an aug col option.
   */
  public int getTotalOptionCount() {
    return usable.size();
  }

  /*
   * Return the number of options the row has, minus any aug col option.
   */
  public int getFreeColOptionCount() {
    int n = usable.size();
    if (hasAugColOption()) {
      n--;
    }
    return n;
  }

  public boolean hasAugColOption() {
    return augColOption;
  }

  /*
   * Get option i, including the aug col option if we have one and if i = 0.
   * null if have no ith option.
   */
  public UsableColumn getGeneralOption(int i) {
    UsableColumn opt = null;
    if (i < usable.size()) {
      opt = usable.get(i);
    }
    return opt;
  }

  /*
   * Get the ith free option. If we have no aug col, this just means we get the ith option.
   * Otherwise, it means we get the i+1st option, since the aug col is the first.
   * null if have no ith free option.
   */
  public UsableColumn getFreeOption(int i) {
    if (!hasAugColOption()) {
      return getGeneralOption(i);
    } else {
      UsableColumn opt = null;
      if (i < usable.size() - 1) {
        opt = usable.get(i + 1);
      }
      return opt;
    }
  }

  /*
   * If this row has a sole option, request that option and return true.
   * Otherwise, return false.
   */
  public boolean makeSoleRequest() {
    if (getTotalOptionCount() == 1) {
      UsableColumn u = getGeneralOption(0);
      u.makeRequest(rowNum);
      logger.log(Level.ALL, "Row",rowNum,"attempting to use column",u.getColNum());
      return true;
    } else {
      return false;
    }
  }


}
