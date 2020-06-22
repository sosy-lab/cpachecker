package org.sosy_lab.cpachecker.core.algorithm.acsl;

import java.util.Collection;

public class BehaviorBuilder {
  private String behaviorName;
  private EnsuresClause ensuresClause;
  private RequiresClause requiresClause;
  private AssumesClause assumesClause;

  public BehaviorBuilder() {
    behaviorName = "";
    ensuresClause = new EnsuresClause(ACSLPredicate.getTrue());
    requiresClause = new RequiresClause(ACSLPredicate.getTrue());
    assumesClause = new AssumesClause(ACSLPredicate.getTrue());
  }

  public BehaviorBuilder setBehaviorName(String name) {
    behaviorName = name;
    return this;
  }

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

  public BehaviorBuilder add(EnsuresClause ens) {
    ensuresClause.and(ens);
    return this;
  }

  public BehaviorBuilder add(RequiresClause req) {
    requiresClause.and(req);
    return this;
  }

  public BehaviorBuilder add(AssumesClause ass) {
    assumesClause.and(ass);
    return this;
  }

  public BehaviorBuilder addAll(Collection<? extends Object> clauses) {
    for (Object clause : clauses) {
      add(clause);
    }
    return this;
  }

  public Behavior build() {
    return new Behavior(behaviorName, ensuresClause, requiresClause, assumesClause);
  }
}
