package org.sosy_lab.cpachecker.core.algorithm.acsl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ContractBuilder {

  private EnsuresClause ensuresClause;
  private RequiresClause requiresClause;
  private List<Behavior> behaviors;
  private List<CompletenessClause> completenessClauses;

  public ContractBuilder() {
    ensuresClause = new EnsuresClause(ACSLPredicate.getTrue());
    requiresClause = new RequiresClause(ACSLPredicate.getTrue());
    behaviors = new ArrayList<>();
    completenessClauses = new ArrayList<>();
  }

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

  public ContractBuilder add(EnsuresClause ens) {
    ensuresClause.and(ens);
    return this;
  }

  public ContractBuilder add(RequiresClause req) {
    requiresClause.and(req);
    return this;
  }

  public ContractBuilder add(Behavior behavior) {
    behaviors.add(behavior);
    return this;
  }

  public ContractBuilder add(CompletenessClause completenessClause) {
    completenessClauses.add(completenessClause);
    return this;
  }

  public ContractBuilder addAll(Collection<? extends Object> clauses) {
    for (Object clause : clauses) {
      add(clause);
    }
    return this;
  }

  public FunctionContract build() {
    return new FunctionContract(requiresClause, ensuresClause, behaviors, completenessClauses);
  }
}
