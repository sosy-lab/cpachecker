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
package org.sosy_lab.cpachecker.util.invariants.balancer;

import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.util.invariants.Rational;
import org.sosy_lab.cpachecker.util.invariants.balancer.Assumption.AssumptionRelation;
import org.sosy_lab.cpachecker.util.invariants.balancer.Assumption.AssumptionType;
import org.sosy_lab.cpachecker.util.invariants.balancer.interfaces.MatrixI;

public class Matrix implements MatrixI {

  private int rowNum;
  private int colNum;
  private RationalFunction[][] entry;
  private int numAugCols = 0;

  private List<Integer> pivotRows = null;
  private Matrix elemMatProd = null;

  // For control of step-by-step RREF:
  private int nextI0 = 0, nextJ0 = 0;

  // This controls the putInRREF method, causing it to stop as soon as it is forced
  // to choose a pivot with variable numerator, when set to true.
  private boolean haltOnVariableNumPivot = false;

  // This is set to true only when we have exited the putInRREF method by running out of pivots.
  private boolean outOfPivots = false;

  // Configuration:
  private boolean verbose = false;
  private boolean useFreePivoting = true;

  public Matrix() {}

  /*
   * Basic constructor.
   */
  public Matrix(int m, int n) {
    rowNum = m;
    colNum = n;
    entry = new RationalFunction[m][n];
    zeroFill();
  }

  /*
   * Construct a single column.
   */
  public Matrix(List<RationalFunction> rfs) {
    int m = rfs.size();
    int n = 1;
    rowNum = m;
    colNum = n;
    entry = new RationalFunction[m][n];
    for (int i = 0; i < m; i++) {
      entry[i][0] = rfs.get(i);
    }
  }

  /*
   * Construct from a 2D array.
   */
  public Matrix(RationalFunction[][] a) {
    int m = a.length;
    if (m == 0) {
      return;
    }
    int n = a[0].length;
    rowNum = m; colNum = n;
    entry = a;
  }

  public void setAugStart(int a) {
    numAugCols = colNum - a;
  }

  /*
   * Return a copy of this Matrix.
   */
  public Matrix copy() {
    RationalFunction[][] a = new RationalFunction[rowNum][colNum];
    for (int i = 0; i < rowNum; i++) {
      for (int j = 0; j < colNum; j++) {
        a[i][j] = entry[i][j].copy();
      }
    }
    Matrix m = new Matrix(a);
    m.numAugCols = numAugCols;
    m.haltOnVariableNumPivot = haltOnVariableNumPivot;
    m.nextI0 = nextI0;
    m.nextJ0 = nextJ0;
    m.useFreePivoting = useFreePivoting;
    m.outOfPivots = outOfPivots;
    m.verbose = verbose;
    // Copy pivot rows:
    m.pivotRows = new Vector<>(pivotRows.size());
    for (Integer i : pivotRows) {
      m.pivotRows.add(Integer.valueOf(i.intValue()));
    }
    //
    return m;
  }

  public RationalFunction[][] getEntries() {
    return entry;
  }

  public int getNumPivotRows() {
    return pivotRows.size();
  }

  public void setHaltOnVarNumPivot(boolean b) {
    haltOnVariableNumPivot = b;
  }

  public boolean isOutOfPivots() {
    return outOfPivots;
  }

  public static Matrix makeIdentity(int n) {
    Matrix id = new Matrix(n, n);
    for (int i = 0; i < n; i++) {
      id.entry[i][i] = RationalFunction.makeUnity();
    }
    return id;
  }

  public void setVerbosity(boolean b) {
    verbose = b;
  }

  public int getRowNum() {
    return rowNum;
  }

  public int getColNum() {
    return colNum;
  }

  public int getNumAugCols() {
    return numAugCols;
  }

  public boolean isPivotRow(int i) {
    return pivotRows != null && pivotRows.contains(i);
  }

  public RationalFunction getEntry(int i, int j) {
    return entry[i][j];
  }

  public void setEntry(int i, int j, RationalFunction f) {
    entry[i][j] = f;
  }

  @Override
  public Matrix getElemMatProd() {
    return elemMatProd;
  }

  /*
   * Concatenate two matrices. We presume that neither has any augmentation columns.
   */
  public static Matrix concat(Matrix a, Matrix b) {
    int m = a.rowNum;
    if (m != b.rowNum) {
      System.err.println("Tried to concatenate matrices with different numbers of rows.");
      return null;
    }
    int an = a.colNum;
    int bn = b.colNum;
    Matrix c = new Matrix(m, an + bn);
    // copy entries
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < an; j++) {
        c.entry[i][j] = a.entry[i][j];
      }
      for (int j = 0; j < bn; j++) {
        c.entry[i][an+j] = b.entry[i][j];
      }
    }
    return c;
  }

  @Override
  public Matrix concat(MatrixI b) {
    Matrix m = (Matrix)b;
    return Matrix.concat(this, m);
  }

  /*
   * We create a matrix in which the ordinary columns come from a, and the
   * augmentation columns come from b.
   */
  public static Matrix augment(Matrix a, Matrix b) {
    Matrix c = concat(a, b);
    int bn = b.colNum;
    c.numAugCols = bn;
    return c;
  }

  @Override
  public Matrix augment(MatrixI b) {
    Matrix m = (Matrix)b;
    return Matrix.augment(this, m);
  }

  /*
   * Fill this matrix will zeros.
   */
  @Override
  public void zeroFill() {
    for (int i = 0; i < rowNum; i++) {
      for (int j = 0; j < colNum; j++) {
        entry[i][j] = RationalFunction.makeZero();
      }
    }
  }

  @Override
  public RationalFunction get(int i, int j) {
    return entry[i][j];
  }

  @Override
  public void set(int i, int j, RationalFunction f) {
    entry[i][j] = f;
  }

  /*
   * Swap rows i1 and i2.
   */
  @Override
  public void swapRows(int i1, int i2) {
    RationalFunction temp;
    for (int j = 0; j < colNum; j++) {
      temp = entry[i1][j];
      entry[i1][j] = entry[i2][j];
      entry[i2][j] = temp;
    }
  }

  /*
   * Swap columns j1 and j2.
   */
  public void swapCols(int j1, int j2) {
    RationalFunction temp;
    for (int i = 0; i < rowNum; i++) {
      temp = entry[i][j1];
      entry[i][j1] = entry[i][j2];
      entry[i][j2] = temp;
    }
  }

  /*
   * Multiply row i by RationalFunction f.
   */
  @Override
  public void multRow(int i, RationalFunction f) {
    for (int j = 0; j < colNum; j++) {
      entry[i][j] = RationalFunction.multiply(entry[i][j], f);
    }
  }

  /*
   * Add row i2 times RationalFunction f to row i1.
   */
  @Override
  public void addMultiple(int i1, int i2, RationalFunction f) {
    for (int j = 0; j < colNum; j++) {
      RationalFunction product = RationalFunction.multiply(f, entry[i2][j]);
      RationalFunction sum = RationalFunction.add(entry[i1][j], product);
      entry[i1][j] = sum;
    }
  }

  /*
   * Turn a List<Integer> into an int[].
   */
  private int[] makeIntArray(List<Integer> ilist) {
    int[] a = new int[ilist.size()];
    int k = 0;
    for (Integer i : ilist) {
      a[k] = i.intValue();
      k++;
    }
    return a;
  }

  /*
   * Say whether row i has all entries zero in nonaug columns.
   */
  private boolean isAlmostZeroRow(int i) {
    boolean ans = true;
    for (int j = 0; j < colNum - numAugCols; j++) {
      if (!entry[i][j].isZero()) {
        ans = false;
        break;
      }
    }
    return ans;
  }

  /*
   * An "almost zero row" is one in which every nonaug entry is zero.
   * For such a row, the "terminals" are the entries in the aug columns.
   * We write the assumptions that the almost zero row terminals be zero
   */
  @Override
  public AssumptionSet getAlmostZeroRowAssumptions() {
    AssumptionSet aset = new AssumptionSet();
    for (int i = 0; i < rowNum; i++) {
      if (isAlmostZeroRow(i)) {
        // In this case, row i has all entries zero in its non-augmentation columns.
        // We now therefore consider the aug cols, one at a time.
        // Each one having an entry not identically zero in row i results in an Assumption.
        for (int j = 0; j < numAugCols; j++) {
          RationalFunction f = entry[i][colNum - numAugCols + j];
          if (!f.isZero()) {
            aset.add(new Assumption(f, AssumptionType.ZERO ));
          }
        }
      }
    }
    return aset;
  }

  /*
   * Return the set of assumptions that all denominators in this matrix are nonzero.
   * We ignore denominators which are nonzero constants, and if we encounter a
   * denominator that is identically zero then we return a singleton set containing only
   * the assumption that zero is nonzero. (This might be useful for deriving a contradiction.)
   */
  @Override
  public AssumptionSet getDenomNonZeroAssumptions() {
    AssumptionSet aset = new AssumptionSet();
    outerloop:
    for (int i = 0; i < rowNum; i++) {
      for (int j = 0; j < colNum; j++) {
        Polynomial denom = entry[i][j].getDenominator();
        if (denom.isConstant()) {
          if (denom.isZero()) {
            aset = new AssumptionSet();
            aset.add(new Assumption(RationalFunction.makeZero(), AssumptionType.NONZERO));
            break outerloop;
          } else {
            // If the denom is a nonzero constant then we add nothing.
          }
        } else {
          aset.add(new Assumption(denom, AssumptionType.NONZERO));
        }
      }
    }
    // If the set is still empty, this is because all we had were nonzero constants for denominators.
    // For now we'll return this empty set. Alternatively, we might want a singleton containing
    // "true".
    return aset;
  }

  /*
   * Put this matrix into reduced row-echelon form, using Gaussian elimination.
   * Returns the set of nonzero assumptions made each time we divided a pivot row by its lead entry.
   */
  @Override
  public AssumptionSet putInRREF() {
    LogManager lm = null;
    return putInRREF(lm);
  }

  /*
   * Version of RREF algorithm that takes a logger.
   * Returns the set of nonzero assumptions made each time we divided a pivot row by its lead entry.
   */
  @Override
  public AssumptionSet putInRREF(LogManager logger) {
    int m = rowNum;
    int n = colNum;
    int i0 = nextI0;
    int j0 = nextJ0;
    pivotRows = new Vector<>();

    AssumptionSet aset = new AssumptionSet();

    int M = m;
    int N = n;
    // Augmented columns cannot have pivots.
    N -= numAugCols;

    // We maintain E as the product of all elementary matrices we would have multiplied by,
    // in order to achieve the row operations that we have done.
    Matrix E = Matrix.makeIdentity(M);

    while (true) {
      // Look for the next pivot.
      int[] pivot;
      if (useFreePivoting) {
        pivot = getNextPivotFree(i0, M, j0, N);
      } else {
        pivot = getNextPivot(i0, M, j0, N);
      }
      // If there isn't any, then we're done.
      if (pivot[0] == -1) {
        // This records explicitly the fact that we exited the method by running out of pivots.
        outOfPivots = true;
        // We also record the next i0 and j0.
        nextI0 = i0;
        nextJ0 = j0;
        break;
      }
      // Let the pivot be i1,j1.
      int i1 = pivot[0];
      int j1 = pivot[1];

      // If the pivot function has variable numerator, and haltOnVariableNumPivot is set to true,
      // then store the next i0 and j0, and return the current assumption set.
      if (haltOnVariableNumPivot && !entry[i1][j1].getNumerator().isConstant()) {
        nextI0 = i0;
        nextJ0 = j0;
        return aset;
      }

      // If i1 > i0, then swap these rows, and set i1 = i0.
      if (i1 > i0) {
        swapRows(i1, i0);
        // and do the same to E
        E.swapRows(i1, i0);
        // log it
        if (verbose && logger != null) {
          logger.log(Level.ALL, "Swapped rows",i1,"and",i0);
          logger.log(Level.ALL, "Matrix:","\n"+this.toString());
          logger.log(Level.ALL, "E:","\n"+E.toString());
        }
        // set i1 = i0.
        i1 = i0;
      }

      // Similarly, for the column.
      // If j1 > j0, then swap these rows, and set j1 = j0.
      if (j1 > j0) {
        swapCols(j1, j0);
        // (Can't do the same to E, since this is not a row operation!)
        // log it
        if (verbose && logger != null) {
          logger.log(Level.ALL, "Swapped columns",j1,"and",j0);
          logger.log(Level.ALL, "Matrix:","\n"+this.toString());
        }
        // set j1 = j0.
        j1 = j0;
      }

      // Record the pivot row.
      pivotRows.add(i1);
      // Get the rational function at the pivot.
      RationalFunction f = entry[i1][j1];

      // Divide row i1 by f, if necessary.
      if (!f.isUnity()) {
        RationalFunction fRecip = RationalFunction.makeReciprocal(f);
        multRow(i1, fRecip);
        // and do the same to E
        E.multRow(i1, fRecip);
        // log it
        if (verbose && logger != null) {
          logger.log(Level.ALL, "Multiplied row",i1,"by",fRecip);
          logger.log(Level.ALL, "Matrix:","\n"+this.toString());
          logger.log(Level.ALL, "E:","\n"+E.toString());
        }
      }

      // Consider recording the nonzero assumption involved.
      // First, make sure we divided by a nonconstant.
      if (!f.getNumerator().isConstant()) {
        // Form the assumption.
        Assumption a = new Assumption(f.getNumerator(), AssumptionType.NONZERO);
        aset.add(a);
      }

      // Now clear out all other entries in column j1.
      for (int i = 0; i < m; i++) {
        if (i != i1 && !entry[i][j1].isZero()) {
          RationalFunction g = entry[i][j1];
          RationalFunction gneg = RationalFunction.makeNegative(g);
          addMultiple(i, i1, gneg);
          // and do the same to E
          E.addMultiple(i, i1, gneg);
          // log it
          if (verbose && logger != null) {
            logger.log(Level.ALL, "Added",gneg,"times row",i1,"to row",i);
            logger.log(Level.ALL, "Matrix:","\n"+this.toString());
            logger.log(Level.ALL, "E:","\n"+E.toString());
          }
        }
      }
      // Advance to the next row and column.
      i0 = i1 + 1;
      j0 = j1 + 1;
    }
    elemMatProd = E;
    return aset;
  }

  /*
   * Version of RREF algorithm that takes a logger and an AssumptionManager.
   */
  public void putInRREF(AssumptionManager amgr, LogManager logger) throws BadAssumptionsException {
    logger.log(Level.ALL, "Setting matrix to verbose mode, for completion of RREF.");
    verbose = true;
    int m = rowNum;
    int n = colNum;
    int i0 = nextI0;
    int j0 = nextJ0;
    if (i0 == 0 && j0 == 0) {
      pivotRows = new Vector<>();
    }

    int M = m;
    int N = n;
    // Augmented columns cannot have pivots.
    N -= numAugCols;

    // We maintain E as the product of all elementary matrices we would have multiplied by,
    // in order to achieve the row operations that we have done.
    Matrix E = Matrix.makeIdentity(M);

    while (true) {
      // Look for the next pivot.
      int[] pivot;
      pivot = getNextVarNumPivot(i0, M, j0, N, amgr, logger);
      // If there isn't any, then we're done.
      if (pivot[0] == -1) {
        // This records the fact that we exited the method by running out of pivots.
        outOfPivots = true;
        // Since we are done row reducing, it is now time to add the AZR assumptions.
        AssumptionSet azr = getAlmostZeroRowAssumptions();
        logger.log(Level.ALL, "Found the following assumptions for almost-zero rows:\n",azr);
        // They are necessary consequences of the assumptions we have made so far,
        // so we add them as such, if there were any.
        if (azr.size() > 0) {
          logger.log(Level.ALL, "Adding almost-zero row assumptions.");
          amgr.addNecessaryAssumptions(azr);
        }
        break;
      }
      // Let the pivot be i1,j1.
      int i1 = pivot[0];
      int j1 = pivot[1];

      // If the pivot function has variable numerator, and haltOnVariableNumPivot is set to true,
      // then store the next i0 and j0, and return.
      if (haltOnVariableNumPivot && !entry[i1][j1].getNumerator().isConstant()) {
        nextI0 = i0;
        nextJ0 = j0;
      }

      // Get the rational function at the pivot.
      RationalFunction f = entry[i1][j1];

      // Record the nonzero assumption involved.
      // First, make sure we are dividing by a nonconstant.
      if (!f.getNumerator().isConstant()) {
        // Form the assumption.
        Assumption a = new Assumption(f.getNumerator(), AssumptionType.NONZERO);
        // Add it, as a possibly unnecessary assumption.
        amgr.addPossiblyUnnecessaryAssumption(a);
      }


      // If i1 > i0, then swap these rows, and set i1 = i0.
      if (i1 > i0) {
        swapRows(i1, i0);
        // and do the same to E
        E.swapRows(i1, i0);
        // log it
        if (verbose) {
          logger.log(Level.ALL, "Swapped rows",i1,"and",i0);
          logger.log(Level.ALL, "Matrix:","\n"+this.toString());
          //logger.log(Level.ALL,"E:","\n"+E.toString());
        }
        // set i1 = i0.
        i1 = i0;
      }

      // Now that we have swapped rows if necessary, we can record the pivot row.
      pivotRows.add(i1);

      // Similarly, for the column.
      // If j1 > j0, then swap these rows, and set j1 = j0.
      if (j1 > j0) {
        swapCols(j1, j0);
        // (Can't do the same to E, since this is not a row operation!)
        // log it
        if (verbose) {
          logger.log(Level.ALL, "Swapped columns",j1,"and",j0);
          logger.log(Level.ALL, "Matrix:","\n"+this.toString());
        }
        // set j1 = j0.
        j1 = j0;
      }

      // Divide row i1 by f, if f is not unity.
      if (!f.isUnity()) {
        RationalFunction fRecip = RationalFunction.makeReciprocal(f);
        multRow(i1, fRecip);
        // and do the same to E
        E.multRow(i1, fRecip);
        // log it
        if (verbose) {
          logger.log(Level.ALL, "Multiplied row",i1,"by",fRecip);
          logger.log(Level.ALL, "Matrix:","\n"+this.toString());
          //logger.log(Level.ALL,"E:","\n"+E.toString());
        }
      }

      // Now clear out all other entries in column j1.
      for (int i = 0; i < m; i++) {
        if (i != i1 && !entry[i][j1].isZero()) {
          RationalFunction g = entry[i][j1];
          RationalFunction gneg = RationalFunction.makeNegative(g);
          addMultiple(i, i1, gneg);
          // and do the same to E
          E.addMultiple(i, i1, gneg);
          // log it
          if (verbose) {
            logger.log(Level.ALL, "Added",gneg,"times row",i1,"to row",i);
            logger.log(Level.ALL, "Matrix:","\n"+this.toString());
            //logger.log(Level.ALL,"E:","\n"+E.toString());
          }
        }
      }
      // Advance to the next row and column.
      i0 = i1 + 1;
      j0 = j1 + 1;
      // Record these, in case we need to revert to this state.
      nextI0 = i0;
      nextJ0 = j0;
    }
    elemMatProd = E;
  }

  // Let j1 be the first column, with j0 <= j1 < n, in which there is a nonzero
  // entry in a row i with i0 <= i < m, or -1 if there is no such column.
  // Let i1 be the first row having a nonzero entry in column j1 (or -1 if j1 is -1).
  // Return [i1,j1].
  @Deprecated
  private int[] getNextPivot(int i0, int m, int j0, int n) {
    int[] pivot = {-1, -1};
    for (int j = j0; j < n; j++) {
      int[] nonzero = findNonzeroEntriesInColumn(j, i0, m);
      if (nonzero.length > 0) {
        pivot[0] = nonzero[0];
        pivot[1] = j;
        break;
      }
    }
    return pivot;
  }

  /*
   * Over the submatrix i0 <= i < m, j0 <= j < n, find a preferred entry to be the next
   * pivot. We prefer constants over variables; among constants, smaller "height" is preferred
   * (this is the max of the absolute values of the num and denom); among constants of equal height,
   * positive is preferred over negative.
   * We return the row and column of the pivot, or [-1, -1] if every remaining entry is 0.
   */
  private int[] getNextPivotFree(int i0, int m, int j0, int n) {
    int[] pivot = {-1, -1};
    // Wrap all entries in the remaining submatrix as SortablePivotEntries,
    // so that they can be sorted according to how well we prefer them as pivots.
    List<SortablePivotEntry> spes = new Vector<>();
    for (int j = j0; j < n; j++) {
      for (int i = i0; i < m; i++) {
        spes.add(new SortablePivotEntry(i, j, entry[i][j]));
      }
    }
    if (spes.size() == 0) {
      // In this case we didn't find any pivots, so return {-1,-1}.
      return pivot;
    }
    // If we found one or more, then sort them.
    Collections.sort(spes);
    // Now the best one is the first one.
    SortablePivotEntry best = spes.get(0);
    if (best.getFunction().isZero()) {
      // In this case even the best selection was 0, so /all/ remaining entries must be zero.
      // So we return 'pivot' as initialized to {-1, -1}, indicating that there are no pivots left.
      return pivot;
    } else {
      // We found a nonzero pivot. Return its location.
      pivot = best.getRowCol();
    }
    return pivot;
  }

  private class PointedAssumption {

    private final int i, j;
    private final Assumption a;

    public PointedAssumption(int i, int j, Assumption a) {
      this.i = i; this.j = j; this.a = a;
    }

    public Assumption getAssumption() {
      return a;
    }

    public int[] getPoint() {
      int[] pt = {i, j};
      return pt;
    }

    @Override
    public String toString() {
      return a.toString();
    }

  }

  /*
   * Over the submatrix i0 <= i < m, j0 <= j < n, find a preferred entry to be the next
   * pivot.
   *
   * This method should be called only once we know that all remaining potential pivots
   * have variable numerator.
   *
   * We prefer a pivot which:
   *   (a) we have already assumed to be nonzero in the current assumption set
   *       (thus minimizing potential nonnecessity)
   *   (b) in lieu of that, one which at least is not contradictory with the
   *       current assumption set
   * And if we cannot find any such, then it is time to backtrack, and we throw an exception.
   *
   * We return the row and column of the pivot, or [-1, -1] if every remaining entry is 0.
   */
  private int[] getNextVarNumPivot(int i0, int m, int j0, int n,
      AssumptionManager amgr, LogManager logger) throws BadAssumptionsException {
    int[] pivot = {-1, -1};
    if (i0 >= m || j0 >= n) {
      return pivot;
    }

    logger.log(Level.ALL, "Selecting next variable-numerator pivot,",
        "for submatrix starting at",i0,",",j0,"in matrix:","\n"+this);

    // Start by forming the nonzero assumption on the numerator of each remaining nonzero entry.
    List<PointedAssumption> nz = new Vector<>();
    for (int j = j0; j < n; j++) {
      for (int i = i0; i < m; i++) {
        Polynomial p = entry[i][j].getNumerator();
        // Skip zeros.
        if (p.isZero()) {
          continue;
        }
        if (p.isConstant()) {
          // This should not happen! However, just to have a defined behavior in case it does happen,
          // we will immediately return this entry as the selected pivot.
          pivot[0] = i; pivot[1] = j;
          return pivot;
        }
        assert (!p.isConstant());
        Assumption a = new Assumption(p, AssumptionType.NONZERO);
        nz.add(new PointedAssumption(i, j, a));
      }
    }

    // If nz is empty, it's because all the remaining entries are zeros.
    if (nz.size() == 0) {
      logger.log(Level.ALL, "All remaining entries are zero.");
      return pivot;
    }

    logger.log(Level.ALL, "Found potential assumptions:\n",nz);

    // Now sort these into classes:
    //   A = those which are already implied by the current assumption set.
    //   B = those which are not already implied, but at least do not immediately contradict
    //       the current assumption set.
    List<PointedAssumption> implied = new Vector<>();
    List<PointedAssumption> noncontra = new Vector<>();
    for (PointedAssumption a : nz) {
      AssumptionRelation rel = amgr.matchAgainst(a.getAssumption());
      switch (rel) {
      // If a is the same as something in the current set, or is implied by anything in there,
      // then we add a to 'implied'.
      case ISSAMEAS:
      case WEAKENS:
        implied.add(a);
        break;
      // Else, if a contradicts something in the current set, then we can't add it to either
      // of our collections, 'implied' or 'noncontra'. It is good for nothing.
      case CONTRADICTS:
        break;
      // Finally, any others cannot be called 'implied', but they don't contradict either
      // (at least not obviously), so they go in 'noncontra'.
      default:
        noncontra.add(a);
      }
    }

    logger.log(Level.ALL, "The following are implied by the assumptions we have already made:\n",implied);
    logger.log(Level.ALL, "The following are not implied by those already made, but are at least non-contradictory:\n",noncontra);

    // Did we get any acceptable ones?
    // If not...
    if (implied.size() == 0 && noncontra.size() == 0) {
      logger.log(Level.ALL, "Every potential pivot results in a contradictory assumption!");
      throw new BadAssumptionsException();
    }
    // Otherwise we got at least one acceptable one.

    //TODO: Among acceptable ones, we should favor
    //  (a) one that is a common factor of all the nonzero terms in its row
    //  (b) one of few terms, both in numerator and denominator.
    // For now, we just take any member of 'implied', or, if it is empty, then any member of 'noncontra'.
    if (implied.size() > 0) {
      PointedAssumption a = implied.get(0);
      logger.log(Level.ALL, "We choose an assumption implied by those already in force:\n",a);
      return a.getPoint();
    } else {
      PointedAssumption a = noncontra.get(0);
      logger.log(Level.ALL, "We choose an assumption not immediately contradictory with those in force:\n",a);
      return a.getPoint();
    }
  }

  /*
   * Return an array listing those rows i, with i0 <= i < m, that have nonzero entries in column j.
   */
  private int[] findNonzeroEntriesInColumn(int j, int i0, int m) {
    Vector<Integer> rows = new Vector<>();
    for (int i = i0; i < m; i++) {
      if (!entry[i][j].isZero()) {
        rows.add(Integer.valueOf(i));
      }
    }
    return makeIntArray(rows);
  }

  private class SortablePivotEntry implements Comparable<SortablePivotEntry> {

    private final int kind;
    private final int height;
    private final int sign;
    private final RationalFunction func;
    private final int i0, j0;

    public SortablePivotEntry(int i, int j, RationalFunction f) {
      i0 = i;
      j0 = j;
      func = f;
      // f is supposed to be nonzero, but just in case it is zero, we make it of kind 10, which
      // is "greater than" all other kinds
      if (f.isZero()) {
        kind = 10;
        height = 0;
        sign = 0;
      } else if (!f.isConstant()) {
        if (f.getNumerator().isConstant()) {
          // If num const, this is almost as good as a constant function.
          kind = 1;
          Rational r = f.getNumerator().getConstant();
          height = r.getHeight();
          if (r.isPositive()) {
            sign = 1;
          } else {
            sign = 2;
          }
        } else {
          kind = 5;
          // We prefer functions with fewer terms in num and denom.
          height = f.getTermHeight();
          sign = 0;
        }
      } else {
        // In this case f must be a nonzero constant.
        kind = 0;
        Rational r = f.getConstant();
        height = r.getHeight();
        if (r.isPositive()) {
          sign = 1;
        } else {
          sign = 2;
        }
      }
    }

    @Override
    public int compareTo(SortablePivotEntry other) {
      if (this.kind != other.kind) {
        return this.kind - other.kind;
      }
      if (this.height != other.height) {
        return this.height - other.height;
      }
      return this.sign - other.sign;
    }

    public int[] getRowCol() {
      int[] rc = {i0, j0};
      return rc;
    }

    public RationalFunction getFunction() {
      return func;
    }

  }

  // Set a limit on the width of any column. Any entry going over this width will be
  // given an abbreviation, and displayed in full underneath the matrix.
  // Set to 0 if you don't want to impose any maximum.
  private int colWidthCap = 6;
  @Override
  public String toString() {
    // End caps and gaps:
    String left = "[ ";
    String right = " ]\n";
    String gap = "  ";

    // Set max line length:
    int maxLen = 150;

    // How many chars for max row number?
    // I.e. how many decimal digits in the decimal representation of the number of rows
    // in the matrix?
    int maxrownumchars = Integer.toString(rowNum-1).length();
    maxLen -= maxrownumchars;
    // This is okay as long as maxrownumchars < 150.
    // If not, then you have a matrix of at least 10^150 rows!!
    // Your computer doesn't have that much memory....

    // Get the width of each column.
    int[] widths = new int[colNum];
    for (int j = 0; j < colNum; j++) {
      int w = 0; int l;
      for (int i = 0; i < rowNum; i++) {
        l = entry[i][j].toString().length();
        if (l > w) {
          // Then this entry is longer than those seen before.
          if (colWidthCap > 0 && l > colWidthCap) {
            // In this case, we are limiting column widths, and this entry exceeds
            // the limit. So we do not update w, except to try to make sure it is
            // at least wide enough for the substitution string we will put in for
            // this entry. We'll go for a width of 3 here permitting %0 up to %99.
            // If there are more than 100 abbreviated entries, your matrix is
            // probably too bad to browse anyway.
            if (w < 3) {
              w = 3;
            }
          } else {
            w = l;
          }
        }
      }
      widths[j] = w;
    }

    // Write linebreak and gap prefixes.
    String[] pre = new String[colNum];
    int c = 0; int w;
    String prefix;
    for (int j = 0; j < colNum; j++) {
      w = widths[j];

      // Prepare prefix.
      prefix = "";
      if (c > 0) {
        prefix += gap;
      }
      if (j == colNum - numAugCols) {
        prefix += "|"+gap;
      }
      w = w + prefix.length();

      if (c == 0) {
        // an empty line takes the next column, no matter how wide it is
        c = w;
        pre[j] = prefix;
      } else if (c + w <= maxLen) {
        // a nonempty line takes the next column if it fits
        c = c + w;
        pre[j] = prefix;
      } else {
        // when the next column overflows a nonempty line, we go to the next line
        // So the prefix is '\n' plus the one we thought we'd use, minus the initial gap
        // we put on it.
        c = w;
        pre[j] = "\n"+prefix.substring(gap.length());
      }
    }

    // Construct a "space string", of which substrings can be used.
    char sp = " ".charAt(0);
    char[] sps = new char[maxLen];
    for (int k = 0; k < maxLen; k++) {
      sps[k] = sp;
    }
    String spaces = new String(sps);

    // Now write.
    List<String> abbreviatedEntries = new Vector<>();
    String s = "";
    String e;
    int W;
    for (int i = 0; i < rowNum; i++) {
      String rn = Integer.toString(i);
      s += spaces.substring(0, maxrownumchars-rn.length()) + rn + " ";
      s += left;
      for (int j = 0; j < colNum; j++) {
        e = entry[i][j].toString();
        if (colWidthCap > 0 && e.length() > colWidthCap) {
          abbreviatedEntries.add(e);
          e = "%"+Integer.toString(abbreviatedEntries.size());
        }
        if (widths[j] < maxLen) {
          W = widths[j] - e.length();
          s += pre[j] + spaces.substring(0, W) + e;
        } else {
          s += pre[j] + e;
        }
      }
      s += right;
    }

    if (colWidthCap > 0 && abbreviatedEntries.size() > 0) {
      s += "\n";
      for (int i = 0; i < abbreviatedEntries.size(); i++) {
        s += "%"+Integer.toString(i+1)+": "+abbreviatedEntries.get(i)+"\n";
      }
      s += "\n";
    }

    return s;
  }

  /*
   * Modify this matrix in-place, applying the substitution to each of its entries.
   */
  public void applySubstitution(Substitution subs) {
    for (int i = 0; i < rowNum; i++) {
      for (int j = 0; j < colNum; j++) {
        RationalFunction f = entry[i][j];
        entry[i][j] = RationalFunction.applySubstitution(subs, f);
      }
    }
  }

}
