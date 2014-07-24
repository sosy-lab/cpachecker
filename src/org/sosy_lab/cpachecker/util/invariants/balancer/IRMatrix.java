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

import java.util.List;
import java.util.Vector;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.invariants.InfixReln;
import org.sosy_lab.cpachecker.util.invariants.balancer.Assumption.AssumptionType;
import org.sosy_lab.cpachecker.util.invariants.balancer.interfaces.MatrixI;

/*
 * "IRMatrix" stands for "Inequality Row Matrix", i.e., a matrix with an "inequality row".
 */
public class IRMatrix implements MatrixI {

  private final int rowNum;
  private final int colNum;
  private RationalFunction[][] entry;

  private final boolean hasInequalityRow = true;
  private InfixReln[] colIneq;

  private int numAugCols = 0;
  private InfixReln[] augIneq = new InfixReln[0];

  /*
   * Basic constructor.
   */
  public IRMatrix(int m, int n) {
    rowNum = m;
    colNum = n;
    entry = new RationalFunction[m][n];
    zeroFill();
    colIneq = new InfixReln[n];
    leqFill();
  }

  /*
   * Construct a single column.
   */
  public IRMatrix(List<RationalFunction> rfs, InfixReln reln) {
    int m = rfs.size();
    int n = 1;
    rowNum = m;
    colNum = n;
    entry = new RationalFunction[m][n];
    for (int i = 0; i < m; i++) {
      entry[i][0] = rfs.get(i);
    }
    colIneq = new InfixReln[1];
    colIneq[0] = reln;
  }

  @Override
  public IRMatrix getElemMatProd() {
    return null;
  }

  /*
   * Concatenate two matrices. We presume that neither has any augmentation columns.
   */
  public static IRMatrix concat(IRMatrix a, IRMatrix b) {
    int m = a.rowNum;
    if (m != b.rowNum) {
      System.err.println("Tried to concatenate matrices with different numbers of rows.");
      return null;
    }
    int an = a.colNum;
    int bn = b.colNum;
    IRMatrix c = new IRMatrix(m, an + bn);
    // copy entries
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < an; j++) {
        c.entry[i][j] = a.entry[i][j];
      }
      for (int j = 0; j < bn; j++) {
        c.entry[i][an+j] = b.entry[i][j];
      }
    }
    // copy column inequalities
    for (int j = 0; j < an; j++) {
      c.colIneq[j] = a.colIneq[j];
    }
    for (int j = 0; j < bn; j++) {
      c.colIneq[an+j] = b.colIneq[j];
    }
    return c;
  }

  @Override
  public IRMatrix concat(MatrixI b) {
    IRMatrix m = (IRMatrix)b;
    return IRMatrix.concat(this, m);
  }

  /*
   * We create a matrix in which the ordinary columns come from a, and the
   * augmentation columns come from b.
   */
  public static IRMatrix augment(IRMatrix a, IRMatrix b) {
    IRMatrix c = concat(a, b);
    int bn = b.colNum;
    c.numAugCols = bn;
    c.augIneq = new InfixReln[bn];
    for (int j = 0; j < bn; j++) {
      // For now we do this simplistically.
      // Really, strict ineqs can be relaxed when a strong column on the left is used.
      // Alternately, as a heuristic method, we might simply try relaxing them all, when
      // we fail to get the strict ineq., just to see if it works.
      c.augIneq[j] = b.colIneq[j];
    }
    return c;
  }

  @Override
  public IRMatrix augment(MatrixI b) {
    IRMatrix m = (IRMatrix) b;
    return IRMatrix.augment(this, m);
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

  /*
   * Make every column inequality lax.
   */
  public void leqFill() {
    for (int j = 0; j < colNum; j++) {
      colIneq[j] = InfixReln.LEQ;
    }
  }

  /*
   * Use this only to override the automatic computation of the ineqRow inequality.
   */
  public void setIneqRowIneq(int j, InfixReln reln) {
    augIneq[j] = reln;
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
   * We write the assumptions that the almost zero row terminals be zero, or satisfy
   * the appropriate constraint for the inequality row.
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
            AssumptionType atype;
            // If we're in the inequality row...
            if (hasInequalityRow && i == rowNum - 1) {
              if (augIneq[j] == InfixReln.LEQ) {
                // If the ineq for this column is <=, then we assume the entry is nonnegative.
                atype = AssumptionType.NONNEGATIVE;
              } else {
             // If the ineq for this column is <, then we assume the entry is positive.
                atype = AssumptionType.POSITIVE;
              }
            // Else we're in an ordinary row, and we must assume the the entry in the
            // aug col is actually equal to zero.
            } else {
              atype = AssumptionType.ZERO;
            }
            Assumption a = new Assumption(f, atype);
            aset.add(a);
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
   * Return set of nonzero assumptions made when dividing.
   */
  @Override
  public AssumptionSet putInRREF() {
    int m = rowNum;
    int n = colNum;
    int i0 = 0;
    int j0 = 0;
    AssumptionSet assume = new AssumptionSet();

    int M = m;
    int N = n;
    // An inequality row cannot have a pivot.
    if (hasInequalityRow) {
      M--;
    }
    // Augmented columns cannot have pivots.
    N -= numAugCols;

    while (true) {
      // Look for the next pivot.
      int[] pivot = getNextPivot(i0, M, j0, N);
      // If there isn't any, then we're done.
      if (pivot[0] == -1) {
        break;
      }
      // Let the pivot be i1,j1.
      int i1 = pivot[0];
      int j1 = pivot[1];
      // If i1 > i0, then swap these rows, and set i1 = i0.
      if (i1 > i0) {
        swapRows(i1, i0);
        i1 = i0;
      }
      // Get the rational function at the pivot.
      RationalFunction f = entry[i1][j1];
      // Divide row i1 by f.
      RationalFunction fRecip = RationalFunction.makeReciprocal(f);
      multRow(i1, fRecip);
      // If f has any parameters, then add the assumption that f's numerator is nonzero.
      if (!f.isConstant()) {
        Polynomial num = f.getNumerator();
        RationalFunction numOverUnity = new RationalFunction(num, new Polynomial(1));
        assume.add(new Assumption(numOverUnity, AssumptionType.NONZERO));
      }
      // Now clear out all other entries in column j1.
      for (int i = 0; i < m; i++) {
        if (i != i1 && !entry[i][j1].isZero()) {
          RationalFunction g = entry[i][j1];
          RationalFunction gneg = RationalFunction.makeNegative(g);
          addMultiple(i, i1, gneg);
        }
      }
      // Advance to the next row and column.
      i0 = i1 + 1;
      j0 = j1 + 1;
    }

    return assume;
  }

  @Override
  public AssumptionSet putInRREF(LogManager logger) {
    // TODO
    return null;
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
    Vector<Integer> rows = new Vector<>();
    for (int i = i0; i < m; i++) {
      if (!entry[i][j].isZero()) {
        rows.add(Integer.valueOf(i));
      }
    }
    return makeIntArray(rows);
  }

  @Override
  public String toString() {
    String s = "";
    String left = "[ ";
    String right = " ]\n";
    String gap = "  ";
    for (int i = 0; i < rowNum; i++) {
      // Write inequalities.
      if (hasInequalityRow && i == rowNum - 1) {
        s += left;
        for (int j = 0; j < colNum; j++) {
          if (j > 0) {
            s += gap;
          }
          if (j == colNum - numAugCols) {
            s += "|"+gap;
          }
          //s += colIneq[j].toString();
          s += "<";
        }
        s += right;
        s += left;
        for (int j = 0; j < colNum; j++) {
          if (j > 0) {
            s += gap;
          }
          if (j == colNum - numAugCols) {
            s += "|"+gap;
          }
          if (colIneq[j].toString().equals("<=")) {
            s += "=";
          } else {
            s += " ";
          }
        }
        s += right;
      }
      // Write row of entries.
      s += left;
      for (int j = 0; j < colNum; j++) {
        if (j > 0) {
          s += gap;
        }
        if (j == colNum - numAugCols) {
          s += "|"+gap;
        }
        s += entry[i][j].toString();
      }
      s += right;
    }
    return s;
  }

}
