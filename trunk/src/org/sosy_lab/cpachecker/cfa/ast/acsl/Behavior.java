// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.util.Objects;

public class Behavior {

  private final String name;
  private final EnsuresClause ensuresClause;
  private final RequiresClause requiresClause;
  private final AssumesClause assumesClause;

  public Behavior(String pName) {
    this(
        pName,
        new EnsuresClause(ACSLPredicate.getTrue()),
        new RequiresClause(ACSLPredicate.getTrue()),
        new AssumesClause(ACSLPredicate.getTrue()));
  }

  public Behavior(String pName, EnsuresClause ens, RequiresClause req, AssumesClause ass) {
    name = pName;
    ensuresClause = new EnsuresClause(ens.getPredicate().simplify());
    requiresClause = new RequiresClause(req.getPredicate().simplify());
    assumesClause = new AssumesClause(ass.getPredicate().simplify());
  }

  public String getName() {
    return name;
  }

  public AssumesClause getAssumesClause() {
    return assumesClause;
  }

  public ACSLPredicate getPreStatePredicate() {
    ACSLPredicate requiresPredicate = requiresClause.getPredicate();
    ACSLPredicate negatedAssumesPredicate = assumesClause.getPredicate().negate();
    return new ACSLLogicalPredicate(
        requiresPredicate, negatedAssumesPredicate, ACSLBinaryOperator.OR);
  }

  public ACSLPredicate getPostStatePredicate() {
    ACSLPredicate ensuresPredicate = ensuresClause.getPredicate();
    ACSLPredicate negatedAssumesPredicate =
        new PredicateAt(assumesClause.getPredicate(), ACSLDefaultLabel.OLD).negate();
    return new ACSLLogicalPredicate(
        ensuresPredicate, negatedAssumesPredicate, ACSLBinaryOperator.OR);
  }

  @Override
  public String toString() {
    return "behavior "
        + name
        + ":\n"
        + assumesClause
        + '\n'
        + requiresClause
        + '\n'
        + ensuresClause;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Behavior)) {
      return false;
    }
    Behavior behavior = (Behavior) other;
    // We require that behaviors have globally unique names, so this is enough here
    return Objects.equals(name, behavior.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }
}
