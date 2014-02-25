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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.util.invariants.balancer.AssumptionSet;
import org.sosy_lab.cpachecker.util.invariants.balancer.prh12.ColumnChoiceFrame.ChallengeType;


public class OptionManager2 {

  @SuppressWarnings("unused")
  private LogManager logger;
  private PivotRowHandler2 prh;
  private Map<Integer, UsableColumn> cols;
  private List<PivotRow2> rows;

  public OptionManager2(PivotRowHandler2 p, LogManager lm) {
    prh = p;
    logger = lm;
    cols = new HashMap<>();
    rows = new Vector<>();
  }

  public int numRemainingRows() {
    return rows.size();
  }

  public void addUsableColumn(UsableColumn u, Integer j) {
    cols.put(j, u);
  }

  public UsableColumn getUsableColumn(Integer j) {
    return cols.get(j);
  }

  public void addPivotRow(PivotRow2 pr) {
    rows.add(pr);
  }

  AssumptionSet getRequestedAssumptions() {
    AssumptionSet aset = new AssumptionSet();
    for (UsableColumn u : cols.values()) {
      AssumptionSet a = u.getRequestedAssumptions();
      logger.log(Level.ALL, "Column",u.getColNum(),"produced assumption set",a);
      aset.addAll(a);
    }
    return aset;
  }

  private void discardRows(List<PivotRow2> discard) {
    rows.removeAll(discard);
  }

  /*
   * Return set of assumptions for those rows that have just a sole option.
   * Empty if there are no such rows.
   *
   * We also remove any such rows from further consideration.
   */
  public AssumptionSet getSoleOptionRowsAssumptions() {
    List<PivotRow2> discard = new Vector<>();
    for (PivotRow2 pr : rows) {
      boolean madeRequest = pr.makeSoleRequest();
      if (madeRequest) {
        discard.add(pr);
      }
    }
    AssumptionSet aset = getRequestedAssumptions();
    discardRows(discard);
    return aset;
  }

  /*
   * Return a list of all rows that do not have a height.
   * If we have assigned heights properly, and since we have already dealt in the First Pass
   * with all rows having the aug col as their sole option, it should be true that any
   * row that did not get a height is involved in a cyclic dependency.
   */
  List<PivotRow2> getRowsLackingHeights() {
    List<PivotRow2> r = new Vector<>();
    for (PivotRow2 pr : rows) {
      int g = pr.getHeight();
      if (g < 0) {
        r.add(pr);
      }
    }
    return r;
  }

  /*
   * Assign heights to the rows and columns.
   */
  public void assignHeights() {
    // First tell each column the rows in which it has 1s, according
    // to the current code table in the pivot row handler.
    // In the process, columns of height 0 register the fact.
    findRowsWith1s();

    // Next get rows of height 0 to discover their height.
    int h = 0;
    boolean changed = assignRowHeights(h);

    // Now continue to alternate between columns and rows, until one fails to change.
    while (changed) {
      h++;
      changed &= assignColHeights(h);
      if (!changed) {
        break;
      }
      changed &= assignRowHeights(h);
    }
  }

  /*
   * For all cols c, if every row in which col c has a 1 has been assigned a height,
   * then ask col c to take height h. Return true if any column takes on a height; false otherwise.
   */
  private boolean assignColHeights(int h) {
    boolean changed = false;
    for (UsableColumn u : cols.values()) {
      changed |= u.discoverHeight(h);
    }
    return changed;
  }

  /*
   * For all rows r, if row r has a usable column c that has been assigned a height
   * yet, then ask row r to take height h.
   * Return true if any row is assigned a height; false otherwise.
   */
  private boolean assignRowHeights(int h) {
    boolean changed = false;
    for (PivotRow2 pr : rows) {
      changed |= pr.discoverHeight(h);
    }
    return changed;
  }

  /*
   * Tell each column the rows in which it has 1s, according
   * to the current code table in the pivot row handler.
   * Those columns that are of height 0, i.e. which have no 1s in them,
   * will automatically register the fact, at this time.
   */
  private void findRowsWith1s() {
    for (Integer c : cols.keySet()) {
      if (c.intValue() == -1) {
        // Skip the aug col (which always has column number -1), since it doesn't get a height.
        continue;
      }
      // Determine the numbers of rows that have 1s.
      List<Integer> rowNums = prh.getRowsWith1sInCol(c);
      // Turn these into actual rows.
      List<PivotRow2> r = new Vector<>(rowNums.size());
      for (PivotRow2 pr : rows) {
        Integer i = Integer.valueOf(pr.getRowNum());
        if (rowNums.contains(i)) {
          r.add(pr);
        }
      }
      // Record the list in the column.
      cols.get(c).setRowsWith1s(r);
    }
  }

  ColumnChoiceFrame buildChoiceFrame(List<Integer> rl, ChallengeType ct) {
    // Build list of pivot rows, based on passed row numbers.
    List<PivotRow2> prs = new Vector<>(rl.size());
    for (PivotRow2 pr : rows) {
      if (rl.contains(pr.getRowNum())) {
        prs.add(pr);
      }
    }
    // Sort so that rows of greatest height come first.
    Collections.sort(prs);
    // Construct the choice frame and return it.
    ColumnChoiceFrame ccf = new ColumnChoiceFrame(prs, ct);
    return ccf;
  }

}
