// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.util;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * {@link ShareableBooleanFormula} allows sharing a {@link PathFormula} between code relying on
 * separate {@link Solver} instances.
 *
 * <p>{@link Solver} provides no thread-safety. As such, concurrent operations must rely on separate
 * {@link Solver} instances. Operations on {@link PathFormula}s are only supported among
 * instances which belong to the same {@link Solver}. As such, {@link PathFormula}s created under
 * the supervision of different {@link Solver}s are not compatible at first.
 *
 * <p>{@link ShareableBooleanFormula} encapsulates a {@link PathFormula} together with the {@link
 * FormulaManagerView} to which it belongs. The method {@link
 * ShareableBooleanFormula#getFor(FormulaManagerView, PathFormulaManager)} provides the user with
 * the ability to obtain an equivalent {@link PathFormula} usable in the context of an arbitrary
 * {@link PathFormulaManager}. As such, it offers an opportunity to easily exchange a {@link
 * PathFormula} across threads whose operations rely on different underlying instances of {@link
 * Solver}.
 *
 * <p style="margin-top: 10pt;">Todo</em>: Investigate whether {@link
 * FormulaManagerView#translateFrom(BooleanFormula, FormulaManagerView)} is thread-safe in its
 * access of the old {@link FormulaManagerView} ... If not, storing as {@link String} and fresh
 * parsing with the target {@link FormulaManagerView} might be the only option?
 */
public class ShareableBooleanFormula {
  private final FormulaManagerView fMgr;

  private final PathFormula formula;

  public ShareableBooleanFormula(FormulaManagerView pFMgr, PathFormula pFormula) {
    checkNotNull(pFMgr);
    checkNotNull(pFormula);

    fMgr = pFMgr;
    formula = pFormula;
  }

  public PathFormula getFor(FormulaManagerView pTargetFMgr, PathFormulaManager pTargetPfMgr) {
    checkNotNull(pTargetFMgr);
    checkNotNull(pTargetPfMgr);

    if (fMgr == pTargetFMgr) {
      return formula;
    }

    BooleanFormula sourceRaw = formula.getFormula();
    BooleanFormula targetRaw = pTargetFMgr.translateFrom(sourceRaw, fMgr);

    SSAMap ssa = formula.getSsa();
    PointerTargetSet pts = formula.getPointerTargetSet();
    PathFormula targetFormula = pTargetPfMgr.makeEmptyPathFormulaWithContext(ssa, pts);

    return targetFormula.withFormula(targetRaw);
  }
}
