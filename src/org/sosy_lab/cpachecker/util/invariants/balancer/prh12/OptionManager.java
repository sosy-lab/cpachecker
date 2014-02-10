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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.util.invariants.balancer.AssumptionSet;


public class OptionManager {

  @SuppressWarnings("unused")
  private LogManager logger;
  private Map<Integer, UsableColumn> cols;
  private List<PivotRow> rows;
  private boolean initialized;

  public OptionManager(LogManager lm) {
    logger = lm;
    cols = new HashMap<>();
    rows = new Vector<>();
    initialized = false;
  }

  private void initialize() {
    if (rows.size() >= 2) {
      // We link the rows together.
      for (int i = 1; i < rows.size(); i++) {
        PivotRow pr1 = rows.get(i-1);
        PivotRow pr2 = rows.get(i);
        pr1.setNextRow(pr2);
      }
    }
    initialized = true;
  }

  public int numRemainingRows() {
    return rows.size();
  }

  public AssumptionSet nextTry() {
    if (rows.size() == 0) {
      return null;
    }
    if (!initialized) {
      initialize();
    }
    // Clear any prior requests.
    for (UsableColumn u : cols.values()) {
      u.clearRequests();
    }
    // Now make the next request.
    boolean madeRequest = rows.get(0).makeNextRequest();
    if (!madeRequest) {
      // then we are out of options.
      return null;
    } else {
      return getRequestedAssumptions();
    }
  }

  public void addUsableColumn(UsableColumn u, Integer j) {
    cols.put(j, u);
  }

  public UsableColumn getUsableColumn(Integer j) {
    return cols.get(j);
  }

  public void addPivotRow(PivotRow pr) {
    rows.add(pr);
  }

  private AssumptionSet getRequestedAssumptions() {
    AssumptionSet aset = new AssumptionSet();
    for (UsableColumn u : cols.values()) {
      AssumptionSet a = u.getRequestedAssumptions();
      logger.log(Level.ALL, "Column",u.getColNum(),"produced assumption set",a);
      aset.addAll(a);
    }
    return aset;
  }

  private void discardRows(List<PivotRow> discard) {
    rows.removeAll(discard);
  }

  /*
   * Return set of assumptions for those rows that have just a sole option.
   * Empty if there are no such rows.
   *
   * We also remove any such rows from further consideration.
   */
  public AssumptionSet getSoleOptionRowsAssumptions() {
    List<PivotRow> discard = new Vector<>();
    for (PivotRow pr : rows) {
      boolean madeRequest = pr.makeSoleRequest();
      if (madeRequest) {
        discard.add(pr);
      }
    }
    AssumptionSet aset = getRequestedAssumptions();
    discardRows(discard);
    return aset;
  }


}
