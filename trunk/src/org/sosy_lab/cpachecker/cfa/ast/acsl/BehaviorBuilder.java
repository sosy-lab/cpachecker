// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Collection;

public class BehaviorBuilder {
  private String behaviorName;
  private EnsuresClause ensuresClause;
  private RequiresClause requiresClause;
  private AssumesClause assumesClause;

  public BehaviorBuilder() {
    ensuresClause = new EnsuresClause(ACSLPredicate.getTrue());
    requiresClause = new RequiresClause(ACSLPredicate.getTrue());
    assumesClause = new AssumesClause(ACSLPredicate.getTrue());
  }

  @CanIgnoreReturnValue
  public BehaviorBuilder setBehaviorName(String name) {
    behaviorName = name;
    return this;
  }

  @CanIgnoreReturnValue
  public BehaviorBuilder add(Object o) {
    if (o instanceof RequiresClause) {
      return add((RequiresClause) o);
    } else if (o instanceof EnsuresClause) {
      return add((EnsuresClause) o);
    } else if (o instanceof AssumesClause) {
      return add((AssumesClause) o);
    } else {
      throw new IllegalArgumentException();
    }
  }

  @CanIgnoreReturnValue
  public BehaviorBuilder add(EnsuresClause ens) {
    ensuresClause = ensuresClause.and(ens);
    return this;
  }

  @CanIgnoreReturnValue
  public BehaviorBuilder add(RequiresClause req) {
    requiresClause = requiresClause.and(req);
    return this;
  }

  @CanIgnoreReturnValue
  public BehaviorBuilder add(AssumesClause ass) {
    assumesClause = assumesClause.and(ass);
    return this;
  }

  @CanIgnoreReturnValue
  public BehaviorBuilder addAll(Collection<?> clauses) {
    for (Object clause : clauses) {
      add(clause);
    }
    return this;
  }

  public Behavior build() {
    assert behaviorName != null : "Behavior needs a name";
    return new Behavior(behaviorName, ensuresClause, requiresClause, assumesClause);
  }
}
