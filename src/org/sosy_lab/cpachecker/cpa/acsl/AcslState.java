// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

@SuppressWarnings("unused")
public class AcslState implements AcslReportingState {

  private final LogManager logger;
  private final ImmutableSet<AcslPredicate>
      acslInvariants; // TODO maybe remove later in favor or some map like the one below
  private final ImmutableMap<CFANode, ArrayInitialization> arrayInitializations;
  private final Solver solver;
  private final CToFormulaConverterWithPointerAliasing converter;

  public AcslState(
      LogManager pLogger,
      Solver pSolver,
      CToFormulaConverterWithPointerAliasing pConverter,
      ImmutableSet<AcslPredicate> pAcslInvariants,
      ImmutableMap<CFANode, ArrayInitialization> pArrayInitializations) {
    this.logger = pLogger;
    this.solver = pSolver;
    this.acslInvariants = pAcslInvariants;
    this.arrayInitializations = pArrayInitializations;
    this.converter = pConverter;
  }

  protected ImmutableMap<CFANode, ArrayInitialization> getArrayInitializations() {
    return arrayInitializations;
  }

  protected ArrayInitialization getArrayInitializationForNode(CFANode node) {
    return arrayInitializations.get(node);
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
    return pO instanceof AcslState that && this.acslInvariants.equals(that.acslInvariants);
  }

  @Override
  public String toString() {
    return "AcslState " + acslInvariants;
  }

  @Override
  public AcslPredicate getAcslPredicate() {
    // TODO: combine the predicates in acslInvariants?
    return null;
  }

  public AbstractState withAddedArrayInitialization(CFANode pNode, ArrayInitialization pInit) {
    ImmutableMap<CFANode, ArrayInitialization> newArrayInitializations =
        ImmutableMap.<CFANode, ArrayInitialization>builder()
            .putAll(arrayInitializations)
            .put(pNode, pInit)
            .build();

    return new AcslState(logger, solver, converter, acslInvariants, newArrayInitializations);
  }
}
