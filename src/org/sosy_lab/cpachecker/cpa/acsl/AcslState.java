// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.util.Objects;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate.AcslBinaryPredicateOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

@SuppressWarnings("unused")
public class AcslState implements AcslReportingState {

  private final LogManager logger;
  private final ImmutableSetMultimap<CFANode, AcslInvariantProvider> invariants;
  private final CProgramScope scope;

  public AcslState(
      LogManager pLogger,
      ImmutableSetMultimap<CFANode, AcslInvariantProvider> pInvariants,
      CProgramScope pScope) {
    this.logger = pLogger;
    this.invariants = pInvariants;
    this.scope = pScope;
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
    return Objects.hashCode(invariants);
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof AcslState that && this.invariants.equals(that.invariants);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("AcslState: ");
    invariants
        .asMap()
        .forEach(
            (node, nodeInvariants) -> {
              sb.append(node)
                  .append(" -> ")
                  .append(nodeInvariants.size())
                  .append(" Invariant Class(es): ");
              nodeInvariants.forEach(
                  invariant -> sb.append(invariant.getClass().getSimpleName()).append(", "));
              sb.append("; ");
            });

    return sb.toString();
  }

  @Override
  public AcslPredicate getAcslPredicate() {
    // TODO either implement or remove with all of the places it is called depending on the chosen
    // communication approach between the CPAs
    return null;
  }

  @Override
  public AcslPredicate getAcslPredicate(CFANode pNode, FileLocation pLoc) {
    return invariants.get(pNode).stream()
        .map(provider -> provider.toInvariant(scope, pLoc, logger))
        .reduce((p1, p2) -> new AcslBinaryPredicate(pLoc, p1, p2, AcslBinaryPredicateOperator.AND))
        .orElse(null);
  }

  public AbstractState withAddedInvariant(CFANode pNode, AcslInvariantProvider pInit) {
    ImmutableSetMultimap<CFANode, AcslInvariantProvider> newInvariants =
        ImmutableSetMultimap.<CFANode, AcslInvariantProvider>builder()
            .putAll(invariants)
            .put(pNode, pInit)
            .build();

    return new AcslState(logger, newInvariants, scope);
  }

  public AbstractState withAddedInvariant(
      ImmutableList<CFANode> pNodes, AcslInvariantProvider pInit) {
    ImmutableSetMultimap.Builder<CFANode, AcslInvariantProvider> builder =
        ImmutableSetMultimap.<CFANode, AcslInvariantProvider>builder().putAll(invariants);
    for (CFANode n : pNodes) {
      builder.put(n, pInit);
    }

    ImmutableSetMultimap<CFANode, AcslInvariantProvider> newInvariants = builder.build();
    return new AcslState(logger, newInvariants, scope);
  }
}
