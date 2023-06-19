// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class ACSLLoopAnnotation implements ACSLAnnotation {

  private final ACSLLoopInvariant lInvariant;
  private final ImmutableMap<ImmutableList<Behavior>, ACSLLoopInvariant> additionalInvariants;

  ACSLLoopAnnotation(ACSLLoopInvariant invariant) {
    this(invariant, ImmutableMap.of());
  }

  ACSLLoopAnnotation(Map<ImmutableList<Behavior>, ACSLLoopInvariant> pAdditionalInvariants) {
    this(new ACSLLoopInvariant(ACSLPredicate.getTrue()), pAdditionalInvariants);
  }

  ACSLLoopAnnotation(
      ACSLLoopInvariant invariant,
      Map<ImmutableList<Behavior>, ACSLLoopInvariant> pAdditionalInvariants) {
    lInvariant = new ACSLLoopInvariant(invariant.getPredicate().simplify());
    additionalInvariants =
        ImmutableMap.copyOf(
            Maps.transformValues(
                pAdditionalInvariants,
                loopInvariant -> new ACSLLoopInvariant(loopInvariant.getPredicate().simplify())));
  }

  @Override
  public List<Behavior> getDeclaredBehaviors() {
    return ImmutableList.of();
  }

  @Override
  public List<Behavior> getReferencedBehaviors() {
    return FluentIterable.concat(additionalInvariants.keySet()).toList();
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
      for (Entry<ImmutableList<Behavior>, ACSLLoopInvariant> entry :
          additionalInvariants.entrySet()) {
        builder.append('\n').append("for ");
        Joiner.on(", ").appendTo(builder, entry.getKey().stream().map(x -> x.getName()).iterator());
        builder.append(':');
        builder.append(entry.getValue().toString());
      }
    }
    return builder.toString();
  }
}
