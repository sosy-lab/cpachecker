// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ContractBuilder {

  private EnsuresClause ensuresClause;
  private RequiresClause requiresClause;
  private final List<Behavior> behaviors;
  private final List<CompletenessClause> completenessClauses;

  public ContractBuilder() {
    ensuresClause = new EnsuresClause(ACSLPredicate.getTrue());
    requiresClause = new RequiresClause(ACSLPredicate.getTrue());
    behaviors = new ArrayList<>();
    completenessClauses = new ArrayList<>();
  }

  @CanIgnoreReturnValue
  public ContractBuilder add(Object o) {
    if (o instanceof RequiresClause) {
      return add((RequiresClause) o);
    } else if (o instanceof EnsuresClause) {
      return add((EnsuresClause) o);
    } else if (o instanceof Behavior) {
      return add((Behavior) o);
    } else if (o instanceof CompletenessClause) {
      return add((CompletenessClause) o);
    } else {
      throw new IllegalArgumentException();
    }
  }

  @CanIgnoreReturnValue
  public ContractBuilder add(EnsuresClause ens) {
    ensuresClause = ensuresClause.and(ens);
    return this;
  }

  @CanIgnoreReturnValue
  public ContractBuilder add(RequiresClause req) {
    requiresClause = requiresClause.and(req);
    return this;
  }

  @CanIgnoreReturnValue
  public ContractBuilder add(Behavior behavior) {
    behaviors.add(behavior);
    return this;
  }

  @CanIgnoreReturnValue
  public ContractBuilder add(CompletenessClause completenessClause) {
    completenessClauses.add(completenessClause);
    return this;
  }

  @CanIgnoreReturnValue
  public ContractBuilder addAll(Collection<?> clauses) {
    for (Object clause : clauses) {
      add(clause);
    }
    return this;
  }

  public FunctionContract build() {
    return new FunctionContract(requiresClause, ensuresClause, behaviors, completenessClauses);
  }
}
