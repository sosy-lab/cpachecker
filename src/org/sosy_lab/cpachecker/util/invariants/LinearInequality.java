/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.invariants;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.sosy_lab.cpachecker.util.invariants.interfaces.GeneralVariable;
import org.sosy_lab.cpachecker.util.invariants.interfaces.VariableManager;

public class LinearInequality {

  // Represents a matrix inequality Ax <= b.

  private Vector< List<Coeff> > rows = new Vector<List<Coeff>>();
  private Vector<Coeff> rhs = new Vector<Coeff>();
  private VariableManager vmgr;

  public LinearInequality(VariableManager vmgr) {
    this.vmgr = vmgr;
  }

  public int getNumIneqs() {
    return rows.size();
  }

  public int getNumVars() {
    return vmgr.getNumVars();
  }

  public Coeff getCoeff(int i, int j) {
    Coeff a = null;
    try {
      a = rows.get(i).get(j);
    } catch (ArrayIndexOutOfBoundsException e) {
      System.err.println(e.getMessage());
    }
    return a;
  }

  public Coeff getRHSCoeff(int i) {
    Coeff a = null;
    try {
      a = rhs.get(i);
    } catch (ArrayIndexOutOfBoundsException e) {
      System.err.println(e.getMessage());
    }
    return a;
  }

  public void addRow(List<Coeff> coeffs, Coeff ub) {
    rows.add(coeffs);
    rhs.add(ub);
  }

  public void setRows(Vector<List<Coeff>> rows) {
    this.rows = rows;
  }

  public Vector<List<Coeff>> getRows() {
    return this.rows;
  }

  public void setRHS(Vector<Coeff> rhs) {
    this.rhs = rhs;
  }

  public Vector<Coeff> getRHS() {
    return this.rhs;
  }

  /**
   * Does not modify this object.
   * Returns a new LinearInequality which is the
   * concatenation of these two; this one on top, B on bottom.
   * The new LinearInequality will have the same variable
   * manager as this one. This should be okay, since you should
   * not be appending unless the two LI's have the same list of
   * variables.
   * @param B
   * @return
   */
  public LinearInequality combine(LinearInequality B) {
    Vector<List<Coeff>> rows = new Vector<List<Coeff>>();
    Vector<Coeff> rhs = new Vector<Coeff>();

    // Make the rows.
    for (int i = 0; i < this.rows.size(); i++) {
      rows.add( this.rows.get(i) );
    }
    Vector<List<Coeff>> rowsB = B.getRows();
    for (int i = 0; i < rowsB.size(); i++) {
      rows.add( rowsB.get(i) );
    }

    // Make the rhs.
    for (int i = 0; i < this.rhs.size(); i++) {
      rhs.add( this.rhs.get(i) );
    }
    Vector<Coeff> rhsB = B.getRHS();
    for (int i = 0; i < rhsB.size(); i++) {
      rhs.add( rhsB.get(i) );
    }

    // Build the new LI.
    LinearInequality AB = new LinearInequality(this.vmgr);
    AB.setRows(rows);
    AB.setRHS(rhs);
    return AB;
  }

  /**
   * Modifies this object, by adding the rows of B
   * to the bottom of this one.
   */
  public void append(LinearInequality B) {
    this.rows.addAll(B.rows);
    this.rhs.addAll(B.rhs);
  }

  @Override
  public String toString() {
    // returns an easily readable representation of the inequality
    String s = "Matrix:\n";
    List<Coeff> row;
    Coeff C;
    for (int i = 0; i < rows.size(); i++) {
      row = rows.get(i);
      for (int j = 0; j < row.size(); j++) {
        C = row.get(j);
        s += " "+C.toString();
      }
      s += "\n";
    }

    s += "Variables:\n";
    Iterator<GeneralVariable> vars = vmgr.iterator();
    GeneralVariable V;
    while(vars.hasNext()) {
      V = vars.next();
      s += " "+V.toString();
    }

    s += "\nRHS:\n";
    for (int i = 0; i < rhs.size(); i++) {
      C = rhs.get(i);
      s += " "+C.toString();
    }
    s += "\n";
    return s;
  }

}