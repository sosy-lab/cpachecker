// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public final class PathFormula implements Serializable {

  private static final long serialVersionUID = -7716850731790578620L;
  private final BooleanFormula formula;
  private final SSAMap ssa;
  private final int length;
  private final PointerTargetSet pts;
  private final BooleanFormulaManagerView bfmgr;
  private final Set<BooleanFormula> booleanFormulas = new HashSet<>();

  public PathFormula(BooleanFormula pf, SSAMap ssa, PointerTargetSet pts,
      int pLength) {
    this(pf, ssa, pts, pLength, null);
  }

  public PathFormula(BooleanFormula pf, SSAMap ssa, PointerTargetSet pts,
      int pLength, BooleanFormulaManagerView bfmgr) {
    this.formula = checkNotNull(pf);
    this.ssa = checkNotNull(ssa);
    this.pts = checkNotNull(pts);
    this.length = pLength;
    this.bfmgr = bfmgr;
    this.booleanFormulas.addAll(splitBooleanFormula(pf));
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

  private Set<BooleanFormula> splitBooleanFormula(BooleanFormula pf) {
    Set<BooleanFormula> conjunctionSet = bfmgr.toConjunctionArgs(pf, true);
    HashSet<BooleanFormula> returnSet = new HashSet<>();
    for (BooleanFormula subFormula : conjunctionSet) {
      Set<BooleanFormula> disjunctionSet = bfmgr.toDisjunctionArgs(subFormula, true);
      if (disjunctionSet.size() == 1) {
        returnSet.addAll(disjunctionSet);
        continue;
      }
      for (BooleanFormula subFormulaDis : disjunctionSet) {
        returnSet.addAll(splitBooleanFormula(subFormulaDis));
      }
    }
    return returnSet;
  }

  /**
   * Change the constraint associated with the path formula, but keep everything
   * else as is.
   */
  public PathFormula updateFormula(BooleanFormula newConstraint) {
    return new PathFormula(newConstraint, ssa, pts, length, bfmgr);
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
    final boolean equalBooleanFormulas =
        booleanFormulas.containsAll(other.booleanFormulas) || other.booleanFormulas.containsAll(
            booleanFormulas);

    return (length == other.length)
        && equalBooleanFormulas
        && ssa.equals(other.ssa)
        && pts.equals(other.pts);
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
   *
   * @param in the input stream
   */
  @SuppressWarnings("UnusedVariable") // parameter is required by API
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
      return new PathFormula(formula, ssa, pts, length, null);
    }
  }
}
