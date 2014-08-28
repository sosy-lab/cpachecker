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
package org.sosy_lab.cpachecker.util.predicates.pathformula;

import java.io.Serializable;

import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;

public final class PathFormula implements Serializable {

  private static final long serialVersionUID = -7716850731790578619L;
  private final transient BooleanFormula formula;
  private final SSAMap ssa;
  private final int length;
  private final PointerTargetSet pts;

  public PathFormula(BooleanFormula pf, SSAMap ssa, PointerTargetSet pts,
      int pLength) {
    this.formula = pf;
    this.ssa = ssa;
    this.pts = pts;
    this.length = pLength;
  }

  public BooleanFormula getFormula() {
    return formula;
  }

  public SSAMap getSsa() {
    return ssa;
  }

  public PointerTargetSet getPointerTargetSet() {
    return pts;
  }

  public int getLength() {
    return length;
  }

  @Override
  public String toString() {
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
    return (length == other.length)
        && formula.equals(other.formula)
        && ssa.equals(other.ssa)
        && pts.equals(other.pts)
        ;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + formula.hashCode();
    result = prime * result + length;
    result = prime * result + pts.hashCode();
    result = prime * result + ssa.hashCode();
    return result;
  }
}