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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public final class PathFormula implements Serializable {

  private static final long serialVersionUID = -7716850731790578620L;
  private final BooleanFormula formula;
  private final SSAMap ssa;
  private final int length;
  private final PointerTargetSet pts;

  public PathFormula(BooleanFormula pf, SSAMap ssa, PointerTargetSet pts,
      int pLength) {
    this.formula = checkNotNull(pf);
    this.ssa = checkNotNull(ssa);
    this.pts = checkNotNull(pts);
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

  /**
   * Change the constraint associated with the path formula, but keep everything
   * else as is.
   */
  public PathFormula updateFormula(BooleanFormula newConstraint) {
    return new PathFormula(newConstraint, ssa, pts, length);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
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

  private Object writeReplace() {
    return new SerializationProxy(this);
  }

  /**
   * javadoc to remove unused parameter warning
   * @param in the input stream
   */
  private void readObject(ObjectInputStream in) throws IOException {
    throw new InvalidObjectException("Proxy required");
  }

  private static class SerializationProxy implements Serializable {
    // (de)serialization only works properly for formulae which were built with the same
    // formula manager as used by PredicateCPA
    private static final long serialVersionUID = 309890892L;

    private final String formulaDump;
    private final SSAMap ssa;
    private final int length;
    private final PointerTargetSet pts;

    public SerializationProxy(PathFormula pPathFormula) {
      FormulaManagerView mgr = GlobalInfo.getInstance().getPredicateFormulaManagerView();
      formulaDump = mgr.dumpFormula(pPathFormula.formula).toString();
      ssa = pPathFormula.ssa;
      length = pPathFormula.length;
      pts = pPathFormula.pts;
    }

    private Object readResolve() {
      FormulaManagerView mgr = GlobalInfo.getInstance().getPredicateFormulaManagerView();
      BooleanFormula formula = mgr.parse(formulaDump);
      return new PathFormula(formula, ssa, pts, length);
    }
  }
}