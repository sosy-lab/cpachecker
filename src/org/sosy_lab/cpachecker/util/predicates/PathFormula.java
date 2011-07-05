/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates;

import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;

public class PathFormula {

  private final Formula formula;
  private final SSAMap ssa;
  private final int length;

  protected PathFormula(Formula pf, SSAMap ssa, int pLength) {
    this.formula = pf;
    this.ssa = ssa;
    this.length = pLength;
  }

  public Formula getFormula() {
    return formula;
  }

  public SSAMap getSsa() {
    return ssa;
  }

  public int getLength() {
    return length;
  }

  @Override
  public String toString(){
    return getFormula().toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PathFormula)) {
      return false;
    }

    PathFormula other = (PathFormula)obj;
    return formula.equals(other.formula)
        && ssa.equals(other.ssa);
  }

  @Override
  public int hashCode() {
    return formula.hashCode() * 17 + ssa.hashCode();
  }
}