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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * A {@link BooleanFormula} that represents a path or a set of paths. Such formulas do not describe
 * program states but program operations. All variables in path formulas have SSA indices, and the
 * current mapping of variables to SSA indices is also stored as an {@link SSAMap}. Furthermore, a
 * {@link PointerTargetSet} with information about pointers and how certain pointer operations are
 * encoded in the formula is also stored.
 *
 * <p>Path formulas can be created with the methods in {@link PathFormulaManager}, which also
 * provides methods with additional operations.
 */
@javax.annotation.concurrent.Immutable // cannot prove deep immutability
public final class PathFormula implements Serializable {

  private static final long serialVersionUID = -7716850731790578620L;
  private final BooleanFormula formula;
  private final SSAMap ssa;
  private final int length;
  private final PointerTargetSet pts;

  // Do not make public, cf. createManually()
  PathFormula(BooleanFormula pf, SSAMap ssa, PointerTargetSet pts, int pLength) {
    formula = checkNotNull(pf);
    this.ssa = checkNotNull(ssa);
    this.pts = checkNotNull(pts);
    length = pLength;
  }

  /**
   * Create a new instance with full custom values.
   *
   * <p>WARNING: Most components should NOT call this method but instead one of the high-level
   * methods in {@link PathFormulaManager} or at least one of the <code>withSomething</code> methods
   * in this class. This method should only be called where strictly necessary, i.e., in low-level
   * code that is the implementation of such high-level methods. Callers inside the same package
   * should call the constructor.
   */
  @Deprecated
  public static PathFormula createManually(
      BooleanFormula pf, SSAMap ssa, PointerTargetSet pts, int pLength) {
    return new PathFormula(pf, ssa, pts, pLength);
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
   * Create a copy of this instance but with a new constraint, everything else stays as is.
   *
   * <p>WARNING: Use this method only if you are sure that the result is meaningful and correct in
   * your specific use case. Usually the more high-level methods from {@link PathFormulaManager}
   * should be used instead.
   */
  public PathFormula withFormula(BooleanFormula newConstraint) {
    return new PathFormula(newConstraint, ssa, pts, length);
  }

  /**
   * Create a copy of this instance but with a new SSAMap and PointerTargetSet.
   *
   * <p>WARNING: Use this method only if you are sure that the result is meaningful and correct in
   * your specific use case. Usually the more high-level methods from {@link PathFormulaManager}
   * should be used instead.
   *
   * <p>WARNING: When using this method to update only the SSAMap, think twice about whether the
   * PointerTargetSet also needs to be updated! Usually this is the case because the
   * PointerTargetSet also contains information that influences how variables in CFA edges are
   * represented in path formulas, just like the SSAMap does.
   */
  public PathFormula withContext(SSAMap newSsa, PointerTargetSet newPts) {
    return new PathFormula(formula, newSsa, newPts, length);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PathFormula)) {
      return false;
    }

    PathFormula other = (PathFormula) obj;
    return (length == other.length)
        && formula.equals(other.formula)
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
      return new PathFormula(formula, ssa, pts, length);
    }
  }
}
