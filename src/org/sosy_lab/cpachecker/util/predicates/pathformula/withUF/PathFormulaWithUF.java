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
package org.sosy_lab.cpachecker.util.predicates.pathformula.withUF;

import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;


public class PathFormulaWithUF extends PathFormula {

  public PathFormulaWithUF(final BooleanFormula formula,
                           final SSAMap ssa,
                           final PointerTargetSet pointerTargetSet,
                           final int length) {
    super(formula, ssa, length);
    this.pointerTargetSet = pointerTargetSet;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PathFormulaWithUF)) {
      return false;
    }

    final PathFormulaWithUF other = (PathFormulaWithUF) o;
    return getFormula().equals(other.getFormula())
        && getSsa().equals(other.getSsa())
        && pointerTargetSet.equals(other.pointerTargetSet)
        && (getLength() == other.getLength());
  }

  public PointerTargetSet getPointerTargetSet() {
    return pointerTargetSet;
  }

  @Override
  public int hashCode() {
    return (getFormula().hashCode() * 31 + getSsa().hashCode() * 17 + pointerTargetSet.hashCode()) * 53 + getLength();
  }

  private final PointerTargetSet pointerTargetSet;

  private static final long serialVersionUID = 3298647820263644655L;
}
