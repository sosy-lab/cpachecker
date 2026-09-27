// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

@SuppressWarnings("unused")
public class AcslState implements AcslReportingState {

  private final LogManager logger;
  private final ImmutableSet<AcslPredicate> acslInvariants;
  private final Solver solver;

  public AcslState(
      LogManager pLogger,
      Solver pSolver,
      CToFormulaConverterWithPointerAliasing pConverter,
      ImmutableSet<AcslPredicate> pAcslInvariants) {
    this.logger = pLogger;
    this.solver = pSolver;
    this.acslInvariants = pAcslInvariants;
  }

  @Override
  public int hashCode() {
    // TODO
    return 42;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    // TODO replace true below with actual comparison of the relevant fields
    return pO instanceof AcslState that
        && this.acslInvariants.equals(that.acslInvariants);
  }

  @Override
  public String toString() {
    return "AcslState " + acslInvariants.toString();
  }

  @Override
  public AcslPredicate getAcslPredicate() {
    //TODO: combine the predicates in acslInvariants?
    return null;
  }
}
