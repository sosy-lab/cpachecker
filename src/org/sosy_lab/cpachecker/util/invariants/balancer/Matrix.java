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
import org.sosy_lab.cpachecker.util.invariants.balancer.interfaces.MatrixI;

public class Matrix implements MatrixI {

  private int rowNum;
  private int colNum;
  private RationalFunction[][] entry;
  private int numAugCols = 0;

  private List<Integer> pivotRows = null;
  private Matrix elemMatProd = null;
  private boolean verbose = false;

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
    return m;
  }

  public static Matrix makeIdentity(int n) {
    Matrix id = new Matrix(n,n);
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

  public Matrix concat(MatrixI b) {
    Matrix m = (Matrix)b;
    return Matrix.concat(this, m);
  }

  /*
   * We create a matrix in which the ordinary columns come from a, and the
   * augmentation columns come from b.
   */
  public static Matrix augment(Matrix a, Matrix b) {
    Matrix c = concat(a,b);
    int bn = b.colNum;
    c.numAugCols = bn;
    return c;
  }

  public Matrix augment(MatrixI b) {
    Matrix m = (Matrix)b;
    return Matrix.augment(this,m);
  }

  /*
   * Fill this matrix will zeros.
   */
  public void zeroFill() {
    for (int i = 0; i < rowNum; i++) {
      for (int j = 0; j < colNum; j++) {
        entry[i][j] = RationalFunction.makeZero();
      }
    }
  }

  public RationalFunction get(int i, int j) {
    return entry[i][j];
  }

  public void set(int i, int j, RationalFunction f) {
    entry[i][j] = f;
  }

  /*
   * Swap rows i1 and i2.
   */
  public void swapRows(int i1, int i2) {
    RationalFunction temp;
    for (int j = 0; j < colNum; j++) {
      temp = entry[i1][j];
      entry[i1][j] = entry[i2][j];
      entry[i2][j] = temp;
    }
  }

  /*
   * Multiply row i by RationalFunction f.
   */
  public void multRow(int i, RationalFunction f) {
    for (int j = 0; j < colNum; j++) {
      entry[i][j] = RationalFunction.multiply(entry[i][j],f);
    }
  }

  /*
   * Add row i2 times RationalFunction f to row i1.
   */
  public void addMultiple(int i1, int i2, RationalFunction f) {
    for (int j = 0; j < colNum; j++) {
      RationalFunction product = RationalFunction.multiply(f,entry[i2][j]);
      RationalFunction sum = RationalFunction.add(entry[i1][j],product);
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
  public Set<Assumption> getAlmostZeroRowAssumptions() {
    Set<Assumption> aset = new HashSet<Assumption>();
    for (int i = 0; i < rowNum; i++) {
      if (isAlmostZeroRow(i)) {
        // In this case, row i has all entries zero in its non-augmentation columns.
        // We now therefore consider the aug cols, one at a time.
        // Each one having an entry not identically zero in row i results in an Assumption.
        for (int j = 0; j < numAugCols; j++) {
          RationalFunction f = entry[i][colNum - numAugCols + j];
          if (!f.isZero()) {
            aset.add( new Assumption(f, AssumptionType.ZERO ) );
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
  public Set<Assumption> getDenomNonZeroAssumptions() {
    Set<Assumption> aset = new HashSet<Assumption>();
    outerloop:
    for (int i = 0; i < rowNum; i++) {
      for (int j = 0; j < colNum; j++) {
        Polynomial denom = entry[i][j].getDenominator();
        if (denom.isConstant()) {
          if (denom.isZero()) {
            aset = new HashSet<Assumption>();
            aset.add( new Assumption(RationalFunction.makeZero(),AssumptionType.NONZERO) );
            break outerloop;
          } else {
            // If the denom is a nonzero constant then we add nothing.
          }
        } else {
          aset.add( new Assumption(denom,AssumptionType.NONZERO) );
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
  public List<Assumption> putInRREF() {
    LogManager lm = null;
    return putInRREF(lm);
  }

  /*
   * Version of RREF algorithm that takes a logger.
   * Returns the set of nonzero assumptions made each time we divided a pivot row by its lead entry.
   */
  public List<Assumption> putInRREF(LogManager logger) {
    int m = rowNum;
    int n = colNum;
    int i0 = 0;
    int j0 = 0;
    pivotRows = new Vector<Integer>();

    List<Assumption> alist = new Vector<Assumption>();

    int M = m;
    int N = n;
    // Augmented columns cannot have pivots.
    N -= numAugCols;

    // We maintain E as the product of all elementary matrices we would have multiplied by,
    // in order to achieve the row operations that we have done.
    Matrix E = Matrix.makeIdentity(M);

    while (true) {
      // Look for the next pivot.
      int[] pivot = getNextPivot(i0,M,j0,N);
      // If there isn't any, then we're done.
      if (pivot[0] == -1) {
        break;
      }
      // Let the pivot be i1,j1.
      int i1 = pivot[0];
      int j1 = pivot[1];
      // If i1 > i0, then swap these rows, and set i1 = i0.
      if (i1 > i0) {
        swapRows(i1,i0);
        // and do the same to E
        E.swapRows(i1, i0);
        // log it
        if (verbose && logger != null) {
          logger.log(Level.ALL,"Swapped rows",i1,"and",i0);
          logger.log(Level.ALL,"Matrix:","\n"+this.toString());
          logger.log(Level.ALL,"E:","\n"+E.toString());
        }
        // set i1 = i0.
        i1 = i0;
      }
      // Record the pivot row.
      pivotRows.add(i1);
      // Get the rational function at the pivot.
      RationalFunction f = entry[i1][j1];
      // Divide row i1 by f.
      RationalFunction fRecip = RationalFunction.makeReciprocal(f);
      multRow(i1,fRecip);
      // and do the same to E
      E.multRow(i1,fRecip);
      // log it
      if (verbose && logger != null) {
        logger.log(Level.ALL,"Multiplied row",i1,"by",fRecip);
        logger.log(Level.ALL,"Matrix:","\n"+this.toString());
        logger.log(Level.ALL,"E:","\n"+E.toString());
      }
      // Record the nonzero assumption involved.
      if (!f.getNumerator().isConstant()) {
        alist.add( new Assumption(f.getNumerator(),AssumptionType.NONZERO) );
      }

      // Now clear out all other entries in column j1.
      for (int i = 0; i < m; i++) {
        if (i != i1 && !entry[i][j1].isZero()) {
          RationalFunction g = entry[i][j1];
          RationalFunction gneg = RationalFunction.makeNegative(g);
          addMultiple(i,i1,gneg);
          // and do the same to E
          E.addMultiple(i,i1,gneg);
          // log it
          if (verbose && logger != null) {
            logger.log(Level.ALL,"Added",gneg,"times row",i1,"to row",i);
            logger.log(Level.ALL,"Matrix:","\n"+this.toString());
            logger.log(Level.ALL,"E:","\n"+E.toString());
          }
        }
      }
      // Advance to the next row and column.
      i0 = i1 + 1;
      j0 = j1 + 1;
    }
    elemMatProd = E;
    return alist;
  }

  // Let j1 be the first column, with j0 <= j1 < n, in which there is a nonzero
  // entry in a row i with i0 <= i < m, or -1 if there is no such column.
  // Let i1 be the first row having a nonzero entry in column j1 (or -1 if j1 is -1).
  // Return [i1,j1].
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
   * Return an array listing those rows i, with i0 <= i < m, that have nonzero entries in column j.
   */
  private int[] findNonzeroEntriesInColumn(int j, int i0, int m) {
    Vector<Integer> rows = new Vector<Integer>();
    for (int i = i0; i < m; i++) {
      if (!entry[i][j].isZero()) {
        rows.add(new Integer(i));
      }
    }
    return makeIntArray(rows);
  }

  @Override
  public String toString() {
    // End caps and gaps:
    String left = "[ ";
    String right = " ]\n";
    String gap = "  ";

    // Set max line length:
    int maxLen = 150;

    // How many chars for max row number?
    int maxrownumchars = Integer.toString(rowNum-1).length();
    maxLen -= maxrownumchars; // if maxrownumchars >= 150, you're nuts.

    // Get the width of each column.
    int[] widths = new int[colNum];
    for (int j = 0; j < colNum; j++) {
      int w = 0; int l;
      for (int i = 0; i < rowNum; i++) {
        l = entry[i][j].toString().length();
        if (l > w) {
          w = l;
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
    String s = "";
    String e;
    int W;
    for (int i = 0; i < rowNum; i++) {
      String rn = Integer.toString(i);
      s += spaces.substring(0,maxrownumchars-rn.length()) + rn + " ";
      s += left;
      for (int j = 0; j < colNum; j++) {
        e = entry[i][j].toString();
        if (widths[j] < maxLen) {
          W = widths[j] - e.length();
          s += pre[j] + spaces.substring(0,W) + e;
        } else {
          s += pre[j] + e;
        }
      }
      s += right;
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
        entry[i][j] = RationalFunction.applySubstitution(subs,f);
      }
    }
  }

}
