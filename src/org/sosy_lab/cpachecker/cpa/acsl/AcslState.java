// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

@SuppressWarnings("unused")
public class AcslState implements AcslReportingState {

  private final LogManager logger;
  private final ImmutableSetMultimap<CFANode, AcslInvariantProvider> invariants;
  private final Solver solver;
  private final CToFormulaConverterWithPointerAliasing converter;

  public AcslState(
      LogManager pLogger,
      Solver pSolver,
      CToFormulaConverterWithPointerAliasing pConverter,
      ImmutableSetMultimap<CFANode, AcslInvariantProvider> pInvariants) {
    this.logger = pLogger;
    this.solver = pSolver;
    this.invariants = pInvariants;
    this.converter = pConverter;
  }

  protected ImmutableSetMultimap<CFANode, AcslInvariantProvider> getInvariants() {
    return invariants;
  }

  protected <T extends AcslInvariantProvider> ImmutableSetMultimap<CFANode, T> getInvariantsOfType(
      Class<T> type) {
    ImmutableSetMultimap.Builder<CFANode, T> result = ImmutableSetMultimap.builder();

    invariants.forEach(
        (node, invariant) -> {
          if (type.isInstance(invariant)) {
            result.put(node, type.cast(invariant));
          }
        });

    return result.build();
  }

  protected ImmutableSet<AcslInvariantProvider> getInvariantsForNode(CFANode node) {
    return invariants.get(node);
  }

  protected <T extends AcslInvariantProvider> ImmutableSet<T> getInvariantsForNode(
      CFANode node, Class<T> type) {

    return invariants.get(node).stream()
        .filter(type::isInstance)
        .map(type::cast)
        .collect(ImmutableSet.toImmutableSet());
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
    return pO instanceof AcslState that && this.invariants.equals(that.invariants);
  }

  @Override
  public String toString() {
    return "AcslState " + invariants;
  }

  @Override
  public AcslPredicate getAcslPredicate() {
    // TODO: combine the predicates in acslInvariants? Also this interface is probably not what I
    // will need, change it
    return null;
  }

  public AbstractState withAddedInvariant(CFANode pNode, AcslInvariantProvider pInit) {
    ImmutableSetMultimap<CFANode, AcslInvariantProvider> newInvariants =
        ImmutableSetMultimap.<CFANode, AcslInvariantProvider>builder()
            .putAll(invariants)
            .put(pNode, pInit)
            .build();

    return new AcslState(logger, solver, converter, newInvariants);
  }
}
