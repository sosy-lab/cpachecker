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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.sosy_lab.cpachecker.util.invariants.balancer.Assumption.AssumptionType;



public class LinCombOverParamField {

  private List<RationalFunction> coeffs;
  private int numVars;

  LinCombOverParamField(List<RationalFunction> c) {
    construct(c);
  }

  LinCombOverParamField(RationalFunction[] a) {
    List<RationalFunction> c = new Vector<>(a.length);
    for (RationalFunction f : a) {
      c.add(f);
    }
    construct(c);
  }

  /*
   * m: total number of places
   * k: place to set equal to 1, while all others are set to 0. Places run from 0 to m-1.
   */
  LinCombOverParamField(int m, int k) {
    List<RationalFunction> c = new Vector<>(m);
    for (int i = 0; i < m; i++) {
      int n = (i == k ? 1 : 0);
      RationalFunction f = new RationalFunction(n);
      c.add(f);
    }
    construct(c);
  }

  private void construct(List<RationalFunction> c) {
    this.coeffs = c;
    this.numVars = c.size();
  }

  /*
   * Return the linear combination you would get if you set this linear combination
   * equal to zero, and solved for the kth variable, where 0 <= k < this.numVars.
   */
  LinCombOverParamField setZeroAndSolveFor(int k) {
    // If k out of bounds, return null.
    if (k < 0 || k >= this.numVars) {
      return null;
    }
    // Get the kth coefficient fk.
    RationalFunction fk = this.coeffs.get(k);
    // If kth coefficient is zero, return null.
    if (fk.isZero()) { return null; }
    // Make a fresh copy of all the coeffs.
    LinCombOverParamField lc = this.copy();
    // Set the kth to zero.
    lc.setCoeff(k, new RationalFunction(0));
    // Get the negative reciprocal nr of fk.
    RationalFunction nr = RationalFunction.makeNegative(RationalFunction.makeReciprocal(fk));
    // Multiply all coeffs in lc by nr.
    for (int i = 0; i < this.numVars; i++) {
      RationalFunction g = lc.coeffs.get(i);
      g = RationalFunction.multiply(nr, g);
      lc.coeffs.set(i, g);
    }
    return lc;
  }

  /*
   * Return the lin comb that results from substituting lc in for the kth variable in this one.
   * Create a fresh object, and do not modify this one or the passed one.
   * The passed lc and this one must have the same number of variables.
   */
  LinCombOverParamField substitute(int k, LinCombOverParamField lc) {
    if (k < 0 || k >= this.numVars) { return null; }
    if (lc.numVars != this.numVars) { return null; }
    // Get kth coeff in this lin comb.
    RationalFunction fk = this.coeffs.get(k);
    // Multiply each coeff in the given lin comb by fk, and add the corresponding coeff in
    // this lin comb; except we don't add our kth coeff.
    LinCombOverParamField result = lc.copy();
    for (int i = 0; i < this.numVars; i++) {
      RationalFunction g = lc.getCoeff(i);
      g = RationalFunction.multiply(fk, g);
      if (i != k) {
        g = RationalFunction.add(g, this.getCoeff(i));
      }
      result.setCoeff(i, g);
    }
    return result;
  }

  /*
   * Look for the first nonzero coefficient, starting from the left, and return j
   * if it is found in place j, or -1 if they are all zero.
   */
  int findFirstNonzeroCoeff() {
    int n = -1;
    for (int j = 0; j < numVars; j++) {
      if (!coeffs.get(j).isZero()) {
        n = j; break;
      }
    }
    return n;
  }

  /*
   * Here we treat all but the last coefficient as coeffs of variables.
   * So to "have variables" is to have at least one nonzero coefficient
   * in one of the places other than the last.
   */
  boolean hasVars() {
    int j = findFirstNonzeroCoeff();
    return (j >= 0 && j < numVars - 1);
  }

  Assumption getAssumption(AssumptionType at) {
    if (hasVars()) { return null; }
    RationalFunction f = coeffs.get(numVars-1);
    return new Assumption(f, at);
  }

  boolean featuresVar(int j) {
    return (0 <= j && j < numVars-1 && !coeffs.get(j).isZero());
  }

  /*
   * Return the set of all those integers j such that
   * we have a nonzero coefficient in place j, and j
   * does NOT represent the constant term.
   */
  Set<Integer> getOccurringVars() {
    Set<Integer> vars = new HashSet<>();
    for (int j = 0; j < numVars - 1; j++) {
      RationalFunction f = coeffs.get(j);
      if (!f.isZero()) {
        vars.add(Integer.valueOf(j));
      }
    }
    return vars;
  }

  int length() {
    return numVars;
  }

  RationalFunction getCoeff(int k) {
    if (k < 0 || k >= this.numVars) { return null; }
    return this.coeffs.get(k);
  }

  void setCoeff(int k, RationalFunction f) {
    this.coeffs.set(k, f);
  }

  LinCombOverParamField copy() {
    List<RationalFunction> c = new Vector<>(this.numVars);
    for (RationalFunction f : this.coeffs) {
      c.add(f.copy());
    }
    return new LinCombOverParamField(c);
  }

  @Override
  public String toString() {
    String s = "";
    for (RationalFunction f : this.coeffs) {
      s += f.toString() + " ";
    }
    return s;
  }

}
