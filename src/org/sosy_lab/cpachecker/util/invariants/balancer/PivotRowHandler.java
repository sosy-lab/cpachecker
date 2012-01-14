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
package org.sosy_lab.cpachecker.util.invariants.balancer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.util.invariants.balancer.Assumption.AssumptionType;
import org.sosy_lab.cpachecker.util.invariants.balancer.MatrixSolvingFailedException.Reason;


public class PivotRowHandler {

  private final LogManager logger;
  private final Matrix mat;
  private final int m, n, aug;
  private final Vector<Integer> remainingRows;
  private final int[][] codes;
  private final List<Integer> AU, CU;

  //-----------------------------------------------------------------
  // Constructing

  public PivotRowHandler(Matrix mx, LogManager lm) {
    logger = lm;
    mat = mx;
    m = mx.getRowNum();
    n = mx.getColNum();
    aug = n - mx.getNumAugCols();

    // Initialize the "remaining rows" as just those that are pivot rows in the matrix.
    remainingRows = new Vector<Integer>(m);
    for (int i = 0; i < m; i++) {
      if (mat.isPivotRow(i)) {
        remainingRows.add(new Integer(i));
      }
    }

    codes = buildCodes();
    List<List<Integer>> unblocked = computeUnblockedColumns();
    AU = unblocked.get(0);
    CU = unblocked.get(1);

  }

  public int[] intListToArray(List<Integer> list) {
    // Convert to int arrays.
    int k = list.size();
    int[] a = new int[k];
    for (int l = 0; l < k; l++) {
      a[l] = list.get(l).intValue();
    }
    return a;
  }

  private List<List<Integer>> computeUnblockedColumns() {
    // Compute absolutely and conditionally unblocked columns.
    Vector<Integer> auv = new Vector<Integer>();
    Vector<Integer> cuv = new Vector<Integer>();
    for (int j = 0; j < n; j++) {
      boolean absolute = true;
      boolean conditional = true;
      for (int i = 0; i < m; i++) {
        if (codes[i][j] == 1) {
          absolute = false;
          conditional = false;
          break;
        }
        else if (codes[i][j] == 2) {
          absolute = false;
        }
      }
      if (absolute) {
        auv.add(new Integer(j));
        cuv.add(new Integer(j));
      } else if (conditional) {
        cuv.add(new Integer(j));
      }
    }
    // Return.
    List<List<Integer>> u = new Vector<List<Integer>>();
    u.add(auv);
    u.add(cuv);
    return u;
  }

  /*
   * We represent each entry f of the matrix by a code:
   * 0: f is identically 0
   * 1: f is a positive constant
   * 2: f is variable
   * 3: f is a negative constant
   */
  private int[][] buildCodes() {
    int[][] codes = new int[m][n];
    RationalFunction f;
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        f = mat.getEntry(i, j);
        if (!f.isConstant()) {
          // f is variable
          codes[i][j] = 2;
        }
        else if (f.isZero()) {
          // f is identically zero
          codes[i][j] = 0;
        }
        else if (f.isPositive()) {
          // f is a positive constant
          codes[i][j] = 1;
        }
        else {
          // f is a negative constant
          codes[i][j] = 3;
        }
      }
    }
    return codes;
  }

  //-----------------------------------------------------------------
  // Solving

  public Set<Set<Assumption>> handlePivotRows() throws MatrixSolvingFailedException {
    // Initialize subset of assumptions that will be used in all sets.
    Set<Assumption> inAll = new HashSet<Assumption>();
    // Make the first pass:
    inAll.addAll( firstPass() );
    // TODO: the rest!
    Set<Set<Assumption>> all = new HashSet<Set<Assumption>>();
    all.add(inAll);
    return all;
  }

  private void discardRows(List<Integer> d) {
    remainingRows.removeAll(d);
  }

  /*
   * Check whether all augmentation entries in row r are of code 0 or 1.
   */
  private boolean FAar01(Integer r) {
    boolean ans = true;
    for (int j = aug; j < n; j++) {
      if (codes[r][j] >= 2) {
        ans = false; break;
      }
    }
    return ans;
  }

  /*
   * Check whether there is a post-pivot entry in row r of code 3, which lies in
   * a column that is absolutely unblocked.
   */
  private boolean EXpr3AU(Integer r) {
    boolean ans = false;
    for (int j = 0; j < aug; j++) {
      if (codes[r][j] == 3 && AU.contains(j)) {
        ans = true; break;
      }
    }
    return ans;
  }

  /*
   * Check whether there is an aug entry in row r of code 3.
   */
  private boolean EXar3(Integer r) {
    boolean ans = false;
    for (int j = aug; j < n; j++) {
      if (codes[r][j] == 3) {
        ans = true; break;
      }
    }
    return ans;
  }

  /*
   * Check whether all post-pivots in row r are of code 0 or 1.
   */
  private boolean FApr01(Integer r) {
    boolean ans = true;
    for (int j = 0; j < aug; j++) {
      if (codes[r][j] >= 2) {
        ans = false; break;
      }
    }
    return ans;
  }

  private Set<Assumption> firstPass() throws MatrixSolvingFailedException {
    Set<Assumption> aset = new HashSet<Assumption>();
    Vector<Integer> discard = new Vector<Integer>();
    logger.log(Level.ALL, "Processing rows:\n",remainingRows);
    for (Integer r : remainingRows) {
      if (FAar01(r)) {
        // If every augmentation entry in row r is of code 0 or 1, then we need not worry about
        // row r at all.
        logger.log(Level.ALL,"Discarding row",r,": all augmentation entries nonnegative constants.");
        discard.add(r);
      }
      else if (EXpr3AU(r)) {
        // If row r has a post-pivot entry that is of code 3 and in an absolutely unblocked column,
        // then again we do not need to worry about row r at all.
        logger.log(Level.ALL,"Discarding row",r,": contains a negative constant in an absolutely unblocked column.");
        discard.add(r);
      }
      else if (FApr01(r)) {
        // Suppose all post-pivot entries are of code 0 or 1.
        logger.log(Level.ALL, "In row",r,", all post-pivot entries are nonnegative constants.");
        if (EXar3(r)) {
          // If in addition there is an aug entry of code 3, then we have a complete fail.
          // We hope it was only because of bad nonzero assumptions during the RREF process,
          // and not that the template is simply unusable.
          logger.log(Level.ALL, "And there is a negative constant in row",r,", so this matrix is unsolvable!");
          throw new MatrixSolvingFailedException(Reason.BadNonzeroAssumptions);
        } else {
          // Else all aug entries are of codes 0, 1, 2, and the only hope for this row is that
          // all entries of code 2 be nonnegative.
          Set<Assumption> nonneg = ar2nonneg(r);
          aset.addAll( nonneg );
          discard.add(r);
          logger.log(Level.ALL, "But there are no augmentation entries in row",r,"that are negative constants.",
              "Therefore we add assumptions that all variable augmentation entries in row",r,
              "be nonnegative:\n",nonneg,"\nand discard row",r,"from further consideration.");
        }
      }
    }
    discardRows(discard);
    return aset;
  }

  /**
   * Return the set of assumptions saying that every aug entry in row r of code 2
   * is nonnegative.
   */
  private Set<Assumption> ar2nonneg(Integer r) {
    Set<Assumption> aset = new HashSet<Assumption>();
    for (int j = aug; j < n; j++) {
      if (codes[r][j] == 2) {
        aset.add( new Assumption(mat.getEntry(r, j), AssumptionType.NONNEGATIVE) );
      }
    }
    return aset;
  }

  //-----------------------------------------------------------------
  // Printing

  public String printCodes() {
    String s = "";
    for (int i = 0; i < m; i++) {
      s += "[ ";
      for (int j = 0; j < n; j++) {
        if (j > 0) {
          s += " ";
        }
        if (j == aug) {
          s += "| ";
        }
        s += Integer.toString(codes[i][j]);
      }
      s += " ]\n";
    }
    return s;
  }

  public String printIntArray(int[] a) {
    String s = "";
    for (int k = 0; k < a.length; k++) {
      s += " "+Integer.toString(a[k]);
    }
    return s;
  }

  @Override
  public String toString() {
    String s = "";
    s += "Codes:\n"+printCodes();
    s += "Remaining rows:\n"+remainingRows.toString()+"\n";
    s += "Absolutely unblocked columns:\n"+AU.toString()+"\n";
    s += "Conditionally unblocked columns:\n"+CU.toString()+"\n";
    return s;
  }

}
