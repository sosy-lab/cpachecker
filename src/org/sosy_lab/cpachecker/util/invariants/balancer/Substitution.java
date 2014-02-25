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


public class Substitution implements Comparable<Substitution> {

  private final Variable var;
  private final Polynomial rhs;
  private final int degree;

  public Substitution(Variable v, Polynomial r) {
    var = v;
    rhs = r;
    degree = r.getDegree();
  }

  public Substitution copy() {
    Variable u = var.copy();
    Polynomial q = rhs.copy();
    return new Substitution(u, q);
  }

  @Override
  public String toString() {
    String s = "";
    s += var.toString()+" <-- "+rhs.toString();
    return s;
  }

  @Override
  public int compareTo(Substitution other) {
    return this.degree - other.degree;
  }

  public Variable getVar() {
    return var;
  }

  public Polynomial getRHS() {
    return rhs;
  }

  public int getDegree() {
    return degree;
  }

  /*
   * Applying subs to this Substitution means the following steps:
   * (1) Apply subs to rhs.
   * (2) Apply subs to var (meaning replace var or don't).
   * (3) Take the resulting polys on the lhs and rhs of this substitution,
   *     subtract one from the other, call its
   *     linIsolate method, and return the result (which may be null).
   */
  public Substitution applySubstitution(Substitution subs) {
    // (1) Apply subs to rhs.
    Polynomial newrhs = Polynomial.applySubstitution(subs, rhs);
    // (2) Apply subs to var (meaning replace var or don't).
    Polynomial newlhs;
    if (var == subs.getVar()) {
      newlhs = subs.getRHS();
    } else {
      newlhs = new Polynomial(var);
    }
    // (3)
    Polynomial f = Polynomial.subtract(newlhs, newrhs);
    Substitution s = f.linearIsolateFirst();
    return s;
  }

}
