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

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;

import org.sosy_lab.cpachecker.util.invariants.balancer.Assumption.AssumptionType;

public class WeispfenningSystem {

  private List<LinCombOverParamField> rows;
  private int numEqns;
  private int numIneqs;
  private int numRows;

  private AssumptionSet aset;

  private boolean initialized = false;
  private boolean hasVars;
  private boolean hasSolvedOnce = false;

  private Queue<Integer> varQueue;
  private Queue<Integer> rowQueue;
  private WeispfenningSystem child;
  private int currentVar;
  private int currentRow;

  WeispfenningSystem(List<LinCombOverParamField> r, int nE, int nI) {
    rows = r; numEqns = nE; numIneqs = nI; numRows = nE + nI; aset = new AssumptionSet();
  }

  WeispfenningSystem(Matrix a) {
    int m = a.getRowNum();
    int n = a.getColNum();
    rows = new Vector<>(m);
    for (int i = 0; i < m; i++) {
      List<RationalFunction> row = new Vector<>(n);
      for (int j = 0; j < n; j++) {
        row.add(a.getEntry(i, j));
      }
      rows.add(new LinCombOverParamField(row));
    }
    // We assume cols 0 through n-2 represent coeffs of variables, while
    // the final column, col n-1, represents constant terms.
    // So we add on n-1 rows, representing the statement that each variable
    // be nonnegative.
    for (int i = 0; i < n-1; i++) {
      rows.add(new LinCombOverParamField(n, i));
    }
    // So first m rows represent equations, and last n-1 represent inequalities.
    numEqns = m;
    numIneqs = n-1;
    numRows = m+n-1;
    aset = new AssumptionSet();
  }

  AssumptionSet getAssumptionSet() {
    return aset;
  }

  /*
   * Eliminate row i, by solving that row for the variable in column j,
   * substituting the result into all other rows, and deleting row i.
   * We do not modify this object, but return a new one representing the
   * result.
   */
  WeispfenningSystem eliminateRow(int i, int j) {
    LinCombOverParamField x = rows.get(i).setZeroAndSolveFor(j);
    List<LinCombOverParamField> newRows = new Vector<>(numRows-1);
    for (int k = 0; k < numRows; k++) {
      if (k == i) { continue; }
      LinCombOverParamField r = rows.get(k);
      r = r.substitute(j, x);
      newRows.add(r);
    }
    int nE = i < numEqns ? numEqns - 1 : numEqns;
    int nI = i < numEqns ? numIneqs : numIneqs - 1;
    WeispfenningSystem w = new WeispfenningSystem(newRows, nE, nI);
    w.aset = this.aset.copy();
    return w;
  }

  WeispfenningSystem deleteRow(int i) {
    List<LinCombOverParamField> newRows = new Vector<>(numRows-1);
    for (int k = 0; k < numRows; k++) {
      if (k == i) { continue; }
      LinCombOverParamField r = rows.get(k);
      r = r.copy();
      newRows.add(r);
    }
    int nE = i < numEqns ? numEqns - 1 : numEqns;
    int nI = i < numEqns ? numIneqs : numIneqs - 1;
    WeispfenningSystem w = new WeispfenningSystem(newRows, nE, nI);
    w.aset = this.aset.copy();
    return w;
  }

  boolean addAssumption(Assumption a) {
    return aset.add(a);
  }

  LinCombOverParamField getRow(int i) {
    return rows.get(i);
  }

  /*
   * Return the system resulting from the elimination of all equations (but not
   * inequalities) in this one. Return null if there is an inconsistency in the equations.
   *
   * NB: Actually, this method makes unsafe assumptions. This is not usable.
   */
  @Deprecated
  WeispfenningSystem eliminateEquations() throws BadAssumptionsException {
    WeispfenningSystem w = this.copy();
    while (w.numEqns > 0) {
      LinCombOverParamField row0 = w.getRow(0);
      int j = row0.findFirstNonzeroCoeff();
      if (j < 0) {
        // The row is all zeros. Simply delete it.
        w = w.deleteRow(0);
      } else if (j == row0.length()-1) {
        // The row has only a constant term, which is not identically zero.
        // So we add it as a new assumption, and delete this row.
        RationalFunction f = row0.getCoeff(j);
        Assumption a = new Assumption(f, AssumptionType.ZERO);
        boolean consistent = w.addAssumption(a);
        if (!consistent) {
          throw new BadAssumptionsException();
        }
        w = w.deleteRow(0);
      } else {
        // Else there is at least one variable remaining, and j points to the
        // first one from the left side.
        w = w.eliminateRow(0, j);
      }
    }
    return w;
  }

  WeispfenningSystem copy() {
    List<LinCombOverParamField> newRows = new Vector<>(numRows-1);
    for (int k = 0; k < numRows; k++) {
      LinCombOverParamField r = rows.get(k);
      r = r.copy();
      newRows.add(r);
    }
    WeispfenningSystem w = new WeispfenningSystem(newRows, numEqns, numIneqs);
    w.aset = this.aset.copy();
    return w;
  }

  @Override
  public String toString() {
    String s = "";
    for (LinCombOverParamField row : rows) {
      s += row.toString()+"\n";
    }
    return s;
  }

  /*
   * Initialize, only if not already initialized.
   */
  private void initialize() {
    if (this.initialized) { return; }
    reinit();
  }

  /*
   * Reinitialize.
   */
  void reinit() {
    // Decide whether we have variables.
    boolean hv = false;
    for (LinCombOverParamField r : rows) {
      if (r.hasVars()) {
        hv = true; break;
      }
    }
    this.hasVars = hv;
    // If have variables, then set up the var and row queues.
    if (this.hasVars) {
      varQueue = new ArrayDeque<>(this.getOccurringVars());
      currentVar = varQueue.poll();
      rowQueue = new ArrayDeque<>(this.getRowsWithVar(currentVar));
      currentRow = rowQueue.poll();
      this.child = this.eliminateRow(currentRow, currentVar);
    }
    this.hasSolvedOnce = false;
    this.initialized = true;
  }

  /*
   * Return the set of all those integers j such that
   * one or more rows has a nonzero coefficient in place j, and j
   * does NOT represent the constant term.
   */
  Set<Integer> getOccurringVars() {
    Set<Integer> vars = new HashSet<>();
    for (LinCombOverParamField r : rows) {
      Set<Integer> v = r.getOccurringVars();
      vars.addAll(v);
    }
    return vars;
  }

  /*
   * Return the set of those integers i such that row number i contains
   * the variable in place j.
   */
  Set<Integer> getRowsWithVar(int j) {
    Set<Integer> r = new HashSet<>();
    for (int i = 0; i < numRows; i++) {
      LinCombOverParamField lc = rows.get(i);
      if (lc.featuresVar(j)) {
        r.add(i);
      }
    }
    return r;
  }

  public AssumptionSet solve() {
    this.initialize();
    if (!this.hasVars) {
      if (hasSolvedOnce) {
        return null;
      } else {
        hasSolvedOnce = true;
        return checkSAT();
      }
    } else {
      AssumptionSet aset = null;
      while (true) {
        if (child == null) {
          return null;
        } else {
          aset = child.solve();
          if (aset != null) {
            return aset;
          } else {
            nextChild();
          }
        }
      }
    }
  }

  /*
   * Compute the next child, if there is one, or set child to null otherwise.
   */
  private void nextChild() {
    Integer nr = rowQueue.poll();
    if (nr != null) {
      currentRow = nr;
      this.child = this.eliminateRow(currentRow, currentVar);
    } else {
      Integer nv = varQueue.poll();
      if (nv != null) {
        currentVar = nv;
        rowQueue = new ArrayDeque<>(this.getRowsWithVar(currentVar));
        currentRow = rowQueue.poll();
        this.child = this.eliminateRow(currentRow, currentVar);
      } else {
        this.child = null;
      }
    }
  }

  /*
   * If no vars left, build assumption set reflecting what all the rows say.
   */
  private void buildAssumptionSet() {
    if (this.hasVars) { return; }
    aset = new AssumptionSet();
    for (int i = 0; i < numRows; i++) {
      LinCombOverParamField r = rows.get(i);
      AssumptionType at = AssumptionType.ZERO;
      if (i >= numEqns) {
        at = AssumptionType.NONNEGATIVE;
      }
      Assumption a = r.getAssumption(at);
      aset.add(a);
    }
  }

  /*
   * This method should only be called if this system has no variables.
   * Check whether the system is satisfiable.
   * If so, return the system's assumption set.
   * If not, return null.
   */
  private AssumptionSet checkSAT() {
    if (this.hasVars) { return null; }
    buildAssumptionSet();
    // TODO -- actually check aset for satisfiability
    //diag:
    //System.out.println(this.toString()+"\n");
    return aset;
    //
  }

}
