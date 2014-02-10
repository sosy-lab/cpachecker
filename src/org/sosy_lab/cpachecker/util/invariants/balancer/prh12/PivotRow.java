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

import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;


public class PivotRow {

  private LogManager logger;
  private final int rowNum;
  private final List<UsableColumn> usable;
  private int reqPtr = 0;
  private PivotRow nextRow = null;

  public PivotRow(int i, LogManager lm) {
    logger = lm;
    rowNum = i;
    usable = new Vector<>();
  }

  private void reset() {
    reqPtr = 0;
  }

  public void setNextRow(PivotRow n) {
    nextRow = n;
  }

  /*
   * Returns true if a request was made; false if not.
   */
  public boolean makeNextRequest() {
    // If we have a next row, then we first ask it to make its next request.
    // If it fails to do so, then we reset it, and ask again, and increment our own
    // pointer, before making our own request.
    if (nextRow != null) {
      boolean nextRowMadeRequest = nextRow.makeNextRequest();
      if (!nextRowMadeRequest) {
        // Next row didn't make request.
        nextRow.reset();
        nextRow.makeNextRequest();
        reqPtr++;
      }
    }
    // Now make our own.
    boolean result = false;
    if (usable != null && reqPtr < usable.size()) {
      // Make request.
      UsableColumn u = usable.get(reqPtr);
      u.makeRequest(rowNum);
      //diag:
      logger.log(Level.ALL, "Making request, integer",rowNum);
      logger.log(Level.ALL, "of column",u);
      //
      logger.log(Level.ALL, "Row",rowNum,"attempting to use column",u.getColNum());
      result = true;
      // And if we don't have a next row, then we need to do our own increment now.
      if (nextRow == null) {
        reqPtr++;
      }
    }
    return result;
  }

  public int getRowNum() {
    return rowNum;
  }

  public void addUsableColumn(UsableColumn u) {
    usable.add(u);
  }

  /*
   * Say whether this row has just a sole option.
   */
  public boolean hasSoleOption() {
    int numOpts = usable.size();
    return numOpts == 1;
  }

  /*
   * If this row has a sole option, request that option and return true.
   * Otherwise, return false.
   */
  public boolean makeSoleRequest() {
    if (hasSoleOption()) {
      UsableColumn u = usable.get(0);
      u.makeRequest(rowNum);
      logger.log(Level.ALL, "Row",rowNum,"attempting to use column",u.getColNum());
      return true;
    } else {
      return false;
    }
  }

}
