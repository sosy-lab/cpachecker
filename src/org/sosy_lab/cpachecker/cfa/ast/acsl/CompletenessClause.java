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
                BinaryOperator.OR);
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
                      BinaryOperator.AND)
                  .negate();
          predicateRepresentation =
              new ACSLLogicalPredicate(predicateRepresentation, notBoth, BinaryOperator.AND);
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
    return kind.toString() + builder.toString();
  }

  public enum RelationKind {
    COMPLETE ("complete"),
    DISJOINT ("disjoint");

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
