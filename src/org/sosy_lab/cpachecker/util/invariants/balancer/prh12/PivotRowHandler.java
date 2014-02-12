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
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.util.invariants.Rational;
import org.sosy_lab.cpachecker.util.invariants.balancer.AbstractBalancer;
import org.sosy_lab.cpachecker.util.invariants.balancer.Assumption;
import org.sosy_lab.cpachecker.util.invariants.balancer.Assumption.AssumptionType;
import org.sosy_lab.cpachecker.util.invariants.balancer.AssumptionManager;
import org.sosy_lab.cpachecker.util.invariants.balancer.AssumptionSet;
import org.sosy_lab.cpachecker.util.invariants.balancer.BadAssumptionsException;
import org.sosy_lab.cpachecker.util.invariants.balancer.Matrix;
import org.sosy_lab.cpachecker.util.invariants.balancer.MatrixSolvingFailedException;
import org.sosy_lab.cpachecker.util.invariants.balancer.MatrixSolvingFailedException.Reason;
import org.sosy_lab.cpachecker.util.invariants.balancer.RationalFunction;


public class PivotRowHandler {

  private final LogManager logger;
  private final Matrix mat;
  private final int m, n, augStart;
  private final Vector<Integer> remainingRows;
  @SuppressWarnings("unused")
  private final Vector<Integer> pivotRows;
  private final int[][] codes;
  private List<Integer> AU, CU;
  private OptionManager opman;

  //-----------------------------------------------------------------
  // Constructing

  public PivotRowHandler(Matrix mx, LogManager lm) {
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

    codes = buildCodes();
    computeUnblockedColumns();
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

  private void computeUnblockedColumns() {
    // Compute absolutely and conditionally unblocked columns.
    // A column is absolutely unblocked if it contains only 0's and 3's.
    // A column is conditionally unblocked if it contains only 0's, 2's, and 3's.
    Vector<Integer> auv = new Vector<>();
    Vector<Integer> cuv = new Vector<>();
    for (int j = 0; j < augStart; j++) {
      boolean absolute = true;
      boolean conditional = true;
      for (int i = 0; i < m; i++) {
        if (codes[i][j] == 1) {
          absolute = false;
          conditional = false;
          break;
        } else if (codes[i][j] == 2) {
          absolute = false;
        }
      }
      if (absolute) {
        auv.add(Integer.valueOf(j));
      } else if (conditional) {
        cuv.add(Integer.valueOf(j));
      }
    }
    AU = auv;
    CU = cuv;
  }
  /*
  private List<List<Integer>> computeUnblockedColumns() {
    // Compute absolutely and conditionally unblocked columns.
    Vector<Integer> auv = new Vector<>();
    Vector<Integer> cuv = new Vector<>();
    for (int j = 0; j < augStart; j++) {
      boolean absolute = true;
      boolean conditional = true;
      for (int i = 0; i < m; i++) {
        if (codes[i][j] == 1) {
          absolute = false;
          conditional = false;
          break;
        } else if (codes[i][j] == 2) {
          absolute = false;
        }
      }
      if (absolute) {
        auv.add(Integer.valueOf(j));
      } else if (conditional) {
        cuv.add(Integer.valueOf(j));
      }
    }
    // Return.
    List<List<Integer>> u = new Vector<>();
    u.add(auv);
    u.add(cuv);
    return u;
  }
  */

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
        } else if (f.isZero()) {
          // f is identically zero
          codes[i][j] = 0;
        } else if (f.isPositive()) {
          // f is a positive constant
          codes[i][j] = 1;
        } else {
          // f is a negative constant
          codes[i][j] = 3;
        }
      }
    }
    return codes;
  }

  //-----------------------------------------------------------------
  // Solving

  public Set<AssumptionSet> handlePivotRows() throws MatrixSolvingFailedException {
    // Initialize subset of assumptions that will be used in all sets.
    AssumptionSet base = new AssumptionSet();
    // Make the first pass:
    base.addAll(firstPass());
    // Second pass, if needed:
    Set<AssumptionSet> asetset;
    if (remainingRows.size() > 0) {
      Set<AssumptionSet> secondPassOptions = secondPass();
      for (AssumptionSet option : secondPassOptions) {
        option.addAll(base);
      }
      asetset = secondPassOptions;
    } else {
      asetset = new HashSet<>();
      asetset.add(base);
    }
    return asetset;
  }

  private void discardRows(List<Integer> d) {
    remainingRows.removeAll(d);
  }

  /*
   * Check whether all augmentation entries in row r are of code 0 or 1.
   */
  private boolean FAar01(Integer r) {
    boolean ans = true;
    for (int j = augStart; j < n; j++) {
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
   * Check whether all post-pivots in row r are of code 0 or 1.
   */
  private boolean FApr01(Integer r) {
    boolean ans = true;
    for (int j = 0; j < augStart; j++) {
      if (codes[r][j] >= 2) {
        ans = false; break;
      }
    }
    return ans;
  }

  /*
   * Check whether all post-pivots in row r are of code 0 or 3.
   */
  private boolean FApr03(Integer r) {
    boolean ans = true;
    for (int j = 0; j < augStart; j++) {
      if (codes[r][j] == 1 || codes[r][j] == 2) {
        ans = false; break;
      }
    }
    return ans;
  }

  private AssumptionSet firstPass() throws MatrixSolvingFailedException {
    AssumptionSet aset = new AssumptionSet();
    Vector<Integer> discard = new Vector<>();
    logger.log(Level.ALL, "Processing pivot rows:\n",remainingRows);
    for (Integer r : remainingRows) {
      if (FAar01(r)) {
        // If every augmentation entry in row r is of code 0 or 1, then we need not worry about
        // row r at all.
        logger.log(Level.ALL, "Discarding row",r,": all augmentation entries nonnegative constants.");
        discard.add(r);
      } else if (EXpr3AU(r)) {
        // If row r has a post-pivot entry that is of code 3 and in an absolutely unblocked column,
        // then again we do not need to worry about row r at all.
        logger.log(Level.ALL, "Discarding row",r,": contains a negative constant in an absolutely unblocked column.");
        discard.add(r);
      } else if (FApr01(r)) {
        // Suppose all post-pivot entries are of code 0 or 1.
        if (EXar3(r)) {
          // If in addition there is an aug entry of code 3, then we have a complete fail.
          // We hope it was only because of bad nonzero assumptions during the RREF process,
          // and not that the template is simply unusable.
          logger.log(Level.ALL, "Matrix unsolvable! Row",r,
              "has a negative constant augmentation entry, but all post-pivot entries nonnegative constants.");
          throw new MatrixSolvingFailedException(Reason.BadNonzeroAssumptions);
        } else {
          // Else all aug entries are of codes 0, 1, 2, and the only hope for this row is that
          // all entries of code 2 be nonnegative.
          AssumptionSet nonneg = ar2nonneg(r);
          aset.addAll(nonneg);
          discard.add(r);
          logger.log(Level.ALL, "Discarding row",r,", and adding assumptions:",
              "all post-pivot entries are nonnegative constants, but no augmentation entries are negative",
              "constants. Therefore we add assumptions that all variable augmentation entries in row",
              r, "be nonnegative. Assumptions added:","\n"+nonneg.toString());
        }
      }
    }
    discardRows(discard);
    return aset;
  }

  public void firstPass(AssumptionManager amgr) throws BadAssumptionsException {
    Vector<Integer> discard = new Vector<>();
    logger.log(Level.ALL, "Processing pivot rows:\n",remainingRows,"\nfor matrix:","\n"+mat.toString());
    for (Integer r : remainingRows) {
      if (FAar01(r) && FApr03(r)) {
        // If every augmentation entry in row r is of code 0 or 1, then row r will be satisfied
        // if and only if it has no positive entries in a column that some row wants to use.
        // So if there are any 1's or 2's among the postpivots, then we might still need to use one of
        // them during the third pass. So we rule this row out right now only if in addition there
        // are no 1's or 2's among the postpivots.
        logger.log(Level.ALL, "Discarding row",r,": all augmentation entries nonnegative constants,",
            "and all postpivots are nonpositive constants.");
        discard.add(r);
      } else if (EXpr3AU(r)) {
        // If row r has a post-pivot entry that is of code 3 and in an absolutely unblocked column,
        // then again we do not need to worry about row r at all.
        logger.log(Level.ALL, "Discarding row",r,": contains a negative constant in an absolutely unblocked column.");
        discard.add(r);
      } else if (FApr01(r)) {
        // Suppose all post-pivot entries are of code 0 or 1.
        if (EXar3(r)) {
          // If in addition there is an aug entry of code 3, then we have a complete fail.
          // We hope it was only because of bad nonzero assumptions during the RREF process,
          // and not that the template is simply unusable.
          logger.log(Level.ALL, "Matrix unsolvable! Row",r,
              "has a negative constant augmentation entry, but all post-pivot entries nonnegative constants.");
          throw new BadAssumptionsException();
        }
        /*
         * Actually we need to keep this kind of row (below), in case another pivot row tries to use
         * a column that has a 1 in this row.
         *
        else {
          // Else all aug entries are of codes 0, 1, 2, and the only hope for this row is that
          // all entries of code 2 be nonnegative.
          AssumptionSet nonneg = ar2nonneg(r);
          discard.add(r);
          logger.log(Level.ALL, "Discarding row",r,", and adding assumptions:",
              "all post-pivot entries are nonnegative constants, but no augmentation entries are negative",
              "constants. Therefore we add assumptions that all variable augmentation entries in row",
              r, "be nonnegative. Assumptions added:","\n"+nonneg.toString());
          amgr.addNecessaryAssumptions(nonneg);
        }
        */
      }
    }
    discardRows(discard);
    if (remainingRows.size() > 0) {
      logger.log(Level.ALL, "The rows still remaining to be processed are:\n",remainingRows);
    }
  }

  /**
   * Return the set of assumptions saying that every aug entry in row r of code 2
   * is nonnegative.
   */
  AssumptionSet ar2nonneg(Integer r) {
    AssumptionSet aset = new AssumptionSet();
    for (int j = augStart; j < n; j++) {
      if (codes[r][j] == 2) {
        aset.add(new Assumption(mat.getEntry(r, j), AssumptionType.NONNEGATIVE));
      }
    }
    return aset;
  }

  //-----------------------------------------------------------------
  // Second pass

  private Set<AssumptionSet> secondPass() {
    logger.log(Level.ALL, "Second pass: processing remaining rows:",remainingRows);
    Set<AssumptionSet> asetset = new HashSet<>();
    // Build option table.
    OptionTable optionTable = buildOptionTable();
    logger.log(Level.ALL, "Built option table:","\n"+optionTable.toString());
    // First step: for those rows that only have a single option, take those "options".
    AssumptionSet soleOpAset = optionTable.takeSoleOptions();
    // Next ... TODO
    if (optionTable.getRemainingRows().size() > 0) {
      logger.log(Level.FINEST, "Not all rows satisfied on second pass! Unsatisfied rows:",remainingRows);
    }
    // For now we return just the soleOpAset, to see test our progress.
    asetset.add(soleOpAset);
    //
    return asetset;
  }

  public void secondPass(AssumptionManager amgr) throws BadAssumptionsException {
    // On this second pass we build the OptionManager, populate it with PivotRows, and
    // retrieve all those conditions corresponding to pivot rows that have just a single option.
    // We store the OptionManager, so that it can be retrieved and used later, by the thirdPass.

    // Build the option manager.
    buildOptionManager();

    // If there are any rows that have just a sole option, add the corresponding assumptions
    // as necessary assumptions.
    AssumptionSet aset = opman.getSoleOptionRowsAssumptions();
    if (aset.size() > 0) {
      logger.log(Level.ALL, "Some of the remaining rows have just a single option.",
          "Taking those options yields the new assumptions:\n",aset);
      amgr.addNecessaryAssumptions(aset);
    } else {
      logger.log(Level.ALL, "There were no remaining pivot rows having just a single option.");
    }
  }

  public void thirdPass(AssumptionManager amgr, AbstractBalancer balancer) throws BadAssumptionsException {
    if (opman == null || opman.numRemainingRows() == 0) {
      return;
    }
    // Otherwise we have an opman (it should have been initialized during the second pass)
    // and it has at least one row left.

    boolean successful = false;

    AssumptionSet aset = opman.nextTry();
    while (aset != null) {
      logger.log(Level.ALL, "Trying to satisfy remaining pivot rows with set:\n",aset);
      // Get a copy of the current assumption set.
      AssumptionSet curr = new AssumptionSet(amgr.getCurrentAssumptionSet());
      logger.log(Level.ALL, "Current set is:\n",curr);
      boolean consistent = curr.addAll(aset);
      logger.log(Level.ALL, "Combined set is:\n",curr);
      // Is the next try from opman consistent with the current assumption set?
      if (!consistent) {
        // If not, go to the next try.
        logger.log(Level.ALL, "This set was inconsistent.");
      } else {
        // It is consistent. So now we ask Redlog whether it can find parameter values.
        logger.log(Level.ALL, "The set is at least not immediately contradictory.",
            "We ask Redlog if it is satisfiable.");
        Map<String, Rational> map = balancer.tryAssumptionSet(curr);
        if (map != null || balancer.redlogSaidTrue()) {
          // Success!
          logger.log(Level.ALL, "The set is satisfiable!");
          successful = true;
          // FIXME: Instead of the next line, we should be adding these assumptions as
          // possibly unnecessary. We cannot yet do that, because AssumptionManager is not
          // prepared to store a stack frame the popping of which will return us precisely
          // to the point we are at right now!
          amgr.setCurrentAssumptionSet(curr);
          amgr.zeroSubsCurrent(aset);
          break;
        }
      }
      aset = opman.nextTry();
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
    opman = new OptionManager(logger);
    // Add the usable columns.
    for (Integer c : CU) {
      UsableColumn u = new UsableColumn(mat, c, logger);
      opman.addUsableColumn(u, c);
    }
    // Create the augmentation column.
    AugmentationColumn ac = new AugmentationColumn();
    for (Integer r : remainingRows) {
      if (!EXar3(r)) {
        // In this case row r has no 3 codes in its augmentation columns.
        // This means that it does have an aug col option.
        AssumptionSet aco = ar2nonneg(r);
        ac.addSet(r, aco);
      }
    }
    // Add it to the option manager as column number -1.
    opman.addUsableColumn(ac, -1);

    // Create the pivot rows, and add them to the option manager.
    // Tell each one which are its usable columns.
    for (Integer r : remainingRows) {
      // Create and add.
      PivotRow pr = new PivotRow(r, logger);
      opman.addPivotRow(pr);
      // Assign usable columns.
      for (Integer c : CU) {
        int a = codes[r][c];
        if (2 <= a && a <= 3) {
          UsableColumn u = opman.getUsableColumn(c);
          pr.addUsableColumn(u);
        }
      }
      // Add the aug col, if it is usable by this row.
      if (ac.rowHasAugColOption(r)) {
        pr.addUsableColumn(ac);
      }
    }
  }

  private OptionTable buildOptionTable() {
    int[][] table = new int[m][augStart+1];
    // Entries not corresponding to CU columns in remaining rows will not be initialized.
    for (Integer i : remainingRows) {
      // First get the codes from the conditionally unblocked columns.
      for (Integer j : CU) {
        table[i][j] = codes[i][j];
      }
      // Now assess the augmentation entries.
      table[i][augStart] = (EXar3(i) ? 5 : 4);
    }
    return new OptionTable(this, mat, table, remainingRows, CU, logger);
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
