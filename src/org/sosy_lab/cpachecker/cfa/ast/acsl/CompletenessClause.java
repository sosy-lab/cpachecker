// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import com.google.common.base.Joiner;
import java.util.List;
import java.util.Objects;

public class CompletenessClause {

  private final List<Behavior> behaviors;
  private final RelationKind kind;

  public CompletenessClause(List<Behavior> pBehaviors, RelationKind pKind) {
    behaviors = pBehaviors;
    kind = pKind;
  }

  private ACSLPredicate makePredicateRepresentation() {
    ACSLPredicate predicateRepresentation;
    if (kind.equals(RelationKind.COMPLETE)) {
      predicateRepresentation = ACSLPredicate.getFalse();
      for (Behavior behavior : behaviors) {
        predicateRepresentation =
            new ACSLLogicalPredicate(
                predicateRepresentation,
                behavior.getAssumesClause().getPredicate(),
                ACSLBinaryOperator.OR);
      }
    } else if (kind.equals(RelationKind.DISJOINT)) {
      predicateRepresentation = ACSLPredicate.getTrue();
      for (Behavior behavior1 : behaviors) {
        for (Behavior behavior2 : behaviors) {
          if (behavior1 == behavior2) {
            continue;
          }
          ACSLPredicate notBoth =
              new ACSLLogicalPredicate(
                      behavior1.getAssumesClause().getPredicate(),
                      behavior2.getAssumesClause().getPredicate(),
                      ACSLBinaryOperator.AND)
                  .negate();
          predicateRepresentation =
              new ACSLLogicalPredicate(predicateRepresentation, notBoth, ACSLBinaryOperator.AND);
        }
      }
    } else {
      throw new AssertionError("Unknown kind: " + kind);
    }
    return predicateRepresentation;
  }

  public ACSLPredicate getPredicateRepresentation() {
    return makePredicateRepresentation();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (!behaviors.isEmpty()) {
      builder.append(' ');
      Joiner.on(", ").appendTo(builder, behaviors.stream().map(x -> x.getName()).iterator());
    }
    builder.append(';');
    return kind.toString() + builder;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof CompletenessClause)) {
      return false;
    }
    CompletenessClause that = (CompletenessClause) other;
    return Objects.equals(behaviors, that.behaviors) && kind == that.kind;
  }

  @Override
  public int hashCode() {
    return Objects.hash(behaviors, kind);
  }

  public enum RelationKind {
    COMPLETE("complete"),
    DISJOINT("disjoint");

    private final String kindName;

    RelationKind(String pKindName) {
      kindName = pKindName;
    }

    @Override
    public String toString() {
      return kindName + " behaviors";
    }
  }
}
