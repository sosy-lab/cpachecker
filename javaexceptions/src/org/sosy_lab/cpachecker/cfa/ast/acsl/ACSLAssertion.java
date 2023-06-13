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
import java.util.List;

public final class ACSLAssertion implements ACSLAnnotation {

  private final AssertionKind kind;
  private final ImmutableList<Behavior> enclosingBehaviors;
  private final ACSLPredicate predicate;

  ACSLAssertion(AssertionKind pKind, ACSLPredicate p) {
    this(pKind, ImmutableList.of(), p);
  }

  ACSLAssertion(AssertionKind pKind, List<Behavior> enclosing, ACSLPredicate p) {
    kind = pKind;
    enclosingBehaviors = ImmutableList.copyOf(enclosing);
    predicate = p.simplify();
  }

  @Override
  public List<Behavior> getDeclaredBehaviors() {
    return ImmutableList.of();
  }

  @Override
  public List<Behavior> getReferencedBehaviors() {
    return enclosingBehaviors;
  }

  @Override
  public ACSLPredicate getPredicateRepresentation() {
    if (!enclosingBehaviors.isEmpty()) {
      // TODO: can currently not be expressed correctly
      return ACSLPredicate.getTrue();
    }
    return predicate;
  }

  @Override
  public ACSLPredicate getCompletenessPredicate() {
    return ACSLPredicate.getTrue();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (!enclosingBehaviors.isEmpty()) {
      builder.append("for ");
      Joiner.on(", ")
          .appendTo(builder, enclosingBehaviors.stream().map(x -> x.getName()).iterator());
      builder.append(": ");
    }
    return builder.toString() + kind + ' ' + predicate + ';';
  }

  public enum AssertionKind {
    ASSERT("assert"),
    CHECK("check");

    private final String name;

    AssertionKind(String pName) {
      name = pName;
    }

    @Override
    public String toString() {
      return name;
    }
  }
}
