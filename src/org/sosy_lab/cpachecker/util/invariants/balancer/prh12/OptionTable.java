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
import org.sosy_lab.cpachecker.util.invariants.balancer.Assumption;
import org.sosy_lab.cpachecker.util.invariants.balancer.Assumption.AssumptionType;
import org.sosy_lab.cpachecker.util.invariants.balancer.AssumptionSet;
import org.sosy_lab.cpachecker.util.invariants.balancer.Matrix;

/*
 * This class is used by PivotRowHandler.
 * It creates and manages a numerical table for the "remaining rows", indicating what are
 * the options for each row, to ensure that the matrix has a solution.
 *
 * Codes:
 * 0: a zero entry in the matrix
 * 1: a positive constant entry in the matrix
 * 2: a variable entry in the matrix
 * 3: a negative constant entry in the matrix
 * 4: says row has no constant negative augmentation entries
 * 5: says row has a constant negative augmentation entry
 * 6: a variable entry which we will assume to be nonpositive
 * 7: a variable entry which we will assume to be negative
 * 8: says we'll assume all augmentation entries in the row to be nonnegative
 * 9: a constant negative entry s.t. we will assume all variable entries in the same
 *    column to be at least nonpositive, and maybe even negative
 *
 * Rules:
 * The table is "satisfied" when each row has a 7, 8, or 9 in it.
 * You may make the following changes:
 *   4 -> 8
 *   6 -> 7
 *   2 -> 7, plus 2 -> 6 for all 2's in same column
 *   3 -> 9, plus 2 -> 6 for all 2's in same column
 * Once the table is satisfied, you must build an assumption set with the following
 * assumptions, according to entries in the table:
 *   6: this quantity must be assumed nonpositive
 *   7: this quantity must be assumed negative
 *   8: all augmentation entries in this row must be assumed nonnegative
 *
 */
public class OptionTable {

    private int m = 0;
    private int c = 0;
    private int[][] table;
    private List<List<Integer>> usableColumns;

    private LogManager logger;
    private PivotRowHandler prh;
    private Matrix mat;
    private List<Integer> remainingRows;
    private List<Integer> CU;


    public OptionTable(PivotRowHandler p, Matrix mx, int[][] a, List<Integer> rr, List<Integer> cu, LogManager lm) {
      logger = lm;
      prh = p;
      mat = mx;
      remainingRows = rr;
      CU = cu;
      table = a;
      m = a.length;
      if (m > 0) {
        c = a[0].length;
      }
      usableColumns = computeUsableColumns();
    }

    public List<Integer> getRemainingRows() {
      return remainingRows;
    }

    /*
     * Return a list of those rows that have just a single option.
     */
    public List<Integer> getSoleOptionRows() {
      List<Integer> sole = new Vector<>();
      for (Integer i : remainingRows) {
        List<Integer> l = usableColumns.get(i);
        if (l.size() == 1) {
          sole.add(i);
        }
      }
      return sole;
    }

    public AssumptionSet takeSoleOptions() {
      AssumptionSet aset = new AssumptionSet();
      List<Integer> rows = getSoleOptionRows();
      List<Integer> discard = new Vector<>();
      for (Integer i0 : rows) {
        // Get the number j0 of the one column that row i0 can use.
        int j0 = usableColumns.get(i0).get(0);
        // The code there must be one of 2, 3, 4, or 6.
        int code = table[i0][j0];
        AssumptionSet as;
        switch (code) {
        case 2:
          as = change2to7(i0, j0);
          aset.addAll(as);
          discard.add(i0);
          logger.log(Level.ALL, "Dicarding row",i0,"and adding assumptions",
              as.toString()+". Row has only this option in option table.");
          break;
        case 3:
          as = change3to9(i0, j0);
          aset.addAll(as);
          discard.add(i0);
          logger.log(Level.ALL, "Dicarding row",i0,"and adding assumptions",
              as.toString()+". Row has only this option in option table.");
          break;
        case 4:
          as = change4to8(i0);
          aset.addAll(as);
          discard.add(i0);
          logger.log(Level.ALL, "Dicarding row",i0,"and adding assumptions",
              as.toString()+". Row has only this option in option table.");
          break;
        case 6:
          as = change6to7(i0, j0);
          aset.addAll(as);
          discard.add(i0);
          logger.log(Level.ALL, "Dicarding row",i0,"and adding assumptions",
              as.toString()+". Row has only this option in option table.");
          break;
        }
      }
      // Remove discarded rows from consideration.
      remainingRows.removeAll(discard);
      // Return the set of assumptions.
      return aset;
    }

    private AssumptionSet change2to7(int i, int j) {
      AssumptionSet aset = new AssumptionSet();
      table[i][j] = 7;
      aset.add(new Assumption(mat.getEntry(i, j), AssumptionType.NEGATIVE));
      for (Integer k : remainingRows) {
        if (k != i && table[k][j] == 2) {
          table[k][j] = 6;
          aset.add(new Assumption(mat.getEntry(k, j), AssumptionType.NONPOSITIVE));
        }
      }
      return aset;
    }

    private AssumptionSet change3to9(int i, int j) {
      AssumptionSet aset = new AssumptionSet();
      table[i][j] = 9;
      for (Integer k : remainingRows) {
        if (k != i && table[k][j] == 2) {
          table[k][j] = 6;
          aset.add(new Assumption(mat.getEntry(k, j), AssumptionType.NONPOSITIVE));
        }
      }
      return aset;
    }

    private AssumptionSet change4to8(int i) {
      table[i][c-1] = 8;
      return prh.ar2nonneg(i);
    }

    private AssumptionSet change6to7(int i, int j) {
      AssumptionSet aset = new AssumptionSet();
      table[i][j] = 7;
      aset.add(new Assumption(mat.getEntry(i, j), AssumptionType.NEGATIVE));
      return aset;
    }

    /*
     * Make a list in which the ith entry says the columns
     * that row i could use.
     */
    private List<List<Integer>> computeUsableColumns() {
      List<List<Integer>> usable = new Vector<>(m);
      for (int i = 0; i < m; i++) {
        usable.add(new Vector<Integer>());
      }
      for (Integer i : remainingRows) {
        // Which columns are usable for row i?
        List<Integer> u = new Vector<>();
        // Consider the conditionally unblocked columns.
        for (Integer j : CU) {
          // A column is usable iff it has a 2, 3, 4, or 6.
          int a = table[i][j];
          if (2 <= a && a <= 6 && a != 5) {
            u.add(j);
          }
        }
        // Consider the augmentation columns.
        int a = table[i][c-1];
        if (2 <= a && a <= 6 && a != 5) {
          u.add(c-1);
        }
        usable.set(i, u);
      }
      return usable;
    }

    @Override
    public String toString() {
      String s = "";
      s += "Option Table:\n";
      s += "Remaining rows: "+remainingRows.toString()+"\n";
      s += "Conditionally unblocked columns: "+CU.toString()+"\n";
      s += "Usable columns:\n"+usableColumns.toString()+"\n";
      s += "Code table:\n";
      for (int i = 0; i < m; i++) {
        s += Integer.toString(i)+"  [";
        for (int j = 0; j < c; j++) {
          s += " "+Integer.toString(table[i][j]);
        }
        s += " ]\n";
      }
      return s;
    }

}
