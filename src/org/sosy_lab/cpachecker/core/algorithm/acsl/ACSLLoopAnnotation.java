// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.acsl;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ACSLLoopAnnotation implements ACSLAnnotation {

  private final LoopInvariant lInvariant;
  private final ImmutableMap<List<Behavior>, LoopInvariant> additionalInvariants;

  public ACSLLoopAnnotation(LoopInvariant invariant) {
    this(invariant, ImmutableMap.of());
  }

  public ACSLLoopAnnotation(Map<List<Behavior>, LoopInvariant> pAdditionalInvariants) {
    this(new LoopInvariant(ACSLPredicate.getTrue()), pAdditionalInvariants);
  }

  public ACSLLoopAnnotation(
      LoopInvariant invariant, Map<List<Behavior>, LoopInvariant> pAdditionalInvariants) {
    lInvariant = invariant;
    additionalInvariants = ImmutableMap.copyOf(pAdditionalInvariants);
  }

  @Override
  public ACSLPredicate getPredicateRepresentation() {
    // TODO: Invariants specific to certain enclosing behaviors can currently not be expressed
    //  correctly, so we ignore them
    return lInvariant.getPredicate();
  }

  @Override
  public ACSLPredicate getCompletenessPredicate() {
    return ACSLPredicate.getTrue();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(lInvariant.toString());
    if (!additionalInvariants.isEmpty()) {
      for (Entry<List<Behavior>, LoopInvariant> entry : additionalInvariants.entrySet()) {
        builder.append('\n').append("for ");
        Joiner.on(", ").appendTo(builder, entry.getKey().stream().map(x -> x.getName()).iterator());
        builder.append(':');
        builder.append(entry.getValue().toString());
      }
    }
    return builder.toString();
  }
}
