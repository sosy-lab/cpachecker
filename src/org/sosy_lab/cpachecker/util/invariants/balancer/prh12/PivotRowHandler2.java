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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.invariants.Rational;
import org.sosy_lab.cpachecker.util.invariants.balancer.AbstractBalancer;
import org.sosy_lab.cpachecker.util.invariants.balancer.Assumption;
import org.sosy_lab.cpachecker.util.invariants.balancer.Assumption.AssumptionType;
import org.sosy_lab.cpachecker.util.invariants.balancer.AssumptionManager;
import org.sosy_lab.cpachecker.util.invariants.balancer.AssumptionSet;
import org.sosy_lab.cpachecker.util.invariants.balancer.BadAssumptionsException;
import org.sosy_lab.cpachecker.util.invariants.balancer.Matrix;
import org.sosy_lab.cpachecker.util.invariants.balancer.RationalFunction;
import org.sosy_lab.cpachecker.util.invariants.balancer.prh12.ColumnChoiceFrame.ChallengeType;


public class PivotRowHandler2 {

  private final AbstractBalancer mBalancer;
  private final AssumptionManager amgr;
  private final LogManager logger;
  private final Matrix mat;
  private final int m, n, augStart;
  private final Vector<Integer> remainingRows;
  private Vector<Integer> augChallengedRows;
  @SuppressWarnings("unused")
  private final Vector<Integer> pivotRows;
  private final Vector<Integer> availableCols;
  private int[][] codes;
  private List<Integer> AU;
  private OptionManager2 opman;

  //-----------------------------------------------------------------
  // Constructing

  public PivotRowHandler2(Matrix mx, AssumptionManager am, AbstractBalancer mb, LogManager lm) {
    mBalancer = mb;
    amgr = am;
    logger = lm;
    mat = mx;
    m = mx.getRowNum(); // number of rows in the matrix
    n = mx.getColNum(); // number of columns in the matrix, including augmentation columns
    augStart = n - mx.getNumAugCols(); // index of first augmentation column; also equals
                                       // the number of nonaugmentation columns

    // Initialize the "remaining rows" as just those that are pivot rows in the matrix.
    remainingRows = new Vector<>(m);
    for (int i = 0; i < m; i++) {
      if (mat.isPivotRow(i)) {
        remainingRows.add(Integer.valueOf(i));
      }
    }
    // Make a copy, which we will NOT alter as we proceed.
    // This serves to say which rows were pivot rows when we began.
    pivotRows = new Vector<>(remainingRows);

    // Initialize the "available cols" as all the non augmentation columns.
    availableCols = new Vector<>(augStart);
    for (int j = 0; j < augStart; j++) {
      availableCols.add(Integer.valueOf(j));
    }

    writeCodes();
    computeUnblockedColumns();
  }

  /*
   * We represent each entry f of the matrix by a code:
   *  0: f = 0
   *  1: f > 0
   *  2: f is variable
   *  3: f < 0
   * 10: f >= 0
   * 30: f <= 0
   * 31: f <> 0
   * We use the current assumption set in amgr to make the codes as accurate as we can.
   */
  private void writeCodes() {
    codes = new int[m][n];
    AssumptionSet aset = amgr.getCurrentAssumptionSet();
    RationalFunction f;
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        f = mat.getEntry(i, j);
        if (f.isConstant()) {
          // First, if f is constant, then we simply check its sign.
          if (f.isZero()) {
            // f is identically zero
            codes[i][j] = 0;
          } else if (f.isPositive()) {
            // f is a positive constant
            codes[i][j] = 1;
          } else {
            // f is a negative constant
            codes[i][j] = 3;
          }
        } else {
          // If f is not constant, then we attempt to constrain its sign using the
          // current assumption set.
          AssumptionType at = aset.query(f);
          int[] lookUp = {2, 0, 1, 10, 3, 30, 31, 2};
          int b = at.getCode();
          int c = lookUp[b];
          codes[i][j] = c;
        }
      }
    }
    logger.log(Level.ALL, "Wrote code table:","\n"+printCodes());
  }

  private void computeUnblockedColumns() {
    // Compute absolutely unblocked columns.
    // A column is absolutely unblocked if all its codes are 0, 3, and 30.
    Vector<Integer> auv = new Vector<>();
    for (int j = 0; j < augStart; j++) {
      boolean absolute = true;
      for (int i = 0; i < m; i++) {
        int c = codes[i][j];
        if (c != 0 && c != 3 && c != 30) {
          absolute = false;
          break;
        }
      }
      if (absolute) {
        auv.add(Integer.valueOf(j));
      }
    }
    AU = auv;
  }

  // ----------------------------------------------------------------
  // utilities

  private void discardRows(List<Integer> d) {
    remainingRows.removeAll(d);
  }

  private void discardCols(List<Integer> c) {
    availableCols.removeAll(c);
  }

  // ----------------------------------------------------------------
  // First pass

  public void firstPass() throws BadAssumptionsException {
    Vector<Integer> rowDiscard = new Vector<>();
    Vector<Integer> colDiscard = new Vector<>();
    logger.log(Level.ALL, "Processing pivot rows:\n",remainingRows,"\nfor matrix:","\n"+mat.toString());
    for (Integer r : remainingRows) {
      if (FAar0110(r) && FApr0330(r)) {
        // If every augmentation entry in row r is of code 0, 1, or 10,
        // and if every postpivot entry in row r is of code 0, 3, or 30,
        // then row r can be discarded.
        logger.log(Level.ALL, "Discarding row",r,": all augmentation entries nonnegative,",
            "and all postpivots nonpositive.");
        rowDiscard.add(r);
      } else if (EXpr3AU(r)) {
        // If row r has a post-pivot entry that is of code 3 and in an absolutely unblocked column,
        // then again we do not need to worry about row r at all.
        logger.log(Level.ALL, "Discarding row",r,": contains a negative constant in an absolutely unblocked column.");
        rowDiscard.add(r);
      } else if (FApr0110(r)) {
        // Suppose all post-pivot entries are of code 0, 1, or 10.
        if (EXar3(r)) {
          // If in addition there is an aug entry of code 3, then we have a complete fail.
          // We hope it was only because of bad nonzero assumptions during the RREF process,
          // and not that the template is simply unusable.
          logger.log(Level.ALL, "Matrix unsolvable! Row",r,
              "has a negative augmentation entry, but all post-pivot entries nonnegative.");
          throw new BadAssumptionsException();
        } else {
          // Else this row is not an immediate fail, but the only hope for it is that
          // all aug entries be nonnegative, and the row have no positive postpivots.
          // We add the corresponding assumptions, as necessary ones. Namely,
          // that all aug entries be nonnegative.
          // We also discard this row, and discard all postpivot columns that have a 1 in this row.
          // FIXME: This case is still not handled properly.
          // Consider the columns in which this row has entry 10. For these columns, we
          // have a choice: either we need to eliminate them from consideration altogether,
          // or we need to assume that the 10 entry is actually 0.
          AssumptionSet nonneg = arnonneg(r);
          Set<Integer> poscols = prposcols(r);
          rowDiscard.add(r);
          colDiscard.addAll(poscols);
          logger.log(Level.ALL, "Discarding row",r,": all post-pivots are nonnegative.",
              "We assume all augmentation entries are nonnegative:\n",nonneg);
          if (poscols.size() > 0) {
            logger.log(Level.ALL, "We also discard all postpivot columns having positive entry in this row:\n",
                poscols);
          }
          amgr.addNecessaryAssumptions(nonneg);
          // Now update the code table.
          writeCodes();
        }
      }
    }
    // Discard the rows and columns that have been eliminated.
    discardRows(rowDiscard);
    discardCols(colDiscard);
    // If there are any rows left...
    if (remainingRows.size() > 0) {
      // ...say what they are...
      logger.log(Level.ALL, "The rows still remaining to be processed are:\n",remainingRows);
      // ...and determine which ones are challenged by their augmentation columns.
      // (while the rest are only potentially challenged, by a potentially positive
      //  post pivot entry).
      augChallengedRows = new Vector<>();
      for (Integer i : remainingRows) {
        if (!FAar0110(i)) {
          augChallengedRows.add(i);
        }
      }
    } else {
      // else, just set augChallenged rows to be empty.
      augChallengedRows = new Vector<>();
    }
  }

  /*
   * Check whether all augmentation entries in row r are of code 0, 1, 10.
   */
  private boolean FAar0110(Integer r) {
    boolean ans = true;
    for (int j = augStart; j < n; j++) {
      int c = codes[r][j];
      if (c >= 2 && c != 10) {
        ans = false; break;
      }
    }
    return ans;
  }

  /*
   * Check whether all post-pivots in row r are of code 0, 3, 30.
   */
  private boolean FApr0330(Integer r) {
    boolean ans = true;
    boolean postpivot = false;
    for (int j = 0; j < augStart; j++) {
      int c = codes[r][j];
      if (!postpivot && c == 1) {
        postpivot = true;
        continue;
      } else if (postpivot) {
        if (c != 0 && c != 3 && c != 30) {
          ans = false; break;
        }
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
    for (int j = 0; j < augStart; j++) {
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
    for (int j = augStart; j < n; j++) {
      if (codes[r][j] == 3) {
        ans = true; break;
      }
    }
    return ans;
  }

  /*
   * Check whether all post-pivots in row r are of code 0, 1, 10.
   */
  private boolean FApr0110(Integer r) {
    boolean ans = true;
    for (int j = 0; j < augStart; j++) {
      int c = codes[r][j];
      if (c >= 2 && c != 10) {
        ans = false; break;
      }
    }
    return ans;
  }

  /*
   * Return the set of postpivot columns that have code 1 in row r.
   */
  Set<Integer> prposcols(Integer r) {
    Set<Integer> pc = new HashSet<>();
    boolean postpivot = false;
    for (int j = 0; j < augStart; j++) {
      int c = codes[r][j];
      if (c == 1) {
        if (postpivot) {
          // Then this is a 1 in a postpivot column.
          pc.add(j);
        } else {
          // Then this is the pivot column.
          postpivot = true;
        }
      }
    }
    return pc;
  }

  /*
   * Return the set of assumptions saying that every aug entry in row r of
   * code 2, 30, or 31 be nonnegative.
   */
  AssumptionSet arnonneg(Integer r) {
    AssumptionSet aset = new AssumptionSet();
    for (int j = augStart; j < n; j++) {
      int c = codes[r][j];
      RationalFunction f = mat.getEntry(r, j);
      if (c == 2) {
        aset.add(new Assumption(f, AssumptionType.NONNEGATIVE));
      } else if (c == 30) {
        aset.add(new Assumption(f, AssumptionType.ZERO));
      } else if (c == 31) {
        aset.add(new Assumption(f, AssumptionType.POSITIVE));
      }
    }
    return aset;
  }

  // ----------------------------------------------------------------
  // Second Pass

  public void secondPass() throws BadAssumptionsException {
    // First check whether there are any aug col challenged rows.
    // If not, then there is nothing left to be done.
    if (augChallengedRows == null || augChallengedRows.size() == 0) {
      return;
    }

    // Build the option manager.
    buildOptionManager();

    // Assign heights to the remaining rows and columns.
    opman.assignHeights();

    // Check whether there were any rows that didn't get a height.
    // Any such rows must be involved in a cyclic dependency, so we
    // will not be able to generate a set of sufficient assumptions.
    // It may be possible to find an algorithm for this part of the
    // problem, but we don't have one yet.
    List<PivotRow2> noheight = opman.getRowsLackingHeights();
    if (noheight.size() > 0) {
      throw new BadAssumptionsException();
    }

    // Start a column choice stack.
    ColumnChoiceFrame ccf0 = opman.buildChoiceFrame(augChallengedRows, ChallengeType.AUGCOLUMN);
    Stack<ColumnChoiceFrame> cstack = new Stack<>();
    cstack.push(ccf0);

    // Now ask the stack for assumption sets, until one works or we run out.
    // Prepare a boolean to say how we did.
    boolean successful = false;
    while (true) {
      if (cstack.empty()) {
        // In this case, we ran out of options without finding a sufficient set of assumptions.
        // So it is time to backtrack.
        throw new BadAssumptionsException();
      }
      // Otherwise, investigate the top frame.
      ColumnChoiceFrame ccf = cstack.peek();
      // Is it a complete frame?
      if (!ccf.isComplete()) {
        // If not, then check whether it has a next choice.
        if (ccf.hasNext()) {
          // If it does, then create the next frame and push it onto the stack.
          ColumnChoiceFrame ccf1 = ccf.next();
          cstack.push(ccf1);
        } else {
          // If it does not have a next choice, then pop it off the stack.
          cstack.pop();
        }
      } else {
        // If the top frame is complete, then get its assumption set.
        AssumptionSet aset = ccf.getAssumptionSet();
        // Get a copy of the current assumption set.
        AssumptionSet curr = new AssumptionSet(amgr.getCurrentAssumptionSet());
        // Add ccf's set.
        boolean consistent = curr.addAll(aset);
        // Is the new set obviously inconsistent?
        if (!consistent) {
          // Then just pop the current frame off the stack.
          cstack.pop();
        } else {
          // If it is at least not obviously inconsistent, then ask Redlog whether it is satisfiable.
          Map<String, Rational> map = mBalancer.tryAssumptionSet(curr);
          if (map != null || mBalancer.redlogSaidTrue()) {
            // Success!
            logger.log(Level.ALL, "The set is satisfiable!");
            successful = true;
            amgr.setCurrentAssumptionSet(curr);
            amgr.zeroSubsCurrent(aset);
            break;
          }
        }
      }
    }

    if (!successful) {
      // There was no way to satisfy the pivot rows and get parameter values, so we have to backtrack.
      logger.log(Level.ALL, "There was no way to satisfy the remaining rows.");
      throw new BadAssumptionsException();
    }
    // If we were successful, then we just return. The successful assumption set is in amgr.
  }


  private void buildOptionManager() {
    // Initialize the option manager.
    opman = new OptionManager2(this, logger);
    // Add the usable columns.
    for (Integer c : availableCols) {
      UsableColumn u = new UsableColumn(mat, c, logger);
      opman.addUsableColumn(u, c);
    }
    // Create the augmentation column.
    AugmentationColumn ac = new AugmentationColumn();
    for (Integer r : remainingRows) {
      if (!EXar3(r)) {
        // In this case row r has no 3 codes in its augmentation columns.
        // This means that it does have an aug col option.
        AssumptionSet aco = arnonneg(r);
        ac.addSet(r, aco);
      }
    }
    // Add it to the option manager as column number -1.
    opman.addUsableColumn(ac, -1);

    // Create the pivot rows, and add them to the option manager.
    // Tell each one which are its usable columns.
    for (Integer r : remainingRows) {
      // Create and add.
      PivotRow2 pr = new PivotRow2(r, logger);
      opman.addPivotRow(pr);
      // Assign usable columns.
      for (Integer c : availableCols) {
        int a = codes[r][c];
        if (2 <= a && a != 10) {
          UsableColumn u = opman.getUsableColumn(c);
          pr.addFreeColumn(u);
        }
      }
      // Add the aug col, if it is usable by this row.
      if (ac.rowHasAugColOption(r)) {
        pr.addAugColumn(ac);
      }
    }
  }

  /*
   * Return the list of all rows that have a 1 in the column named.
   */
  public List<Integer> getRowsWith1sInCol(int col) {
    List<Integer> ones = new Vector<>();
    for (int i = 0; i < m; i++) {
      if (codes[i][col] == 1) {
        ones.add(i);
      }
    }
    return ones;
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
        if (j == augStart) {
          s += "| ";
        }
        String t = Integer.toString(codes[i][j]);
        if (t.length() == 1) {
          s += " ";
        }
        s += t;
      }
      s += " ]\n";
    }
    return s;
  }

  @Override
  public String toString() {
    String s = "";
    s += "Codes:\n"+printCodes();
    s += "Remaining rows:\n"+remainingRows.toString()+"\n";
    s += "Available columns:\n"+availableCols.toString()+"\n";
    s += "Absolutely unblocked columns:\n"+AU.toString()+"\n";
    return s;
  }

}
