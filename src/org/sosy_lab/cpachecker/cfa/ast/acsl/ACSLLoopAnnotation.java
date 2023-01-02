// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ACSLLoopAnnotation implements ACSLAnnotation {

  private final ACSLLoopInvariant lInvariant;
  private final ImmutableMap<List<Behavior>, ACSLLoopInvariant> additionalInvariants;

  ACSLLoopAnnotation(ACSLLoopInvariant invariant) {
    this(invariant, ImmutableMap.of());
  }

  ACSLLoopAnnotation(Map<List<Behavior>, ACSLLoopInvariant> pAdditionalInvariants) {
    this(new ACSLLoopInvariant(ACSLPredicate.getTrue()), pAdditionalInvariants);
  }

  ACSLLoopAnnotation(
      ACSLLoopInvariant invariant, Map<List<Behavior>, ACSLLoopInvariant> pAdditionalInvariants) {
    lInvariant = new ACSLLoopInvariant(invariant.getPredicate().simplify());
    ImmutableMap.Builder<List<Behavior>, ACSLLoopInvariant> builder =
        ImmutableMap.builderWithExpectedSize(pAdditionalInvariants.size());
    for (Entry<List<Behavior>, ACSLLoopInvariant> entry : pAdditionalInvariants.entrySet()) {
      builder.put(
          entry.getKey(), new ACSLLoopInvariant(entry.getValue().getPredicate().simplify()));
    }
    additionalInvariants = builder.buildOrThrow();
  }

  @Override
  public List<Behavior> getDeclaredBehaviors() {
    return ImmutableList.of();
  }

  @Override
  public List<Behavior> getReferencedBehaviors() {
    ImmutableList.Builder<Behavior> referencedBehaviors = ImmutableList.builder();
    for (List<Behavior> behaviors : additionalInvariants.keySet()) {
      referencedBehaviors.addAll(behaviors);
    }
    return referencedBehaviors.build();
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
      for (Entry<List<Behavior>, ACSLLoopInvariant> entry : additionalInvariants.entrySet()) {
        builder.append('\n').append("for ");
        Joiner.on(", ").appendTo(builder, entry.getKey().stream().map(x -> x.getName()).iterator());
        builder.append(':');
        builder.append(entry.getValue().toString());
      }
    }
    return builder.toString();
  }
}
