// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * {@link ShareableBooleanFormula} allows sharing a {@link BooleanFormula} between code relying on
 * separate {@link Solver} instances.
 *
 * <p>{@link Solver} provides no thread-safety. As such, concurrent operations must rely on separate
 * {@link Solver} instances. Operations on {@link BooleanFormula}s are only supported among
 * instances which belong to the same {@link Solver}. As such, {@link BooleanFormula}s created under
 * the supervision of different {@link Solver}s are not compatible at first.
 *
 * <p>{@link ShareableBooleanFormula} encapsulates a {@link BooleanFormula} together with the {@link
 * FormulaManagerView} to which it belongs. The method {@link
 * ShareableBooleanFormula#getFor(FormulaManagerView)} provides the user with the ability to obtain
 * an equivalent {@link BooleanFormula} usable in the context of an arbitrary {@link
 * FormulaManagerView}. As such, it offers an opportunity to easily exchange a {@link
 * BooleanFormula} across threads whose operations rely on different underlying instances of {@link
 * Solver}.
 */
public class ShareableBooleanFormula {
  private final FormulaManagerView formulaManager;

  private final BooleanFormula formula;

  public ShareableBooleanFormula(FormulaManagerView pFormulaManager, BooleanFormula pFormula) {
    checkNotNull(pFormulaManager);
    checkNotNull(pFormula);

    formulaManager = pFormulaManager;
    formula = pFormula;
  }

  public BooleanFormula getFor(FormulaManagerView targetManager) {
    if (formulaManager == targetManager) {
      return formula;
    }

    return targetManager.translateFrom(formula, formulaManager);
  }
}
