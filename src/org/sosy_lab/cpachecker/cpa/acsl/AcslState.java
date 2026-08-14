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
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.acsltoformula.AcslPredicateToFormulaVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;

@SuppressWarnings("unused")
public class AcslState implements FormulaReportingState {

  private AcslPredicateToFormulaVisitor acslPredVisitor;
  private final LogManager logger;
  private final ImmutableSet<AcslPredicate> acslInvariants;
  private final Solver solver;
  private final CToFormulaConverterWithPointerAliasing converter;
  private final CFA cfa;

  public AcslState(
      LogManager pLogger,
      CFA pCfa,
      Solver pSolver,
      CToFormulaConverterWithPointerAliasing pConverter,
      ImmutableSet<AcslPredicate> pAcslInvariants) {
    this.logger = pLogger;
    this.cfa = pCfa;
    this.solver = pSolver;
    this.converter = pConverter;
    this.acslInvariants = pAcslInvariants;
  }

  protected void setVisitor(AcslPredicateToFormulaVisitor pVisitor) {
    // TODO this should probably work differently, but I need to understand where i get the ssa from
    this.acslPredVisitor = pVisitor;
  }

  @Override
  public BooleanFormula getFormulaApproximation(FormulaManagerView manager) {
    // TODO Important for communication with other CPAs (I think)
    return null;
  }

  @Override
  public BooleanFormula getScopedFormulaApproximation(
      FormulaManagerView manager, FunctionEntryNode functionScope) {
    // TODO
    return null;
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
    return pO instanceof AcslState that && true;
  }

  @Override
  public String toString() {
    // TODO
    return "AcslState " + "TODO";
  }
}
